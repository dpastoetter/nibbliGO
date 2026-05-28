package com.nibbli.nibbligo.feature.models.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.model.Modality
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.storage.work.ModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelItemUi(
    val info: ModelInfo,
    val isInstalled: Boolean,
    val isDownloading: Boolean = false,
)

data class ModelsUiState(
    val models: List<ModelItemUi> = emptyList(),
    val installed: List<InstalledModel> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository,
    private val petEventBus: PetEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        viewModelScope.launch {
            combine(
                modelRepository.observeCatalog(),
                modelRepository.observeInstalled(),
                workManager.getWorkInfosByTagFlow(ModelDownloadWorker.WORK_TAG),
            ) { catalog, installed, workInfos ->
                val installedIds = installed.map { it.modelId }.toSet()
                val downloadingIds = workInfos
                    .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .mapNotNull { work ->
                        work.tags.find { it.startsWith("hf_download_") }
                            ?.removePrefix("hf_download_")
                    }
                    .toSet()
                val failed = workInfos
                    .filter { it.state == WorkInfo.State.FAILED }
                    .maxByOrNull { it.id }
                val failMsg = failed?.outputData?.getString(ModelDownloadWorker.KEY_ERROR)
                ModelsUiState(
                    models = catalog.map { info ->
                        ModelItemUi(
                            info = info,
                            isInstalled = info.id in installedIds,
                            isDownloading = info.id in downloadingIds,
                        )
                    },
                    installed = installed,
                    isLoading = false,
                    message = failMsg?.let { "Download failed: $it" } ?: _uiState.value.message,
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun install(modelId: String) {
        viewModelScope.launch {
            modelRepository.install(modelId)
                .onSuccess {
                    petEventBus.emit(PetEvent.NewModelTried(modelId))
                    val info = modelRepository.getCatalog().find { it.id == modelId }
                    val msg = if (info?.requiresLiteRt == true) {
                        "Downloading $modelId… watch the notification; use Wi‑Fi for large files."
                    } else {
                        "Installed $modelId"
                    }
                    _uiState.update { it.copy(message = msg) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message) }
                }
        }
    }

    fun uninstall(modelId: String) {
        viewModelScope.launch {
            modelRepository.uninstall(modelId)
            _uiState.update { it.copy(message = "Removed $modelId") }
        }
    }

    fun modalityLabel(modality: Modality): String = when (modality) {
        Modality.TEXT -> "Text"
        Modality.VISION -> "Vision"
        Modality.AUDIO -> "Audio"
    }
}

package com.nibbli.nibbligo.feature.models.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthRepository
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
    val isWaitingForNetwork: Boolean = false,
    val downloadProgress: Int = 0,
)

data class ModelsUiState(
    val models: List<ModelItemUi> = emptyList(),
    val installed: List<InstalledModel> = emptyList(),
    val isLoading: Boolean = true,
    val hfSignedIn: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository,
    private val petEventBus: PetEventBus,
    private val huggingFaceAuthRepository: HuggingFaceAuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        viewModelScope.launch {
            combine(
                modelRepository.observeCatalog(),
                modelRepository.observeInstalled(),
                huggingFaceAuthRepository.accessToken,
                workManager.getWorkInfosByTagFlow(ModelDownloadWorker.WORK_TAG),
            ) { catalog, installed, hfToken, workInfos ->
                val installedIds = installed.map { it.modelId }.toSet()
                val activeWorks = workInfos.filter {
                    it.state in ACTIVE_DOWNLOAD_STATES
                }
                val downloadingIds = activeWorks.mapNotNull { work ->
                    work.tags.find { it.startsWith("hf_download_") }
                        ?.removePrefix("hf_download_")
                }.toSet()
                val blockedIds = workInfos
                    .filter { it.state == WorkInfo.State.BLOCKED }
                    .mapNotNull { work ->
                        work.tags.find { it.startsWith("hf_download_") }
                            ?.removePrefix("hf_download_")
                    }
                    .toSet()
                val progressByModel = activeWorks.associate { work ->
                    val modelId = work.tags.find { it.startsWith("hf_download_") }
                        ?.removePrefix("hf_download_") ?: ""
                    modelId to work.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                }
                val failed = workInfos
                    .filter { it.state == WorkInfo.State.FAILED }
                    .maxByOrNull { it.id }
                val cancelled = workInfos.any { it.state == WorkInfo.State.CANCELLED }
                val failMsg = failed?.outputData?.getString(ModelDownloadWorker.KEY_ERROR)?.let {
                    "Download failed: $it"
                }
                ModelsUiState(
                    models = catalog
                        .sortedWith(
                            compareByDescending<ModelInfo> { it.recommendedForNibbliGo }
                                .thenBy { it.displayName },
                        )
                        .map { info ->
                        ModelItemUi(
                            info = info,
                            isInstalled = info.id in installedIds,
                            isDownloading = info.id in downloadingIds,
                            isWaitingForNetwork = info.id in blockedIds,
                            downloadProgress = progressByModel[info.id] ?: 0,
                        )
                    },
                    installed = installed,
                    isLoading = false,
                    hfSignedIn = !hfToken.isNullOrBlank(),
                    message = when {
                        failMsg != null -> failMsg
                        cancelled -> "Download cancelled"
                        activeWorks.isNotEmpty() -> _uiState.value.message
                        else -> null
                    },
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun install(modelId: String) {
        viewModelScope.launch {
            modelRepository.install(modelId)
                .onSuccess {
                    petEventBus.emit(PetEvent.NewModelTried(modelId))
                    _uiState.update {
                        it.copy(message = "Downloading $modelId… check the notification for progress.")
                    }
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun modalityLabel(modality: Modality): String = when (modality) {
        Modality.TEXT -> "Text"
        Modality.VISION -> "Vision"
        Modality.AUDIO -> "Audio"
    }

    companion object {
        private val ACTIVE_DOWNLOAD_STATES = setOf(
            WorkInfo.State.RUNNING,
            WorkInfo.State.ENQUEUED,
        )
    }
}

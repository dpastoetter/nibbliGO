package com.nibbli.nibbligo.feature.models.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.model.Modality
import com.nibbli.nibbligo.core.model.PetEvent
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

data class ModelsUiState(
    val models: List<ModelItemUi> = emptyList(),
    val installed: List<InstalledModel> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val petEventBus: PetEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modelRepository.observeCatalog(),
                modelRepository.observeInstalled(),
            ) { catalog, installed ->
                val installedIds = installed.map { it.modelId }.toSet()
                ModelsUiState(
                    models = catalog.map { ModelItemUi(it, it.id in installedIds) },
                    installed = installed,
                    isLoading = false,
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun install(modelId: String) {
        viewModelScope.launch {
            modelRepository.install(modelId)
                .onSuccess {
                    petEventBus.emit(PetEvent.NewModelTried(modelId))
                    _uiState.update { it.copy(message = "Installed $modelId") }
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

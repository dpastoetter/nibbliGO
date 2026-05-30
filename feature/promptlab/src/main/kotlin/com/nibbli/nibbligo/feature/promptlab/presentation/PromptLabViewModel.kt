package com.nibbli.nibbligo.feature.promptlab.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.PromptRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.PromptPreset
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.SavedPrompt
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareResult(val modelId: String, val output: String)

data class PromptLabUiState(
    val prompt: String = "",
    val preset: PromptPreset? = null,
    val output: String = "",
    val compareResults: List<CompareResult> = emptyList(),
    val installedModelIds: List<String> = emptyList(),
    val selectedModelId: String? = null,
    val isRunning: Boolean = false,
    val favorites: List<SavedPrompt> = emptyList(),
)

@HiltViewModel
class PromptLabViewModel @Inject constructor(
    private val inferenceRuntime: InferenceRuntime,
    private val modelRepository: ModelRepository,
    private val promptRepository: PromptRepository,
    private val petEventBus: PetEventBus,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromptLabUiState())
    val uiState: StateFlow<PromptLabUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val installed = modelRepository.getInstalledModelIds()
            val defaultModel = userPreferencesRepository.defaultModelId.first()
            val selected = defaultModel?.takeIf { it in installed } ?: installed.firstOrNull()
            _uiState.update { it.copy(installedModelIds = installed, selectedModelId = selected) }
            promptRepository.observeSavedPrompts().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
    }

    fun updatePrompt(value: String) = _uiState.update { it.copy(prompt = value) }
    fun selectPreset(preset: PromptPreset) = _uiState.update { it.copy(preset = preset) }
    fun selectModel(modelId: String) = _uiState.update { it.copy(selectedModelId = modelId) }

    fun run() {
        val state = _uiState.value
        val modelId = state.selectedModelId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true) }
            inferenceRuntime.ensureModelLoaded(modelId)
            when (val result = inferenceRuntime.complete(
                CompletionRequest(modelId, state.prompt, state.preset),
            )) {
                is RuntimeResult.Success -> {
                    petEventBus.emit(PetEvent.PromptLabRun)
                    _uiState.update { it.copy(output = result.data, isRunning = false) }
                }
                is RuntimeResult.Error -> {
                    _uiState.update { it.copy(output = result.message, isRunning = false) }
                }
                else -> _uiState.update { it.copy(output = "Unavailable", isRunning = false) }
            }
        }
    }

    fun compareAll() {
        val state = _uiState.value
        if (state.prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true) }
            val results = state.installedModelIds.map { modelId ->
                inferenceRuntime.ensureModelLoaded(modelId)
                val output = when (val r = inferenceRuntime.complete(
                    CompletionRequest(modelId, state.prompt, state.preset),
                )) {
                    is RuntimeResult.Success -> r.data
                    is RuntimeResult.Error -> r.message
                    else -> "Unsupported"
                }
                CompareResult(modelId, output)
            }
            petEventBus.emit(PetEvent.PromptLabRun)
            _uiState.update { it.copy(compareResults = results, isRunning = false) }
        }
    }

    fun saveFavorite() {
        val state = _uiState.value
        viewModelScope.launch {
            promptRepository.savePrompt(
                SavedPrompt(
                    title = state.preset?.label ?: "Custom",
                    body = state.prompt,
                    preset = state.preset,
                    isFavorite = true,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }
}

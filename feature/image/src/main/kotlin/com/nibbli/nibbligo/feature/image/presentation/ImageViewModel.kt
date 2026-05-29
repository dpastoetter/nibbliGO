package com.nibbli.nibbligo.feature.image.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.VisionRequest
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImageUiState(
    val installedModelIds: List<String> = emptyList(),
    val selectedModelId: String? = null,
    val imageUri: String = "",
    val question: String = "What is in this image?",
    val result: String? = null,
    val unsupported: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
class ImageViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val inferenceRuntime: InferenceRuntime,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageUiState())
    val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val installed = modelRepository.getInstalledModelIds()
            val visionModel = installed.firstOrNull {
                inferenceRuntime.capabilitiesFor(it).supportsVision
            }
            _uiState.update {
                it.copy(
                    installedModelIds = installed,
                    selectedModelId = visionModel ?: installed.firstOrNull(),
                    unsupported = visionModel == null,
                )
            }
        }
    }

    fun setImageUri(uri: String) = _uiState.update { it.copy(imageUri = uri) }
    fun setQuestion(q: String) = _uiState.update { it.copy(question = q) }

    fun analyze() {
        val state = _uiState.value
        val modelId = state.selectedModelId ?: return
        val caps = inferenceRuntime.capabilitiesFor(modelId)
        if (!caps.supportsVision) {
            _uiState.update { it.copy(unsupported = true, result = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, unsupported = false) }
            inferenceRuntime.ensureModelLoaded(modelId)
            when (val result = inferenceRuntime.analyzeImage(
                VisionRequest(modelId, state.imageUri, state.question),
            )) {
                is RuntimeResult.Success -> _uiState.update { it.copy(result = result.data, isLoading = false) }
                RuntimeResult.Unsupported -> _uiState.update { it.copy(unsupported = true, isLoading = false) }
                is RuntimeResult.Error -> _uiState.update { it.copy(result = result.message, isLoading = false) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

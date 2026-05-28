package com.nibbli.nibbligo.feature.audio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.RecordingRepository
import com.nibbli.nibbligo.core.model.AudioRecording
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioUiState(
    val recordings: List<AudioRecording> = emptyList(),
    val selectedModelId: String? = null,
    val isRecording: Boolean = false,
    val status: String? = null,
)

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val modelRepository: ModelRepository,
    private val inferenceRuntime: InferenceRuntime,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val installed = modelRepository.getInstalledModelIds()
            val audioModel = installed.firstOrNull {
                inferenceRuntime.capabilitiesFor(it).supportsAudio
            }
            _uiState.update { it.copy(selectedModelId = audioModel ?: installed.firstOrNull()) }
            recordingRepository.observeRecordings().collect { list ->
                _uiState.update { it.copy(recordings = list) }
            }
        }
    }

    fun simulateRecord() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = true, status = "Recording…") }
            val uri = "file:///data/user/0/com.nibbli.nibbligo/demo_recording.m4a"
            val id = recordingRepository.saveRecording(
                AudioRecording(
                    uri = uri,
                    durationMs = 12_000,
                    transcript = null,
                    summary = null,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            _uiState.update { it.copy(isRecording = false, status = "Saved recording #$id") }
        }
    }

    fun transcribeLatest(summarize: Boolean) {
        val recording = _uiState.value.recordings.firstOrNull()
        val modelId = _uiState.value.selectedModelId
        if (recording == null || modelId == null) {
            _uiState.update { it.copy(status = "Need a recording and scribe model") }
            return
        }
        viewModelScope.launch {
            inferenceRuntime.ensureModelLoaded(modelId)
            when (val result = inferenceRuntime.transcribeAudio(
                TranscriptionRequest(modelId, recording.uri, summarize),
            )) {
                is RuntimeResult.Success -> {
                    recordingRepository.updateTranscript(recording.id, result.data, null)
                    _uiState.update { it.copy(status = "Transcribed on-device") }
                }
                RuntimeResult.Unsupported -> _uiState.update { it.copy(status = "Model lacks audio support") }
                is RuntimeResult.Error -> _uiState.update { it.copy(status = result.message) }
                else -> Unit
            }
        }
    }
}

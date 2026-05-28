package com.nibbli.nibbligo.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversationId: Long? = null,
    val messages: List<ChatMessage> = emptyList(),
    val installedModelIds: List<String> = emptyList(),
    val selectedModelId: String? = null,
    val input: String = "",
    val streamingText: String? = null,
    val isStreaming: Boolean = false,
    val generationParams: GenerationParams = GenerationParams(),
    val reasoningNotes: String = "",
    val showReasoning: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val inferenceRuntime: InferenceRuntime,
    private val petEventBus: PetEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val installed = modelRepository.getInstalledModelIds()
            val defaultModel = userPreferencesRepository.defaultModelId.first()
            val params = userPreferencesRepository.generationParams.first()
            _uiState.update {
                it.copy(
                    installedModelIds = installed,
                    selectedModelId = defaultModel ?: installed.firstOrNull(),
                    generationParams = params,
                )
            }
        }
    }

    fun updateInput(value: String) = _uiState.update { it.copy(input = value) }
    fun selectModel(modelId: String) = _uiState.update { it.copy(selectedModelId = modelId) }
    fun updateParams(params: GenerationParams) = _uiState.update { it.copy(generationParams = params) }
    fun toggleReasoning() = _uiState.update { it.copy(showReasoning = !it.showReasoning) }
    fun updateReasoningNotes(notes: String) = _uiState.update { it.copy(reasoningNotes = notes) }

    fun newConversation() {
        viewModelScope.launch {
            val modelId = _uiState.value.selectedModelId ?: return@launch
            val id = chatRepository.createConversation(modelId, "New chat")
            _uiState.update { it.copy(conversationId = id, messages = emptyList(), error = null) }
            chatRepository.observeMessages(id).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val modelId = state.selectedModelId
        val text = state.input.trim()
        if (modelId == null || text.isBlank() || state.isStreaming) return

        viewModelScope.launch {
            var conversationId = state.conversationId
            if (conversationId == null) {
                conversationId = chatRepository.createConversation(modelId, text.take(32))
                _uiState.update { it.copy(conversationId = conversationId) }
            }

            val now = System.currentTimeMillis()
            val userMessage = ChatMessage(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                notes = if (state.showReasoning) state.reasoningNotes.takeIf { it.isNotBlank() } else null,
                timestampMillis = now,
                modelId = modelId,
            )
            chatRepository.saveMessage(userMessage)
            _uiState.update { it.copy(input = "", isStreaming = true, streamingText = "", error = null) }

            when (val load = inferenceRuntime.ensureModelLoaded(modelId)) {
                is com.nibbli.nibbligo.core.model.RuntimeResult.Error -> {
                    _uiState.update { it.copy(isStreaming = false, error = load.message) }
                    return@launch
                }
                else -> Unit
            }

            val allMessages = chatRepository.observeMessages(conversationId).first() + userMessage
            var assistantContent = ""
            inferenceRuntime.streamChat(
                ChatInferenceRequest(modelId, allMessages, state.generationParams),
            ).collect { chunk ->
                if (!chunk.isComplete) {
                    assistantContent += chunk.token
                    _uiState.update { it.copy(streamingText = assistantContent) }
                }
            }

            val assistantMessage = ChatMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = assistantContent,
                timestampMillis = System.currentTimeMillis(),
                modelId = modelId,
            )
            chatRepository.saveMessage(assistantMessage)
            petEventBus.emit(PetEvent.AssistantSuccess)
            _uiState.update { it.copy(isStreaming = false, streamingText = null) }
        }
    }
}

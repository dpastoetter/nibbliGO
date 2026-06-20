package com.nibbli.nibbligo.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.PetTurnRequest
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.pet.llm.CompanionMemoryStore
import com.nibbli.nibbligo.core.pet.llm.CompanionTurnLog
import com.nibbli.nibbligo.core.pet.llm.LiteRtPetReactionGenerator
import com.nibbli.nibbligo.core.pet.llm.PetModelResolver
import com.nibbli.nibbligo.core.pet.llm.PetOnboardingPrompt
import com.nibbli.nibbligo.core.pet.llm.PetPromptBuilder
import com.nibbli.nibbligo.core.pet.llm.PetReactionParser
import com.nibbli.nibbligo.core.pet.llm.PetReactionRequest
import com.nibbli.nibbligo.core.pet.llm.PetTalkChatRecorder
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversationId: Long? = null,
    val petName: String = "nibbli",
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val streamingText: String? = null,
    val isStreaming: Boolean = false,
    val hasPetModel: Boolean = false,
    val petModelLabel: String = "",
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val petRepository: PetRepository,
    private val inferenceRuntime: InferenceRuntime,
    private val petEventBus: PetEventBus,
    private val petTalkChatRecorder: PetTalkChatRecorder,
    private val petModelResolver: PetModelResolver,
    private val companionMemoryStore: CompanionMemoryStore,
    private val companionTurnLog: CompanionTurnLog,
) : ViewModel() {

    private val engine = PetSimulationEngine()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val conversationIdFlow = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            val modelId = runCatching { petModelResolver.resolve() }.getOrNull()
            val installed = modelRepository.getInstalledModelIds()
            val petState = petRepository.getPetState()
            _uiState.update {
                it.copy(
                    petName = petState.name,
                    hasPetModel = modelId != null && modelId in installed,
                    petModelLabel = modelId ?: "",
                )
            }
            if (modelId != null) {
                val conversationId = chatRepository.getOrCreateConversation(
                    PetTalkChatRecorder.CONVERSATION_TITLE,
                    modelId,
                )
                setActiveConversation(conversationId)
            }
        }
        viewModelScope.launch {
            petRepository.observePetState()
                .map { it.name }
                .distinctUntilChanged()
                .collect { name ->
                    _uiState.update { ui ->
                        if (ui.petName == name) ui else ui.copy(petName = name)
                    }
                }
        }
        viewModelScope.launch {
            conversationIdFlow
                .flatMapLatest { id ->
                    if (id == null) {
                        flowOf(emptyList())
                    } else {
                        chatRepository.observeMessages(id).map { messages ->
                            messages.filter { msg ->
                                msg.role == MessageRole.USER || msg.role == MessageRole.ASSISTANT
                            }
                        }
                    }
                }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
        }
    }

    fun updateInput(value: String) = _uiState.update { it.copy(input = value) }

    fun clearChat() {
        viewModelScope.launch {
            val conversationId = _uiState.value.conversationId ?: return@launch
            chatRepository.deleteMessagesForConversation(conversationId)
            runCatching {
                val modelId = petModelResolver.resolve()
                inferenceRuntime.resetHomeTalkSession(modelId)
            }
            _uiState.update { it.copy(streamingText = null, input = "", error = null) }
        }
    }

    fun sendMessage(text: String = _uiState.value.input) {
        val state = _uiState.value
        val trimmed = text.trim()
        if (trimmed.isBlank() || state.isStreaming) return

        viewModelScope.launch {
            val modelId = runCatching { petModelResolver.resolve() }.getOrNull()
            if (modelId == null) {
                _uiState.update {
                    it.copy(error = "Install a Pixel Friend model under Manage → Models.")
                }
                return@launch
            }

            var conversationId = state.conversationId
            if (conversationId == null) {
                conversationId = chatRepository.getOrCreateConversation(
                    PetTalkChatRecorder.CONVERSATION_TITLE,
                    modelId,
                )
                setActiveConversation(conversationId)
            }

            val now = System.currentTimeMillis()
            _uiState.update { it.copy(input = "", isStreaming = true, streamingText = "", error = null) }

            var petState = petRepository.getPetState()
            petState = engine.tick(petState, now).state
            companionMemoryStore.ensureMigrated()
            petState = petRepository.getPetState()
            val personality = userPreferencesRepository.petPersonality.first()
            val profile = userPreferencesRepository.petOnboardingProfile.first()
            val onboarding = PetOnboardingPrompt.formatForSystemInstruction(profile, petState.name)
            val systemInstruction = PetPromptBuilder.homeTalkSystemInstruction(onboarding, petState.name)

            when (val loadResult = inferenceRuntime.ensureHomeTalkSession(modelId, systemInstruction)) {
                is RuntimeResult.Error -> {
                    _uiState.update { it.copy(isStreaming = false, error = loadResult.message) }
                    return@launch
                }
                RuntimeResult.LowMemory -> {
                    _uiState.update {
                        it.copy(isStreaming = false, error = "Not enough memory to load the model.")
                    }
                    return@launch
                }
                RuntimeResult.Unsupported -> {
                    _uiState.update {
                        it.copy(isStreaming = false, error = "This model isn't supported here.")
                    }
                    return@launch
                }
                else -> Unit
            }

            val recentTurns = companionTurnLog.recentTurns(excludeCurrentUserMessage = trimmed)
            val request = PetReactionRequest(
                state = petState,
                userMessage = trimmed,
                personality = personality,
                caretakerName = profile.caretakerName.trim().takeIf { it.isNotBlank() },
                recentTurns = recentTurns,
            )
            val parts = PetPromptBuilder.buildHomeTalkParts(request, modelId, onboarding)

            val rawBuilder = StringBuilder()
            inferenceRuntime.streamHomeTalk(
                PetTurnRequest(
                    modelId = modelId,
                    systemInstruction = parts.systemInstruction,
                    userMessage = parts.userMessage,
                ),
            ).collect { chunk ->
                if (chunk.isComplete) return@collect
                if (chunk.token.startsWith(LiteRtPetReactionGenerator.LITERT_ERROR_PREFIX)) {
                    _uiState.update { it.copy(error = chunk.token.removePrefix(LiteRtPetReactionGenerator.LITERT_ERROR_PREFIX)) }
                    return@collect
                }
                rawBuilder.append(chunk.token)
                val display = PetReactionParser.stripForStreaming(
                    rawBuilder.toString(),
                    petState.name,
                    request.caretakerName,
                )
                _uiState.update { it.copy(streamingText = display) }
            }

            val raw = rawBuilder.toString().trim()
            val reaction = if (raw.isNotBlank()) {
                PetReactionParser.parseTalk(raw, petState.name, request.caretakerName)
            } else {
                PetReactionParser.fallback(request)
            }
            val parsed = reaction.dialogue

            if (parsed.isBlank()) {
                _uiState.update {
                    it.copy(isStreaming = false, streamingText = null, error = "Model returned an empty reply.")
                }
                return@launch
            }

            petTalkChatRecorder.recordSimpleTurn(
                conversationId = conversationId,
                modelId = modelId,
                userText = trimmed,
                assistant = parsed,
                timestampMillis = now,
            )
            inferenceRuntime.resetHomeTalkSession(modelId)
            _uiState.update { it.copy(isStreaming = false, streamingText = null) }
            viewModelScope.launch {
                petEventBus.emit(PetEvent.AssistantSuccess)
            }
        }
    }

    private fun setActiveConversation(id: Long?) {
        conversationIdFlow.value = id
        _uiState.update { it.copy(conversationId = id, error = null) }
    }
}

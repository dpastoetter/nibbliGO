package com.nibbli.nibbligo.feature.chat.presentation

import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendMessage_keepsAssistantReplyInMessagesAfterStreamingEnds() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val runtime = FakeInferenceRuntime(reply = "Hello from nibbli")
        val viewModel = ChatViewModel(
            chatRepository = chatRepo,
            modelRepository = FakeModelRepository(),
            userPreferencesRepository = FakeUserPreferencesRepository(),
            inferenceRuntime = runtime,
            petEventBus = PetEventBus(),
        )
        advanceUntilIdle()

        viewModel.selectModel("functiongemma-270m")
        viewModel.updateInput("Hi")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.streamingText)
        assertEquals(false, state.isStreaming)
        assertEquals(2, state.messages.size)
        assertEquals(MessageRole.USER, state.messages[0].role)
        assertEquals("Hi", state.messages[0].content)
        assertEquals(MessageRole.ASSISTANT, state.messages[1].role)
        assertEquals("Hello from nibbli", state.messages[1].content)
    }

    @Test
    fun newConversation_switchesMessageObservation() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val viewModel = ChatViewModel(
            chatRepository = chatRepo,
            modelRepository = FakeModelRepository(),
            userPreferencesRepository = FakeUserPreferencesRepository(),
            inferenceRuntime = FakeInferenceRuntime(reply = "First"),
            petEventBus = PetEventBus(),
        )
        advanceUntilIdle()
        viewModel.selectModel("functiongemma-270m")
        viewModel.updateInput("One")
        viewModel.sendMessage()
        advanceUntilIdle()

        viewModel.newConversation()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        viewModel.updateInput("Two")
        viewModel.sendMessage()
        advanceUntilIdle()

        val roles = viewModel.uiState.value.messages.map { it.role }
        assertEquals(listOf(MessageRole.USER, MessageRole.ASSISTANT), roles)
        assertEquals("Two", viewModel.uiState.value.messages[0].content)
    }
}

private class FakeChatRepository : ChatRepository {
    private var nextConversationId = 1L
    private var nextMessageId = 1L
    private val messagesByConversation = mutableMapOf<Long, MutableStateFlow<List<ChatMessage>>>()

    override fun observeConversations(): Flow<List<Conversation>> = flowOf(emptyList())

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        messagesByConversation.getOrPut(conversationId) { MutableStateFlow(emptyList()) }

    override suspend fun createConversation(modelId: String, title: String): Long {
        val id = nextConversationId++
        messagesByConversation[id] = MutableStateFlow(emptyList())
        return id
    }

    override suspend fun saveMessage(message: ChatMessage) {
        val flow = messagesByConversation.getValue(message.conversationId)
        val stored = message.copy(id = nextMessageId++)
        flow.value = flow.value + stored
    }

    override suspend fun updateConversation(conversation: Conversation) = Unit

    override suspend fun deleteAllConversations() {
        messagesByConversation.clear()
    }
}

private class FakeModelRepository : ModelRepository {
    override fun observeCatalog(): Flow<List<ModelInfo>> = flowOf(emptyList())
    override fun observeInstalled(): Flow<List<InstalledModel>> = flowOf(emptyList())
    override suspend fun getCatalog(): List<ModelInfo> = emptyList()
    override suspend fun getInstalled(): List<InstalledModel> = emptyList()
    override suspend fun isInstalled(modelId: String) = true
    override suspend fun install(modelId: String) = Result.success(Unit)
    override suspend fun uninstall(modelId: String) = Result.success(Unit)
    override suspend fun getInstalledModelIds() = listOf("functiongemma-270m")
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    override val defaultModelId = flowOf("functiongemma-270m")
    override val generationParams = flowOf(GenerationParams())
    override val allowDownloads = flowOf(true)
    override val preferredRuntimeKind = flowOf("LITERT")
    override val petPersonality = flowOf(PetPersonality.PLAYFUL)
    override val usePetLlmReactions = flowOf(true)
    override val petCommentOnAgentWork = flowOf(true)
    override val petMoodPulseMode = flowOf(PetMoodPulseMode.NORMAL)
    override val themeMode = flowOf(AppThemeMode.SYSTEM)
    override val showDoTab = flowOf(false)
    override suspend fun setDefaultModelId(modelId: String?) = Unit
    override suspend fun setGenerationParams(params: GenerationParams) = Unit
    override suspend fun setAllowDownloads(allowed: Boolean) = Unit
    override suspend fun setPreferredRuntimeKind(kind: String) = Unit
    override suspend fun setPetPersonality(personality: PetPersonality) = Unit
    override suspend fun setUsePetLlmReactions(enabled: Boolean) = Unit
    override suspend fun setPetCommentOnAgentWork(enabled: Boolean) = Unit
    override suspend fun setPetMoodPulseMode(mode: PetMoodPulseMode) = Unit
    override suspend fun setThemeMode(mode: AppThemeMode) = Unit
    override suspend fun setShowDoTab(show: Boolean) = Unit
}

private class FakeInferenceRuntime(
    private val reply: String,
) : InferenceRuntime {
    override val runtimeKind: RuntimeKind = RuntimeKind.LITERT

    override suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean) =
        RuntimeResult.Success(Unit)

    override fun unloadModel(modelId: String) = Unit

    override fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk> = flowOf(
        InferenceChunk(reply.take(5)),
        InferenceChunk(reply.drop(5)),
        InferenceChunk("", isComplete = true),
    )

    override suspend fun complete(request: CompletionRequest) =
        RuntimeResult.Success(reply)

    override suspend fun analyzeImage(request: VisionRequest) = RuntimeResult.Unsupported

    override suspend fun transcribeAudio(request: TranscriptionRequest) = RuntimeResult.Unsupported

    override suspend fun runBenchmark(modelId: String) = RuntimeResult.Unsupported

    override fun capabilitiesFor(modelId: String) = ModelCapabilities(
        modelId = modelId,
        supportsChat = true,
        supportsVision = false,
        supportsAudio = false,
        supportsStreaming = true,
    )

    override suspend fun generateWithTools(request: AgentRequest) =
        RuntimeResult.Success(AgentTurn.FinalText("ok"))
}

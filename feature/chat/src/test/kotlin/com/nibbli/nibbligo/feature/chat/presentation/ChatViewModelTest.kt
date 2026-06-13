package com.nibbli.nibbligo.feature.chat.presentation

import android.content.Context
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.ChatPromptMode
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetTurnRequest
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import com.nibbli.nibbligo.core.pet.llm.PetModelResolver
import com.nibbli.nibbligo.core.pet.llm.PetTalkChatRecorder
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import org.robolectric.RuntimeEnvironment
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
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
    fun sendMessage_keepsAssistantReplyWithoutSystemMessages() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val runtime = FakeInferenceRuntime(reply = "Hello from nibbli")
        val viewModel = createViewModel(
            chatRepository = chatRepo,
            inferenceRuntime = runtime,
        )
        advanceUntilIdle()

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
    fun init_restoresPixelFriendThread() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val modelId = "smollm2-360m-instruct"
        val conversationId = chatRepo.getOrCreateConversation(
            PetTalkChatRecorder.CONVERSATION_TITLE,
            modelId,
        )
        chatRepo.saveMessage(
            ChatMessage(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = "Earlier message",
                timestampMillis = 1L,
                modelId = modelId,
            ),
        )

        val viewModel = createViewModel(
            chatRepository = chatRepo,
            inferenceRuntime = FakeInferenceRuntime(reply = "unused"),
            petModelId = modelId,
        )
        advanceUntilIdle()

        assertEquals(conversationId, viewModel.uiState.value.conversationId)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Earlier message", viewModel.uiState.value.messages[0].content)
    }

    @Test
    fun clearChat_removesMessages() = runTest(testDispatcher) {
        val chatRepo = FakeChatRepository()
        val viewModel = createViewModel(
            chatRepository = chatRepo,
            inferenceRuntime = FakeInferenceRuntime(reply = "Hi"),
            petModelId = "smollm2-360m-instruct",
        )
        advanceUntilIdle()
        viewModel.updateInput("Hello")
        viewModel.sendMessage()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isNotEmpty())

        viewModel.clearChat()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}

private fun createViewModel(
    chatRepository: FakeChatRepository,
    inferenceRuntime: FakeInferenceRuntime,
    petModelId: String = "smollm2-360m-instruct",
): ChatViewModel {
    val prefs = FakeUserPreferencesRepository(petModelId)
    val petRepo = FakePetRepository()
    val recorder = PetTalkChatRecorder(
        chatRepository = chatRepository,
        petModelResolver = PetModelResolver(
            inferenceRuntime,
            prefs,
            object : com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate {
                override suspend fun hasUsableModel() = true
                override suspend fun firstUsableModelId() = petModelId
            },
        ),
        userPreferencesRepository = prefs,
    )
    return ChatViewModel(
        chatRepository = chatRepository,
        modelRepository = FakeModelRepository(petModelId),
        userPreferencesRepository = prefs,
        petRepository = petRepo,
        inferenceRuntime = inferenceRuntime,
        petEventBus = PetEventBus(),
        petTalkChatRecorder = recorder,
        petModelResolver = PetModelResolver(
            inferenceRuntime,
            prefs,
            object : com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate {
                override suspend fun hasUsableModel() = true
                override suspend fun firstUsableModelId() = petModelId
            },
        ),
    )
}

private class FakePetRepository : PetRepository {
    override fun observePetState() = flowOf(PetState())
    override suspend fun getPetState() = PetState()
    override suspend fun savePetState(state: PetState) = Unit
}

private class FakeChatRepository : ChatRepository {
    private var nextConversationId = 1L
    private var nextMessageId = 1L
    private val messagesByConversation = mutableMapOf<Long, MutableStateFlow<List<ChatMessage>>>()
    private val conversationsByTitle = mutableMapOf<String, Conversation>()

    override fun observeConversations(): Flow<List<Conversation>> =
        flowOf(conversationsByTitle.values.toList())

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        messagesByConversation.getOrPut(conversationId) { MutableStateFlow(emptyList()) }

    override suspend fun createConversation(modelId: String, title: String): Long {
        val id = nextConversationId++
        messagesByConversation[id] = MutableStateFlow(emptyList())
        conversationsByTitle[title] = Conversation(
            id = id,
            title = title,
            modelId = modelId,
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
        )
        return id
    }

    override suspend fun findConversationByTitle(title: String): Conversation? =
        conversationsByTitle[title]

    override suspend fun getOrCreateConversation(title: String, modelId: String): Long {
        val existing = conversationsByTitle[title]
        if (existing != null) return existing.id
        return createConversation(modelId, title)
    }

    override suspend fun saveMessage(message: ChatMessage) {
        val flow = messagesByConversation.getValue(message.conversationId)
        val stored = message.copy(id = nextMessageId++)
        flow.value = flow.value + stored
    }

    override suspend fun updateConversation(conversation: Conversation) = Unit

    override suspend fun deleteMessagesForConversation(conversationId: Long) {
        messagesByConversation[conversationId]?.value = emptyList()
    }

    override suspend fun deleteAllConversations() {
        messagesByConversation.clear()
    }
}

private class FakeModelRepository(
    private val petModelId: String,
) : ModelRepository {
    override fun observeCatalog(): Flow<List<ModelInfo>> = flowOf(emptyList())
    override fun observeInstalled(): Flow<List<InstalledModel>> = flowOf(emptyList())
    override suspend fun getCatalog(): List<ModelInfo> = emptyList()
    override suspend fun getInstalled(): List<InstalledModel> = emptyList()
    override suspend fun isInstalled(modelId: String) = true
    override suspend fun install(modelId: String) = Result.success(Unit)
    override suspend fun uninstall(modelId: String) = Result.success(Unit)
    override suspend fun getInstalledModelIds() = listOf(petModelId)
}

private class FakeUserPreferencesRepository(
    private val petModel: String,
) : UserPreferencesRepository {
    override val defaultModelId = flowOf(petModel)
    override val petModelId = flowOf<String?>(petModel)
    override val generationParams = flowOf(GenerationParams())
    override val chatPromptMode = flowOf(ChatPromptMode.PIXEL_FRIEND)
    override val allowDownloads = flowOf(true)
    override val preferredRuntimeKind = flowOf("LITERT")
    override val petPersonality = flowOf(PetPersonality.PLAYFUL)
    override val usePetLlmReactions = flowOf(true)
    override val petCommentOnAgentWork = flowOf(true)
    override val petMoodPulseMode = flowOf(PetMoodPulseMode.NORMAL)
    override val themeMode = flowOf(AppThemeMode.SYSTEM)
    override val accentPalette = flowOf(AppAccentPalette.TEAL)
    override val showDoTab = flowOf(false)
    override val litertAccelerator = flowOf(LiteRtAcceleratorPreference.AUTO)
    override val petOnboardingProfile = flowOf(PetOnboardingProfile(completed = true))
    override val onboardingCompleted = flowOf(true)
    override val modelSetupPromptDismissed = flowOf(false)
    override val termsAccepted = flowOf(false)
    override val petSoundHapticsEnabled = flowOf(true)
    override val petNotificationsEnabled = flowOf(true)
    override val lcdCoachMarksDismissed = flowOf(false)
    override val firstTalkGreetingSent = flowOf(false)
    override suspend fun getPetNotificationsEnabled(): Boolean = true
    override suspend fun setPetSoundHapticsEnabled(enabled: Boolean) = Unit
    override suspend fun setPetNotificationsEnabled(enabled: Boolean) = Unit
    override suspend fun setLcdCoachMarksDismissed(dismissed: Boolean) = Unit
    override suspend fun setFirstTalkGreetingSent(sent: Boolean) = Unit
    override suspend fun setDefaultModelId(modelId: String?) = Unit
    override suspend fun setPetModelId(modelId: String?) = Unit
    override suspend fun setGenerationParams(params: GenerationParams) = Unit
    override suspend fun setChatPromptMode(mode: ChatPromptMode) = Unit
    override suspend fun setAllowDownloads(allowed: Boolean) = Unit
    override suspend fun setPreferredRuntimeKind(kind: String) = Unit
    override suspend fun setPetPersonality(personality: PetPersonality) = Unit
    override suspend fun setUsePetLlmReactions(enabled: Boolean) = Unit
    override suspend fun setPetCommentOnAgentWork(enabled: Boolean) = Unit
    override suspend fun setPetMoodPulseMode(mode: PetMoodPulseMode) = Unit
    override suspend fun setThemeMode(mode: AppThemeMode) = Unit
    override suspend fun setAccentPalette(palette: AppAccentPalette) = Unit
    override suspend fun setShowDoTab(show: Boolean) = Unit
    override suspend fun setPetOnboardingProfile(profile: PetOnboardingProfile) = Unit
    override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun setTermsAccepted(acceptedAtMillis: Long) = Unit
    override suspend fun setLitertAccelerator(preference: LiteRtAcceleratorPreference) = Unit
}

private class FakeInferenceRuntime(
    private val reply: String,
) : InferenceRuntime {
    override val runtimeKind: RuntimeKind = RuntimeKind.LITERT

    override suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean) =
        RuntimeResult.Success(Unit)

    override suspend fun ensurePetModelLoaded(modelId: String, systemInstruction: String) =
        RuntimeResult.Success(Unit)

    override suspend fun ensureAgentModelLoaded(modelId: String) =
        RuntimeResult.Success(Unit)

    override suspend fun ensureHomeTalkSession(modelId: String, systemInstruction: String) =
        RuntimeResult.Success(Unit)

    override fun unloadModel(modelId: String) = Unit

    override fun streamHomeTalk(request: PetTurnRequest): Flow<InferenceChunk> = flowOf(
        InferenceChunk(reply.take(5)),
        InferenceChunk(reply.drop(5)),
        InferenceChunk("", isComplete = true),
    )

    override fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk> = flowOf(
        InferenceChunk(reply),
        InferenceChunk("", isComplete = true),
    )

    override suspend fun complete(request: CompletionRequest) =
        RuntimeResult.Success(reply)

    override suspend fun analyzeImage(request: VisionRequest) = RuntimeResult.Unsupported

    override suspend fun transcribeAudio(request: TranscriptionRequest) = RuntimeResult.Unsupported

    override suspend fun runBenchmark(modelId: String) = RuntimeResult.Unsupported

    override suspend fun runPetBenchmark(modelId: String) = RuntimeResult.Unsupported

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

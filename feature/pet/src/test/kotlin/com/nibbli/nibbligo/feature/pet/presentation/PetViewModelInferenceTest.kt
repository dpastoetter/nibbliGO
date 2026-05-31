package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import com.nibbli.nibbligo.core.domain.assist.AssistNavigationBus
import com.nibbli.nibbligo.core.domain.assist.AssistVoiceRequestBus
import com.nibbli.nibbligo.core.domain.pet.PetDeepLinkBus
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.ChatPromptMode
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.pet.llm.PetReaction
import com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader
import com.nibbli.nibbligo.core.pet.llm.PetModelResolver
import com.nibbli.nibbligo.core.pet.llm.PetReactionPort
import com.nibbli.nibbligo.core.pet.llm.PetReactionRequest
import com.nibbli.nibbligo.core.pet.llm.PetReactionStreamEvent
import com.nibbli.nibbligo.core.pet.llm.PetTalkChatRecorder
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PetViewModelInferenceTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun careInteraction_invokesPetReactionPortGenerate() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val reactionPort = RecordingPetReactionPort()
        val viewModel = createViewModel(
            context = RuntimeEnvironment.getApplication(),
            reactionPort = reactionPort,
        )
        delay(50)

        val callsBefore = reactionPort.generateCalls.size
        viewModel.onInteraction(PetInteraction.FEED_MEAL)
        withTimeout(5_000) {
            while (reactionPort.generateCalls.size <= callsBefore) {
                delay(50)
            }
        }

        assertTrue(
            "Care interaction should call PetReactionPort.generate",
            reactionPort.generateCalls.size > callsBefore,
        )
        val lastRequest = reactionPort.generateCalls.last()
        assertEquals("feed meal", lastRequest.lastAction)
    }

    @Test
    fun careInteraction_skippedWhileUserTalkActive() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val reactionPort = HangingStreamPetReactionPort()
        val viewModel = createViewModel(
            context = RuntimeEnvironment.getApplication(),
            reactionPort = reactionPort,
        )
        delay(50)

        viewModel.onTalkSend("Hello pet!")
        withTimeout(5_000) {
            while (!viewModel.uiState.value.talkLcdMode) {
                delay(20)
            }
        }

        viewModel.onInteraction(PetInteraction.FEED_MEAL)
        delay(200)

        assertEquals(0, reactionPort.generateCalls.size)
        assertTrue(viewModel.uiState.value.talkLcdMode)
    }

    private fun createViewModel(
        context: Context,
        reactionPort: PetReactionPort,
    ): PetViewModel {
        val now = System.currentTimeMillis()
        val petState = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
        )
        return PetViewModel(
            context = context,
            petRepository = FakePetRepository(petState),
            petEventBus = PetEventBus(),
            petReactionPort = reactionPort,
            petTalkChatRecorder = createPetTalkChatRecorder(),
            userPreferencesRepository = FakeUserPreferencesRepository(),
            engine = PetSimulationEngine(),
            assistNavigationBus = AssistNavigationBus(AssistVoiceRequestBus()),
            liteRtModelPreloader = createIdlePreloader(),
            petDeepLinkBus = PetDeepLinkBus(),
            modelAvailabilityGate = object : ModelAvailabilityGate {
                override suspend fun hasUsableModel(): Boolean = true
                override suspend fun firstUsableModelId(): String = "smollm2-360m-instruct"
            },
            modelRepository = FakeModelRepository(),
        )
    }

    private fun createPetTalkChatRecorder(): PetTalkChatRecorder {
        val prefs = FakeUserPreferencesRepository()
        val gate = object : ModelAvailabilityGate {
            override suspend fun hasUsableModel(): Boolean = true
            override suspend fun firstUsableModelId(): String = "smollm2-360m-instruct"
        }
        val runtime = object : InferenceRuntime {
            override val runtimeKind = com.nibbli.nibbligo.core.model.RuntimeKind.LITERT
            override suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean) =
                com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
            override fun unloadModel(modelId: String) = Unit
            override fun resetChatSession(modelId: String) = Unit
            override fun streamChat(request: com.nibbli.nibbligo.core.model.ChatInferenceRequest) =
                kotlinx.coroutines.flow.emptyFlow<com.nibbli.nibbligo.core.model.InferenceChunk>()
            override suspend fun complete(request: com.nibbli.nibbligo.core.model.CompletionRequest) =
                com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
            override suspend fun analyzeImage(request: com.nibbli.nibbligo.core.model.VisionRequest) =
                com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
            override suspend fun transcribeAudio(request: com.nibbli.nibbligo.core.model.TranscriptionRequest) =
                com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
            override suspend fun runBenchmark(modelId: String) =
                com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
            override fun capabilitiesFor(modelId: String) = ModelCapabilities(
                modelId = modelId,
                supportsChat = true,
                supportsVision = false,
                supportsAudio = false,
                supportsStreaming = true,
            )
            override suspend fun generateWithTools(request: com.nibbli.nibbligo.core.model.AgentRequest) =
                com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
        }
        return PetTalkChatRecorder(
            chatRepository = FakeChatRepository(),
            petModelResolver = PetModelResolver(runtime, prefs, gate),
            userPreferencesRepository = prefs,
        )
    }
}

private fun createIdlePreloader(): LiteRtModelPreloader {
    val noModelGate = object : ModelAvailabilityGate {
        override suspend fun hasUsableModel(): Boolean = false
        override suspend fun firstUsableModelId(): String? = null
    }
    val idleRuntime = object : InferenceRuntime {
        override val runtimeKind = com.nibbli.nibbligo.core.model.RuntimeKind.LITERT
        override suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean) =
            com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
        override fun unloadModel(modelId: String) = Unit
        override fun streamChat(request: com.nibbli.nibbligo.core.model.ChatInferenceRequest) =
            kotlinx.coroutines.flow.emptyFlow<com.nibbli.nibbligo.core.model.InferenceChunk>()
        override suspend fun complete(request: com.nibbli.nibbligo.core.model.CompletionRequest) =
            com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
        override suspend fun analyzeImage(request: com.nibbli.nibbligo.core.model.VisionRequest) =
            com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
        override suspend fun transcribeAudio(request: com.nibbli.nibbligo.core.model.TranscriptionRequest) =
            com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
        override suspend fun runBenchmark(modelId: String) =
            com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
        override suspend fun generateWithTools(request: com.nibbli.nibbligo.core.model.AgentRequest) =
            com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
        override fun capabilitiesFor(modelId: String) = ModelCapabilities(
            modelId = modelId,
            supportsChat = false,
            supportsVision = false,
            supportsAudio = false,
            supportsStreaming = false,
        )
    }
    val prefs = FakeUserPreferencesRepository()
    val petRepo = object : PetRepository {
        override fun observePetState() = flowOf(PetState())
        override suspend fun getPetState() = PetState()
        override suspend fun savePetState(state: PetState) = Unit
    }
    return LiteRtModelPreloader(
        modelAvailabilityGate = noModelGate,
        petModelResolver = PetModelResolver(
            inferenceRuntime = idleRuntime,
            userPreferencesRepository = prefs,
            modelAvailabilityGate = noModelGate,
        ),
        inferenceRuntime = idleRuntime,
        userPreferencesRepository = prefs,
        petRepository = petRepo,
    )
}

private class RecordingPetReactionPort : PetReactionPort {
    val generateCalls = mutableListOf<PetReactionRequest>()

    override suspend fun generate(request: PetReactionRequest): PetReaction {
        generateCalls.add(request)
        return PetReaction(dialogue = "LLM care reply!")
    }

    override fun generateStream(request: PetReactionRequest): Flow<PetReactionStreamEvent> =
        flowOf(PetReactionStreamEvent.Done(PetReaction(dialogue = "streamed")))

    override suspend fun warmLoad() = Unit

    override suspend fun activeModelDisplayName(): String = "Test model"
}

private class HangingStreamPetReactionPort : PetReactionPort {
    val generateCalls = mutableListOf<PetReactionRequest>()

    override suspend fun generate(request: PetReactionRequest): PetReaction {
        generateCalls.add(request)
        return PetReaction(dialogue = "LLM care reply!")
    }

    override fun generateStream(request: PetReactionRequest): Flow<PetReactionStreamEvent> = flow {
        delay(10_000)
        emit(PetReactionStreamEvent.Done(PetReaction(dialogue = "streamed")))
    }

    override suspend fun warmLoad() = Unit

    override suspend fun activeModelDisplayName(): String = "Test model"
}

private class FakePetRepository(
    private var state: PetState,
) : PetRepository {
    override fun observePetState(): Flow<PetState> = flowOf(state)

    override suspend fun getPetState(): PetState = state

    override suspend fun savePetState(newState: PetState) {
        state = newState
    }
}

private class FakeChatRepository : ChatRepository {
    override fun observeConversations(): Flow<List<Conversation>> = flowOf(emptyList())

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        flowOf(emptyList())

    override suspend fun createConversation(modelId: String, title: String): Long = 1L

    override suspend fun findConversationByTitle(title: String): Conversation? = null

    override suspend fun getOrCreateConversation(title: String, modelId: String): Long = 1L

    override suspend fun saveMessage(message: ChatMessage) = Unit

    override suspend fun updateConversation(conversation: Conversation) = Unit

    override suspend fun deleteMessagesForConversation(conversationId: Long) = Unit

    override suspend fun deleteAllConversations() = Unit
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    override val defaultModelId = flowOf("smollm2-360m-instruct")
    override val petModelId = flowOf<String?>(null)
    override val generationParams = flowOf(GenerationParams())
    override val chatPromptMode = flowOf(ChatPromptMode.PURE_LLM)
    override val allowDownloads = flowOf(true)
    override val preferredRuntimeKind = flowOf("LITERT")
    override val petPersonality = flowOf(PetPersonality.PLAYFUL)
    override val usePetLlmReactions = flowOf(true)
    override val petCommentOnAgentWork = flowOf(false)
    override val petMoodPulseMode = flowOf(PetMoodPulseMode.OFF)
    override val themeMode = flowOf(AppThemeMode.SYSTEM)
    override val accentPalette = flowOf(AppAccentPalette.TEAL)
    override val showDoTab = flowOf(false)
    override val litertAccelerator = flowOf(LiteRtAcceleratorPreference.AUTO)
    override val petOnboardingProfile = flowOf(PetOnboardingProfile(completed = true))
    override val onboardingCompleted = flowOf(true)
    override val modelSetupPromptDismissed = flowOf(true)
    override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) = Unit
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
    override suspend fun setLitertAccelerator(preference: LiteRtAcceleratorPreference) = Unit
}

private class FakeModelRepository : ModelRepository {
    override fun observeCatalog(): Flow<List<ModelInfo>> = flowOf(emptyList())
    override fun observeInstalled(): Flow<List<InstalledModel>> = flowOf(
        listOf(
            InstalledModel(
                modelId = "smollm2-360m-instruct",
                localPath = "/tmp/model.litertlm",
                installedAtMillis = 0L,
                sizeBytes = 1_000_000L,
            ),
        ),
    )
    override suspend fun getCatalog(): List<ModelInfo> = emptyList()
    override suspend fun getInstalled(): List<InstalledModel> = emptyList()
    override suspend fun isInstalled(modelId: String): Boolean = true
    override suspend fun install(modelId: String): Result<Unit> = Result.success(Unit)
    override suspend fun uninstall(modelId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getInstalledModelIds(): List<String> = listOf("smollm2-360m-instruct")
}

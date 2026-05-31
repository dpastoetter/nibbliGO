package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetTurnRequest
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class LiteRtPetReactionGeneratorTest {

    @Test
    fun generate_loadFailure_returnsFallbackWithoutInference() = kotlinx.coroutines.test.runTest {
        val runtime = RecordingInferenceRuntime(
            homeTalkLoadResult = RuntimeResult.Error("load failed"),
        )
        val generator = createGenerator(runtime)

        val reaction = generator.generate(
            PetReactionRequest(state = PetState(), userMessage = "Hello!"),
        )

        assertEquals(0, runtime.homeTalkCompleteCalls.size)
        assertTrue(reaction.dialogue.contains("hiccuped"))
    }

    @Test
    fun generate_emptyPrimary_usesCompactRetry() = kotlinx.coroutines.test.runTest {
        val runtime = RecordingInferenceRuntime(
            homeTalkLoadResult = RuntimeResult.Success(Unit),
            homeTalkCompleteResults = listOf(
                RuntimeResult.Success(""),
                RuntimeResult.Success("Compact reply!|HAPPY"),
            ),
        )
        val generator = createGenerator(runtime)

        val reaction = generator.generate(
            PetReactionRequest(state = PetState(), userMessage = "Hello!"),
        )

        assertEquals(2, runtime.homeTalkCompleteCalls.size)
        assertEquals("Compact reply!", reaction.dialogue)
        assertTrue(runtime.homeTalkCompleteCalls.last().userMessage.contains("Caretaker: Hello!"))
    }

    @Test
    fun generate_statusEcho_usesCompactRetryThenFallback() = kotlinx.coroutines.test.runTest {
        val echoed = "I'm content and cozy, content and cozy, content and cozy, content and cozy|HAPPY"
        val runtime = RecordingInferenceRuntime(
            homeTalkLoadResult = RuntimeResult.Success(Unit),
            homeTalkCompleteResults = listOf(
                RuntimeResult.Success(echoed),
                RuntimeResult.Success(echoed),
            ),
        )
        val generator = createGenerator(runtime)

        val reaction = generator.generate(
            PetReactionRequest(state = PetState(), userMessage = "How are you?"),
        )

        assertEquals(2, runtime.homeTalkCompleteCalls.size)
        assertTrue(reaction.dialogue.contains("Hunger") || reaction.dialogue.contains("okay"))
    }

    @Test
    fun generateStream_errorMarker_returnsFallback() = kotlinx.coroutines.test.runTest {
        val runtime = RecordingInferenceRuntime(
            homeTalkLoadResult = RuntimeResult.Success(Unit),
            homeTalkStreamChunks = listOf(
                InferenceChunk("${LiteRtPetReactionGenerator.LITERT_ERROR_PREFIX}boom"),
                InferenceChunk("", isComplete = true),
            ),
        )
        val generator = createGenerator(runtime)

        val events = generator.generateStream(
            PetReactionRequest(state = PetState(), userMessage = "Hello!"),
        ).toList()

        val done = events.filterIsInstance<PetReactionStreamEvent.Done>().single()
        assertTrue(done.reaction.dialogue.contains("hiccuped"))
    }

    @Test
    fun generateStream_emptyStream_usesCompactRetry() = kotlinx.coroutines.test.runTest {
        val runtime = RecordingInferenceRuntime(
            homeTalkLoadResult = RuntimeResult.Success(Unit),
            homeTalkStreamChunks = listOf(InferenceChunk("", isComplete = true)),
            homeTalkCompleteResults = listOf(RuntimeResult.Success("Retried!|NEUTRAL")),
        )
        val generator = createGenerator(runtime)

        val events = generator.generateStream(
            PetReactionRequest(state = PetState(), userMessage = "Hello!"),
        ).toList()

        val done = events.filterIsInstance<PetReactionStreamEvent.Done>().single()
        assertEquals("Retried!", done.reaction.dialogue)
        assertEquals(1, runtime.homeTalkCompleteCalls.size)
    }

    @Test
    fun isInferenceFailureText_detectsKnownPrefixes() {
        val generator = createGenerator(RecordingInferenceRuntime())
        assertTrue(generator.isInferenceFailureText("__LITERT_ERROR__:oops"))
        assertTrue(generator.isInferenceFailureText("LiteRT error: oops"))
        assertTrue(generator.isInferenceFailureText("Install a LiteRT model under Manage"))
    }

    private fun createGenerator(runtime: RecordingInferenceRuntime): LiteRtPetReactionGenerator {
        val gate = object : ModelAvailabilityGate {
            override suspend fun hasUsableModel(): Boolean = true
            override suspend fun firstUsableModelId(): String = "smollm2-360m-instruct"
        }
        val prefs = object : UserPreferencesRepository {
            override val defaultModelId = flowOf("smollm2-360m-instruct")
            override val petModelId = flowOf<String?>(null)
            override val generationParams = flowOf(com.nibbli.nibbligo.core.model.GenerationParams())
            override val chatPromptMode = flowOf(com.nibbli.nibbligo.core.model.ChatPromptMode.PURE_LLM)
            override val allowDownloads = flowOf(true)
            override val preferredRuntimeKind = flowOf("litert")
            override val petPersonality = flowOf(com.nibbli.nibbligo.core.model.PetPersonality.PLAYFUL)
            override val usePetLlmReactions = flowOf(true)
            override val petCommentOnAgentWork = flowOf(true)
            override val petMoodPulseMode = flowOf(com.nibbli.nibbligo.core.model.PetMoodPulseMode.NORMAL)
            override val themeMode = flowOf(com.nibbli.nibbligo.core.model.AppThemeMode.SYSTEM)
            override val accentPalette = flowOf(com.nibbli.nibbligo.core.model.AppAccentPalette.TEAL)
            override val showDoTab = flowOf(false)
            override val litertAccelerator = flowOf(com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference.AUTO)
            override suspend fun setDefaultModelId(modelId: String?) = Unit
            override suspend fun setPetModelId(modelId: String?) = Unit
            override suspend fun setGenerationParams(params: com.nibbli.nibbligo.core.model.GenerationParams) = Unit
            override suspend fun setChatPromptMode(mode: com.nibbli.nibbligo.core.model.ChatPromptMode) = Unit
            override suspend fun setAllowDownloads(allowed: Boolean) = Unit
            override suspend fun setPreferredRuntimeKind(kind: String) = Unit
            override suspend fun setPetPersonality(personality: com.nibbli.nibbligo.core.model.PetPersonality) = Unit
            override suspend fun setUsePetLlmReactions(enabled: Boolean) = Unit
            override suspend fun setPetCommentOnAgentWork(enabled: Boolean) = Unit
            override suspend fun setPetMoodPulseMode(mode: com.nibbli.nibbligo.core.model.PetMoodPulseMode) = Unit
            override suspend fun setThemeMode(mode: com.nibbli.nibbligo.core.model.AppThemeMode) = Unit
            override suspend fun setAccentPalette(palette: com.nibbli.nibbligo.core.model.AppAccentPalette) = Unit
            override suspend fun setShowDoTab(show: Boolean) = Unit
            override val petOnboardingProfile = flowOf(com.nibbli.nibbligo.core.model.PetOnboardingProfile(completed = true))
            override val onboardingCompleted = flowOf(true)
            override val modelSetupPromptDismissed = flowOf(false)
            override suspend fun setPetOnboardingProfile(profile: com.nibbli.nibbligo.core.model.PetOnboardingProfile) = Unit
            override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) = Unit
            override suspend fun setLitertAccelerator(preference: com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference) = Unit
        }
        val petRepo = object : com.nibbli.nibbligo.core.domain.repository.PetRepository {
            override fun observePetState() = flowOf(com.nibbli.nibbligo.core.model.PetState(name = "Pixel"))
            override suspend fun getPetState() = com.nibbli.nibbligo.core.model.PetState(name = "Pixel")
            override suspend fun savePetState(state: com.nibbli.nibbligo.core.model.PetState) = Unit
        }
        val resolver = PetModelResolver(runtime, prefs, gate)
        val preloader = LiteRtModelPreloader(gate, resolver, runtime, prefs, petRepo)
        return LiteRtPetReactionGenerator(runtime, prefs, gate, resolver, preloader)
    }
}

private class RecordingInferenceRuntime(
    private val homeTalkLoadResult: RuntimeResult<Unit> = RuntimeResult.Success(Unit),
    private val petLoadResult: RuntimeResult<Unit> = RuntimeResult.Success(Unit),
    private val completeResults: List<RuntimeResult<String>> = emptyList(),
    private val homeTalkCompleteResults: List<RuntimeResult<String>> = emptyList(),
    private val streamChunks: List<InferenceChunk> = emptyList(),
    private val homeTalkStreamChunks: List<InferenceChunk> = emptyList(),
) : InferenceRuntime {
    val completeCalls = mutableListOf<PetTurnRequest>()
    val homeTalkCompleteCalls = mutableListOf<PetTurnRequest>()

    override val runtimeKind = RuntimeKind.LITERT

    override suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean) = homeTalkLoadResult

    override suspend fun ensurePetModelLoaded(modelId: String, systemInstruction: String) = petLoadResult

    override suspend fun ensureHomeTalkSession(modelId: String, systemInstruction: String) = homeTalkLoadResult

    override fun unloadModel(modelId: String) = Unit

    override fun resetChatSession(modelId: String) = Unit

    override fun resetHomeTalkSession(modelId: String) = Unit

    override fun streamChat(request: com.nibbli.nibbligo.core.model.ChatInferenceRequest): Flow<InferenceChunk> = flow {
        homeTalkStreamChunks.forEach { emit(it) }
    }

    override suspend fun complete(request: com.nibbli.nibbligo.core.model.CompletionRequest): RuntimeResult<String> =
        RuntimeResult.Unsupported

    override suspend fun completePetTurn(request: PetTurnRequest): RuntimeResult<String> {
        completeCalls.add(request)
        return completeResults.getOrElse(completeCalls.size - 1) { RuntimeResult.Success("") }
    }

    override suspend fun completeHomeTalk(request: PetTurnRequest): RuntimeResult<String> {
        homeTalkCompleteCalls.add(request)
        return homeTalkCompleteResults.getOrElse(homeTalkCompleteCalls.size - 1) { RuntimeResult.Success("") }
    }

    override fun streamHomeTalk(request: PetTurnRequest): Flow<InferenceChunk> = flow {
        homeTalkStreamChunks.forEach { emit(it) }
    }

    override fun streamPetTurn(request: PetTurnRequest): Flow<InferenceChunk> = flow {
        streamChunks.forEach { emit(it) }
    }

    override suspend fun analyzeImage(request: com.nibbli.nibbligo.core.model.VisionRequest) =
        RuntimeResult.Unsupported

    override suspend fun transcribeAudio(request: com.nibbli.nibbligo.core.model.TranscriptionRequest) =
        RuntimeResult.Unsupported

    override suspend fun runBenchmark(modelId: String) = RuntimeResult.Unsupported

    override fun capabilitiesFor(modelId: String): ModelCapabilities = ModelCapabilities(
        modelId = modelId,
        supportsChat = true,
        supportsVision = false,
        supportsAudio = false,
        supportsStreaming = true,
    )

    override suspend fun generateWithTools(request: com.nibbli.nibbligo.core.model.AgentRequest) =
        RuntimeResult.Unsupported
}

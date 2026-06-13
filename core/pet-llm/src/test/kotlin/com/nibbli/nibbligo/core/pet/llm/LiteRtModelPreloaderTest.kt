package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class LiteRtModelPreloaderTest {

    @Test
    fun preloadPrimaryModel_setsWarmingFlagDuringLoad() = runTest {
        val runtime = SlowHomeTalkRuntime()
        val preloader = createPreloader(runtime)

        val warmingDuringLoad = async {
            while (!runtime.loadStarted) {
                delay(1)
            }
            preloader.isWarmingUp.value
        }
        val preloadJob = async { preloader.preloadPrimaryModel() }

        assertTrue(warmingDuringLoad.await())
        preloadJob.await()
        assertFalse(preloader.isWarmingUp.value)
    }

    @Test
    fun preloadPrimaryModel_skipsWarmingWhenAlreadyLoaded() = runTest {
        val runtime = SlowHomeTalkRuntime()
        val preloader = createPreloader(runtime)

        preloader.preloadPrimaryModel()
        assertFalse(preloader.isWarmingUp.value)

        runtime.loadStarted = false
        preloader.preloadPrimaryModel()
        assertFalse(preloader.isWarmingUp.value)
        assertFalse(runtime.loadStarted)
    }

    private fun createPreloader(runtime: InferenceRuntime): LiteRtModelPreloader {
        val gate = object : ModelAvailabilityGate {
            override suspend fun hasUsableModel(): Boolean = true
            override suspend fun firstUsableModelId(): String = "smollm2-360m-instruct"
        }
        val prefs = object : UserPreferencesRepository {
            override val defaultModelId = flowOf("smollm2-360m-instruct")
            override val petModelId = flowOf<String?>(null)
            override val generationParams = flowOf(GenerationParams())
            override val chatPromptMode = flowOf(com.nibbli.nibbligo.core.model.ChatPromptMode.PURE_LLM)
            override val allowDownloads = flowOf(true)
            override val preferredRuntimeKind = flowOf("litert")
            override val petPersonality = flowOf(PetPersonality.PLAYFUL)
            override val usePetLlmReactions = flowOf(true)
            override val petCommentOnAgentWork = flowOf(true)
            override val petMoodPulseMode = flowOf(PetMoodPulseMode.NORMAL)
            override val themeMode = flowOf(AppThemeMode.SYSTEM)
            override val accentPalette = flowOf(com.nibbli.nibbligo.core.model.AppAccentPalette.TEAL)
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
            override suspend fun setChatPromptMode(mode: com.nibbli.nibbligo.core.model.ChatPromptMode) = Unit
            override suspend fun setAllowDownloads(allowed: Boolean) = Unit
            override suspend fun setPreferredRuntimeKind(kind: String) = Unit
            override suspend fun setPetPersonality(personality: PetPersonality) = Unit
            override suspend fun setUsePetLlmReactions(enabled: Boolean) = Unit
            override suspend fun setPetCommentOnAgentWork(enabled: Boolean) = Unit
            override suspend fun setPetMoodPulseMode(mode: PetMoodPulseMode) = Unit
            override suspend fun setThemeMode(mode: AppThemeMode) = Unit
            override suspend fun setAccentPalette(palette: com.nibbli.nibbligo.core.model.AppAccentPalette) = Unit
            override suspend fun setShowDoTab(show: Boolean) = Unit
            override suspend fun setPetOnboardingProfile(profile: PetOnboardingProfile) = Unit
            override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) = Unit
            override suspend fun setTermsAccepted(acceptedAtMillis: Long) = Unit
            override suspend fun setLitertAccelerator(preference: LiteRtAcceleratorPreference) = Unit
        }
        val resolver = PetModelResolver(runtime, prefs, gate)
        val petRepo = object : PetRepository {
            override fun observePetState() = flowOf(PetState(name = "Pixel"))
            override suspend fun getPetState() = PetState(name = "Pixel")
            override suspend fun savePetState(state: PetState) = Unit
        }
        return LiteRtModelPreloader(gate, resolver, runtime, prefs, petRepo)
    }
}

private class SlowHomeTalkRuntime : InferenceRuntime {
    @Volatile var loadStarted = false

    override val runtimeKind = RuntimeKind.LITERT

    override suspend fun ensureHomeTalkSession(modelId: String, systemInstruction: String): RuntimeResult<Unit> {
        loadStarted = true
        delay(50)
        return RuntimeResult.Success(Unit)
    }

    override suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean) =
        RuntimeResult.Success(Unit)

    override fun unloadModel(modelId: String) = Unit

    override fun streamChat(request: com.nibbli.nibbligo.core.model.ChatInferenceRequest) =
        kotlinx.coroutines.flow.emptyFlow<com.nibbli.nibbligo.core.model.InferenceChunk>()

    override suspend fun complete(request: com.nibbli.nibbligo.core.model.CompletionRequest) =
        RuntimeResult.Unsupported

    override suspend fun analyzeImage(request: com.nibbli.nibbligo.core.model.VisionRequest) =
        RuntimeResult.Unsupported

    override suspend fun transcribeAudio(request: com.nibbli.nibbligo.core.model.TranscriptionRequest) =
        RuntimeResult.Unsupported

    override suspend fun runBenchmark(modelId: String) = RuntimeResult.Unsupported

    override suspend fun generateWithTools(request: com.nibbli.nibbligo.core.model.AgentRequest) =
        RuntimeResult.Unsupported

    override fun capabilitiesFor(modelId: String): ModelCapabilities = ModelCapabilities(
        modelId = modelId,
        supportsChat = true,
        supportsVision = false,
        supportsAudio = false,
        supportsStreaming = true,
    )
}

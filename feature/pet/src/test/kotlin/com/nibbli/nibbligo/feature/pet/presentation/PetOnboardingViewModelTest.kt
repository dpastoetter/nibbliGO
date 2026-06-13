package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.AppAccentPalette
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
import com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader
import com.nibbli.nibbligo.core.pet.llm.PetModelResolver
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PetOnboardingViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun canContinue_step1_requiresPetName() {
        assertFalse(PetOnboardingUiState(stepIndex = 1, petName = "  ").canContinue)
        assertTrue(PetOnboardingUiState(stepIndex = 1, petName = "Pixel").canContinue)
    }

    @Test
    fun canContinue_step2_requiresCaretakerName() {
        assertFalse(PetOnboardingUiState(stepIndex = 2, caretakerName = "").canContinue)
        assertTrue(PetOnboardingUiState(stepIndex = 2, caretakerName = "Alex").canContinue)
    }

    @Test
    fun stepCount_isSeven() {
        assertEquals(7, PetOnboardingUiState().stepCount)
    }

    @Test
    fun canContinue_step5_requiresTermsAccepted() {
        assertFalse(PetOnboardingUiState(stepIndex = 5, termsAccepted = false).canContinue)
        assertTrue(PetOnboardingUiState(stepIndex = 5, termsAccepted = true).canContinue)
    }

    @Test
    fun complete_persistsTermsAcceptance() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val prefs = OnboardingFakePrefs(PetOnboardingProfile(), PetPersonality.PLAYFUL)
        val viewModel = createViewModel(prefs = prefs)
        advanceUntilIdle()
        viewModel.updateTermsAccepted(true)
        viewModel.complete(onFinished = {})
        advanceUntilIdle()
        assertTrue(prefs.termsAcceptedAt != null)
    }

    @Test
    fun init_preloadsSavedProfileFields() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val viewModel = createViewModel(
            profile = PetOnboardingProfile(
                caretakerName = "Alex",
                aboutYou = "Loves retro tech",
                companionGoal = "A cozy buddy",
                completed = false,
            ),
            personality = PetPersonality.CALM,
            petState = PetState(name = "Pixel"),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Pixel", state.petName)
        assertEquals("Alex", state.caretakerName)
        assertEquals("Loves retro tech", state.aboutYou)
        assertEquals("A cozy buddy", state.companionGoal)
        assertEquals(PetPersonality.CALM, state.personality)
    }

    @Test
    fun complete_persistsPetName() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        var savedPet: PetState? = null
        val petRepo = object : PetRepository {
            override fun observePetState(): Flow<PetState> = flowOf(PetState(name = "nibbli"))
            override suspend fun getPetState(): PetState = savedPet ?: PetState(name = "nibbli")
            override suspend fun savePetState(state: PetState) {
                savedPet = state
            }
        }
        val prefs = OnboardingFakePrefs(PetOnboardingProfile(), PetPersonality.PLAYFUL)
        val viewModel = createViewModel(prefs = prefs, petRepo = petRepo)
        advanceUntilIdle()
        viewModel.updatePetName("Buddy")
        viewModel.updateCaretakerName("Alex")
        viewModel.updateTermsAccepted(true)
        viewModel.complete(onFinished = {})
        advanceUntilIdle()
        assertEquals("Buddy", savedPet?.name)
    }

    private fun createViewModel(
        profile: PetOnboardingProfile = PetOnboardingProfile(),
        personality: PetPersonality = PetPersonality.PLAYFUL,
        petState: PetState = PetState(name = "nibbli"),
        prefs: OnboardingFakePrefs? = null,
        petRepo: PetRepository? = null,
    ): PetOnboardingViewModel {
        val preferences = prefs ?: OnboardingFakePrefs(profile, personality)
        val repository = petRepo ?: object : PetRepository {
            override fun observePetState(): Flow<PetState> = flowOf(petState)
            override suspend fun getPetState(): PetState = petState
            override suspend fun savePetState(state: PetState) = Unit
        }
        val gate = object : ModelAvailabilityGate {
            override suspend fun hasUsableModel(): Boolean = false
            override suspend fun firstUsableModelId(): String = "smollm2-360m-instruct"
        }
        val runtime = object : InferenceRuntime {
            override val runtimeKind = RuntimeKind.LITERT
            override suspend fun ensureHomeTalkSession(modelId: String, systemInstruction: String) =
                RuntimeResult.Success(Unit)
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
            override fun capabilitiesFor(modelId: String) = ModelCapabilities(
                modelId = modelId,
                supportsChat = true,
                supportsVision = false,
                supportsAudio = false,
                supportsStreaming = true,
            )
            override fun resetHomeTalkSession(modelId: String) = Unit
        }
        val resolver = PetModelResolver(runtime, preferences, gate)
        val preloader = LiteRtModelPreloader(gate, resolver, runtime, preferences, repository)
        val modelRepo = object : ModelRepository {
            override fun observeCatalog(): Flow<List<ModelInfo>> = flowOf(emptyList())
            override fun observeInstalled(): Flow<List<InstalledModel>> = flowOf(emptyList())
            override suspend fun getCatalog(): List<ModelInfo> = emptyList()
            override suspend fun getInstalled(): List<InstalledModel> = emptyList()
            override suspend fun isInstalled(modelId: String): Boolean = false
            override suspend fun install(modelId: String): Result<Unit> = Result.success(Unit)
            override suspend fun uninstall(modelId: String): Result<Unit> = Result.success(Unit)
            override suspend fun getInstalledModelIds(): List<String> = emptyList()
        }
        val context: Context = RuntimeEnvironment.getApplication()
        return PetOnboardingViewModel(
            context,
            preferences,
            repository,
            modelRepo,
            preloader,
            runtime,
            resolver,
            gate,
        )
    }
}

private class OnboardingFakePrefs(
    private val profile: PetOnboardingProfile,
    private val personality: PetPersonality,
) : UserPreferencesRepository {
    var termsAcceptedAt: Long? = null
    override val defaultModelId = flowOf("smollm2-360m-instruct")
    override val petModelId = flowOf<String?>(null)
    override val generationParams = flowOf(GenerationParams())
    override val chatPromptMode = flowOf(com.nibbli.nibbligo.core.model.ChatPromptMode.PURE_LLM)
    override val allowDownloads = flowOf(true)
    override val preferredRuntimeKind = flowOf("litert")
    override val petPersonality = flowOf(personality)
    override val usePetLlmReactions = flowOf(true)
    override val petCommentOnAgentWork = flowOf(true)
    override val petMoodPulseMode = flowOf(PetMoodPulseMode.NORMAL)
    override val themeMode = flowOf(AppThemeMode.SYSTEM)
    override val accentPalette = flowOf(AppAccentPalette.TEAL)
    override val showDoTab = flowOf(false)
    override val litertAccelerator = flowOf(LiteRtAcceleratorPreference.AUTO)
    override val petOnboardingProfile = flowOf(profile)
    override val onboardingCompleted = flowOf(profile.completed)
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
    override suspend fun setAccentPalette(palette: AppAccentPalette) = Unit
    override suspend fun setShowDoTab(show: Boolean) = Unit
    override suspend fun setPetOnboardingProfile(profile: PetOnboardingProfile) = Unit
    override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun setTermsAccepted(acceptedAtMillis: Long) {
        termsAcceptedAt = acceptedAtMillis
    }
    override suspend fun setLitertAccelerator(preference: LiteRtAcceleratorPreference) = Unit
}

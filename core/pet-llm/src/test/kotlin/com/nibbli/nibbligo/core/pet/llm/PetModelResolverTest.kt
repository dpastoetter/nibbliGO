package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class PetModelResolverTest {

    @Test
    fun resolve_prefersExplicitPetModel() = kotlinx.coroutines.test.runTest {
        val resolver = PetModelResolver(
            inferenceRuntime = FakeInferenceRuntime(installed = setOf("smollm2-360m-instruct", "gemma3-1b-it")),
            userPreferencesRepository = FakePrefs(petModelPref = "gemma3-1b-it", defaultModelPref = "smollm2-360m-instruct"),
            modelAvailabilityGate = FakeGate(),
        )

        assertEquals("gemma3-1b-it", resolver.resolve())
    }

    @Test
    fun resolve_prefersDefaultModelOverHardcodedList() = kotlinx.coroutines.test.runTest {
        val resolver = PetModelResolver(
            inferenceRuntime = FakeInferenceRuntime(
                installed = setOf("smollm2-360m-instruct", "gemma3-1b-it"),
            ),
            userPreferencesRepository = FakePrefs(petModelPref = null, defaultModelPref = "gemma3-1b-it"),
            modelAvailabilityGate = FakeGate(),
        )

        assertEquals("gemma3-1b-it", resolver.resolve())
    }

    @Test
    fun resolve_fallsBackToPreferenceListWhenNoDefaults() = kotlinx.coroutines.test.runTest {
        val resolver = PetModelResolver(
            inferenceRuntime = FakeInferenceRuntime(
                installed = setOf("smollm2-360m-instruct", "qwen2.5-1.5b-instruct"),
            ),
            userPreferencesRepository = FakePrefs(petModelPref = null, defaultModelPref = null),
            modelAvailabilityGate = FakeGate(firstUsable = "smollm2-360m-instruct"),
        )

        assertEquals("qwen2.5-1.5b-instruct", resolver.resolve())
    }

    @Test
    fun resolve_prefersQwenOverSmolLmWhenBothInstalled() = kotlinx.coroutines.test.runTest {
        val resolver = PetModelResolver(
            inferenceRuntime = FakeInferenceRuntime(
                installed = setOf("smollm2-360m-instruct", "gemma3-1b-it", "qwen2.5-1.5b-instruct"),
            ),
            userPreferencesRepository = FakePrefs(petModelPref = null, defaultModelPref = null),
            modelAvailabilityGate = FakeGate(firstUsable = "smollm2-360m-instruct"),
        )

        assertEquals("qwen2.5-1.5b-instruct", resolver.resolve())
    }
}

private class FakePrefs(
    petModelPref: String?,
    defaultModelPref: String?,
) : UserPreferencesRepository {
    override val defaultModelId = flowOf(defaultModelPref)
    override val petModelId = flowOf(petModelPref)
    override val generationParams = flowOf(com.nibbli.nibbligo.core.model.GenerationParams())
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
    override val petOnboardingProfile = flowOf(com.nibbli.nibbligo.core.model.PetOnboardingProfile(completed = true))
    override val onboardingCompleted = flowOf(true)
    override val modelSetupPromptDismissed = flowOf(false)
    override suspend fun setDefaultModelId(modelId: String?) = Unit
    override suspend fun setPetModelId(modelId: String?) = Unit
    override suspend fun setGenerationParams(params: com.nibbli.nibbligo.core.model.GenerationParams) = Unit
    override suspend fun setAllowDownloads(allowed: Boolean) = Unit
    override suspend fun setPreferredRuntimeKind(kind: String) = Unit
    override suspend fun setPetPersonality(personality: com.nibbli.nibbligo.core.model.PetPersonality) = Unit
    override suspend fun setUsePetLlmReactions(enabled: Boolean) = Unit
    override suspend fun setPetCommentOnAgentWork(enabled: Boolean) = Unit
    override suspend fun setPetMoodPulseMode(mode: com.nibbli.nibbligo.core.model.PetMoodPulseMode) = Unit
    override suspend fun setThemeMode(mode: com.nibbli.nibbligo.core.model.AppThemeMode) = Unit
    override suspend fun setAccentPalette(palette: com.nibbli.nibbligo.core.model.AppAccentPalette) = Unit
    override suspend fun setShowDoTab(show: Boolean) = Unit
    override suspend fun setPetOnboardingProfile(profile: com.nibbli.nibbligo.core.model.PetOnboardingProfile) = Unit
    override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun setLitertAccelerator(preference: com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference) = Unit
}

private class FakeGate(
    private val firstUsable: String? = "smollm2-360m-instruct",
) : ModelAvailabilityGate {
    override suspend fun hasUsableModel(): Boolean = firstUsable != null

    override suspend fun firstUsableModelId(): String? = firstUsable
}

internal class FakeInferenceRuntime(
    private val installed: Set<String>,
) : InferenceRuntime {
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

    override fun capabilitiesFor(modelId: String): ModelCapabilities = ModelCapabilities(
        modelId = modelId,
        supportsChat = modelId in installed,
        supportsVision = false,
        supportsAudio = false,
        supportsStreaming = modelId in installed,
    )

    override suspend fun generateWithTools(request: com.nibbli.nibbligo.core.model.AgentRequest) =
        com.nibbli.nibbligo.core.model.RuntimeResult.Unsupported
}

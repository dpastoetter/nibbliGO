package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtPetReactionGenerator @Inject constructor(
    private val inferenceRuntime: InferenceRuntime,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelAvailabilityGate: ModelAvailabilityGate,
) : PetReactionPort {

    override suspend fun generate(request: PetReactionRequest): PetReaction {
        if (!modelAvailabilityGate.hasUsableModel()) {
            return installRequiredReaction()
        }
        val modelId = resolvePetModelId()
        val personality = userPreferencesRepository.petPersonality.first()
        val enriched = request.copy(personality = personality)
        val prompt = PetPromptBuilder.build(enriched)
        when (val load = inferenceRuntime.ensureModelLoaded(modelId, includeTools = false)) {
            is RuntimeResult.Success -> Unit
            is RuntimeResult.Error -> return PetReaction(
                dialogue = "Model load failed: ${load.message}",
                suggestedExpression = null,
            )
            RuntimeResult.LowMemory -> return PetReaction(
                dialogue = "Not enough memory to load the model right now.",
                suggestedExpression = null,
            )
            RuntimeResult.Unsupported -> return PetReaction(
                dialogue = "This model isn't supported on this device.",
                suggestedExpression = null,
            )
        }
        return when (
            val result = inferenceRuntime.complete(
                CompletionRequest(modelId = modelId, prompt = prompt, includeTools = false),
            )
        ) {
            is RuntimeResult.Success -> {
                if (result.data.isBlank()) {
                    PetReactionParser.fallback(enriched)
                } else {
                    PetReactionParser.parse(result.data)
                }
            }
            is RuntimeResult.Error -> PetReaction(
                dialogue = "I couldn't think of a reply: ${result.message}",
                suggestedExpression = null,
            )
            RuntimeResult.LowMemory -> PetReaction(
                dialogue = "I'm too low on memory to reply right now.",
                suggestedExpression = null,
            )
            RuntimeResult.Unsupported -> PetReaction(
                dialogue = "Talk isn't supported with this model.",
                suggestedExpression = null,
            )
        }
    }

    private suspend fun resolvePetModelId(): String {
        val default = userPreferencesRepository.defaultModelId.first()
        if (default != null && inferenceRuntime.capabilitiesFor(default).supportsChat) {
            return default
        }
        for (modelId in PET_MODEL_PREFERENCE) {
            if (inferenceRuntime.capabilitiesFor(modelId).supportsChat) return modelId
        }
        return modelAvailabilityGate.firstUsableModelId() ?: DEFAULT_MODEL
    }

    private fun installRequiredReaction() = PetReaction(
        dialogue = "Install a model under Manage → Models so I can talk to you!",
        suggestedExpression = null,
    )

    companion object {
        private const val DEFAULT_MODEL = "functiongemma-270m"
        private val PET_MODEL_PREFERENCE = listOf("gemma-4-e2b-it", "functiongemma-270m")
    }
}

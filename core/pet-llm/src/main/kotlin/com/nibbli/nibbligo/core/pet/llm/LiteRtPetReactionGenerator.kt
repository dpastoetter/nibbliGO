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
        val modelId = userPreferencesRepository.defaultModelId.first()
            ?: modelAvailabilityGate.firstUsableModelId()
            ?: DEFAULT_MODEL
        val personality = userPreferencesRepository.petPersonality.first()
        val enriched = request.copy(personality = personality)
        val prompt = PetPromptBuilder.build(enriched)
        when (inferenceRuntime.ensureModelLoaded(modelId)) {
            is RuntimeResult.Success -> Unit
            is RuntimeResult.Error,
            RuntimeResult.LowMemory,
            RuntimeResult.Unsupported,
            -> return installRequiredReaction()
        }
        return when (
            val result = inferenceRuntime.complete(
                CompletionRequest(modelId = modelId, prompt = prompt),
            )
        ) {
            is RuntimeResult.Success -> PetReactionParser.parse(result.data)
            is RuntimeResult.Error,
            RuntimeResult.LowMemory,
            RuntimeResult.Unsupported,
            -> PetReaction(
                dialogue = "I couldn't think of a reply right now. Try again in a moment.",
                suggestedExpression = null,
            )
        }
    }

    private fun installRequiredReaction() = PetReaction(
        dialogue = "Install a model under Manage → Models so I can talk to you!",
        suggestedExpression = null,
    )

    companion object {
        private const val DEFAULT_MODEL = "functiongemma-270m"
    }
}

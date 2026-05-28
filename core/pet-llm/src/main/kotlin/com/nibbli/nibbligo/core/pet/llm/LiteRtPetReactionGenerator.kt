package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtPetReactionGenerator @Inject constructor(
    private val inferenceRuntime: InferenceRuntime,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val templatePetReactionGenerator: TemplatePetReactionGenerator,
) : PetReactionPort {

    override suspend fun generate(request: PetReactionRequest): PetReaction {
        val modelId = userPreferencesRepository.defaultModelId.first() ?: DEFAULT_MODEL
        val prompt = PetPromptBuilder.build(request)
        when (inferenceRuntime.ensureModelLoaded(modelId)) {
            is RuntimeResult.Success -> Unit
            is RuntimeResult.Error,
            RuntimeResult.LowMemory,
            RuntimeResult.Unsupported,
            -> return templatePetReactionGenerator.generate(request)
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
            -> templatePetReactionGenerator.generate(request)
        }
    }

    companion object {
        private const val DEFAULT_MODEL = "nibbli-fast"
    }
}

package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetModelResolver @Inject constructor(
    private val inferenceRuntime: InferenceRuntime,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelAvailabilityGate: ModelAvailabilityGate,
) {
    suspend fun resolve(): String {
        val preferred = userPreferencesRepository.petModelId.first()
        if (preferred != null && inferenceRuntime.capabilitiesFor(preferred).supportsChat) {
            return preferred
        }
        val default = userPreferencesRepository.defaultModelId.first()
        if (default != null && inferenceRuntime.capabilitiesFor(default).supportsChat) {
            return default
        }
        for (modelId in PET_MODEL_PREFERENCE) {
            if (inferenceRuntime.capabilitiesFor(modelId).supportsChat) return modelId
        }
        return modelAvailabilityGate.firstUsableModelId() ?: DEFAULT_MODEL
    }

    companion object {
        internal val DEFAULT_MODEL = ModelCatalog.RECOMMENDED_PET_MODEL_ID
        internal val PET_MODEL_PREFERENCE = listOf(
            "qwen2.5-1.5b-instruct",
            "smollm2-360m-instruct",
            "gemma3-1b-it",
            "deepseek-r1-distill-qwen-1.5b",
            "gemma-4-e2b-it",
            "functiongemma-270m",
        )
    }
}

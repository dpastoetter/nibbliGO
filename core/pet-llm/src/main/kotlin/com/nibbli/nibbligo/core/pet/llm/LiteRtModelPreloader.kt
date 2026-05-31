package com.nibbli.nibbligo.core.pet.llm

import android.util.Log
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import kotlinx.coroutines.flow.first
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Eagerly loads the primary on-device model at app startup (Edge Gallery pattern:
 * [engine.initialize] completes before the first user message).
 */
@Singleton
class LiteRtModelPreloader @Inject constructor(
    private val modelAvailabilityGate: ModelAvailabilityGate,
    private val petModelResolver: PetModelResolver,
    private val inferenceRuntime: InferenceRuntime,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val petRepository: PetRepository,
) {
    private val mutex = Mutex()
    private var lastPreloadedModelId: String? = null
    private val _isWarmingUp = MutableStateFlow(false)
    val isWarmingUp: StateFlow<Boolean> = _isWarmingUp.asStateFlow()

    suspend fun preloadPrimaryModel(force: Boolean = false) {
        if (!modelAvailabilityGate.hasUsableModel()) return
        val modelId = petModelResolver.resolve()
        val needsLoad = mutex.withLock {
            force || lastPreloadedModelId != modelId
        }
        if (!needsLoad) return

        _isWarmingUp.value = true
        try {
            mutex.withLock {
                if (!force && lastPreloadedModelId == modelId) return@withLock
                val profile = userPreferencesRepository.petOnboardingProfile.first()
                val petName = petRepository.getPetState().name
                val onboardingContext = PetOnboardingPrompt.formatForSystemInstruction(profile, petName)
                val systemInstruction = PetPromptBuilder.homeTalkSystemInstruction(onboardingContext, petName)
                val start = System.nanoTime()
                when (val result = inferenceRuntime.ensureHomeTalkSession(modelId, systemInstruction)) {
                    is RuntimeResult.Success -> {
                        lastPreloadedModelId = modelId
                        val ms = (System.nanoTime() - start) / 1_000_000
                        Log.i(TAG, "Preloaded home talk model $modelId in ${ms}ms")
                    }
                    is RuntimeResult.Error ->
                        Log.w(TAG, "Preload failed for $modelId: ${result.message}")
                    RuntimeResult.LowMemory ->
                        Log.w(TAG, "Preload failed for $modelId: low memory")
                    RuntimeResult.Unsupported ->
                        Log.w(TAG, "Preload skipped for $modelId: unsupported runtime")
                }
            }
        } finally {
            _isWarmingUp.value = false
        }
    }

    fun invalidate() {
        lastPreloadedModelId = null
    }

    companion object {
        private const val TAG = "LiteRtModelPreloader"
    }
}

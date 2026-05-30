package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.PetTurnRequest
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        val enriched = enrichRequest(request)

        val parts = PetPromptBuilder.buildParts(enriched, modelId)
        val primaryText = completePetTurn(modelId, parts)
        if (!primaryText.isNullOrBlank()) {
            return PetReactionParser.parse(primaryText)
        }

        if (enriched.userMessage != null) {
            val compact = PetPromptBuilder.buildCompactTalkParts(enriched, modelId)
            val retryText = completePetTurn(modelId, compact)
            if (!retryText.isNullOrBlank()) {
                return PetReactionParser.parse(retryText)
            }
        }

        return PetReactionParser.fallback(enriched)
    }

    override fun generateStream(request: PetReactionRequest): Flow<PetReactionStreamEvent> = flow {
        if (!modelAvailabilityGate.hasUsableModel()) {
            emit(PetReactionStreamEvent.Done(installRequiredReaction()))
            return@flow
        }
        val modelId = resolvePetModelId()
        val enriched = enrichRequest(request)

        val parts = PetPromptBuilder.buildParts(enriched, modelId)

        if (enriched.userMessage != null) {
            val fastText = completePetTurn(modelId, parts)
            if (!fastText.isNullOrBlank()) {
                val reaction = PetReactionParser.parse(fastText)
                emit(PetReactionStreamEvent.Token(reaction.dialogue))
                emit(PetReactionStreamEvent.Done(reaction))
                return@flow
            }
        }

        val builder = StringBuilder()
        try {
            inferenceRuntime.streamPetTurn(
                PetTurnRequest(
                    modelId = modelId,
                    systemInstruction = parts.systemInstruction,
                    userMessage = parts.userMessage,
                ),
            ).collect { chunk ->
                if (chunk.isComplete) {
                    if (chunk.token.isNotEmpty()) {
                        builder.append(chunk.token)
                    }
                    val raw = builder.toString().trim()
                    val reaction = if (raw.isNotBlank()) {
                        PetReactionParser.parse(raw)
                    } else if (enriched.userMessage != null) {
                        val compact = PetPromptBuilder.buildCompactTalkParts(enriched, modelId)
                        val retry = completePetTurn(modelId, compact)
                        if (!retry.isNullOrBlank()) {
                            PetReactionParser.parse(retry)
                        } else {
                            PetReactionParser.fallback(enriched)
                        }
                    } else {
                        PetReactionParser.fallback(enriched)
                    }
                    emit(PetReactionStreamEvent.Done(reaction))
                } else if (chunk.token.isNotEmpty()) {
                    builder.append(chunk.token)
                    emit(PetReactionStreamEvent.Token(PetReactionParser.stripForStreaming(builder.toString())))
                }
            }
        } catch (e: Exception) {
            emit(PetReactionStreamEvent.Done(PetReactionParser.fallback(enriched)))
        }
    }

    override suspend fun warmLoad() {
        if (!modelAvailabilityGate.hasUsableModel()) return
        val modelId = resolvePetModelId()
        val staticSystem = PetPromptBuilder.buildStaticSystemInstruction(modelId)
        when (inferenceRuntime.ensurePetModelLoaded(modelId, staticSystem)) {
            is RuntimeResult.Error,
            RuntimeResult.LowMemory,
            RuntimeResult.Unsupported,
            -> Unit
            is RuntimeResult.Success -> Unit
        }
    }

    private suspend fun enrichRequest(request: PetReactionRequest): PetReactionRequest {
        val personality = userPreferencesRepository.petPersonality.first()
        return request.copy(personality = personality)
    }

    private suspend fun completePetTurn(modelId: String, parts: PetPromptParts): String? {
        return try {
            when (
                val result = inferenceRuntime.completePetTurn(
                    PetTurnRequest(
                        modelId = modelId,
                        systemInstruction = parts.systemInstruction,
                        userMessage = parts.userMessage,
                    ),
                )
            ) {
                is RuntimeResult.Success -> result.data.trim().takeIf { it.isNotEmpty() }
                is RuntimeResult.Error -> null
                RuntimeResult.LowMemory -> null
                RuntimeResult.Unsupported -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    internal suspend fun resolvePetModelId(): String {
        val preferred = userPreferencesRepository.petModelId.first()
        if (preferred != null && inferenceRuntime.capabilitiesFor(preferred).supportsChat) {
            return preferred
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
        private const val DEFAULT_MODEL = "smollm2-360m-instruct"
        private val PET_MODEL_PREFERENCE = listOf(
            "smollm2-360m-instruct",
            "gemma3-1b-it",
            "qwen2.5-1.5b-instruct",
            "deepseek-r1-distill-qwen-1.5b",
            "gemma-4-e2b-it",
            "functiongemma-270m",
        )
    }
}

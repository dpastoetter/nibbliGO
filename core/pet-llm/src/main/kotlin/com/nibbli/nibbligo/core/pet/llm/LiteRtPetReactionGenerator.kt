package com.nibbli.nibbligo.core.pet.llm

import android.util.Log
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.PetTurnRequest
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtPetReactionGenerator @Inject constructor(
    private val inferenceRuntime: InferenceRuntime,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelAvailabilityGate: ModelAvailabilityGate,
    private val petModelResolver: PetModelResolver,
    private val liteRtModelPreloader: LiteRtModelPreloader,
) : PetReactionPort {

    private var homeTalkTurnCount = 0

    override suspend fun generate(request: PetReactionRequest): PetReaction {
        if (!modelAvailabilityGate.hasUsableModel()) {
            return installRequiredReaction()
        }
        val modelId = petModelResolver.resolve()
        val enriched = enrichRequest(request)

        enriched.userMessage?.let { msg ->
            PetGameFaqMatcher.confidentInstantAnswer(msg)?.let { answer ->
                return PetReactionParser.parseTalk("$answer|NEUTRAL")
            }
            return generateUserTalk(enriched, modelId)
        }

        val onboardingContext = resolveOnboardingContext()
        val parts = PetPromptBuilder.buildParts(enriched, modelId, onboardingContext)
        if (!ensurePetModelReady(modelId, parts.systemInstruction)) {
            return PetReactionParser.fallback(enriched)
        }

        parseModelOutput(completePetTurn(modelId, parts), enriched)?.let { return it }

        Log.w(TAG, "Pet reaction empty; using fallback (non-streaming)")
        return PetReactionParser.fallback(enriched)
    }

    override fun generateStream(request: PetReactionRequest): Flow<PetReactionStreamEvent> = flow {
        if (!modelAvailabilityGate.hasUsableModel()) {
            emit(PetReactionStreamEvent.Done(installRequiredReaction()))
            return@flow
        }
        val modelId = petModelResolver.resolve()
        val enriched = enrichRequest(request)

        enriched.userMessage?.let { msg ->
            PetGameFaqMatcher.confidentInstantAnswer(msg)?.let { answer ->
                emit(PetReactionStreamEvent.Done(PetReactionParser.parseTalk("$answer|NEUTRAL")))
                return@flow
            }
            emitAll(streamUserTalkViaHomeSession(enriched, modelId))
            return@flow
        }

        val onboardingContext = resolveOnboardingContext()
        val parts = PetPromptBuilder.buildParts(enriched, modelId, onboardingContext)

        if (!ensurePetModelReady(modelId, parts.systemInstruction)) {
            emit(PetReactionStreamEvent.Done(PetReactionParser.fallback(enriched)))
            return@flow
        }

        val builder = StringBuilder()
        var streamFailed = false
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
                } else if (chunk.token.isNotEmpty()) {
                    if (isInferenceFailureText(chunk.token)) {
                        streamFailed = true
                        return@collect
                    }
                    builder.append(chunk.token)
                    emit(
                        PetReactionStreamEvent.Token(
                            PetReactionParser.stripForStreaming(builder.toString()),
                        ),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Pet stream failed; using fallback", e)
            emit(PetReactionStreamEvent.Done(PetReactionParser.fallback(enriched)))
            return@flow
        }

        val raw = builder.toString().trim()
        if (!streamFailed) {
            parseModelOutput(raw, enriched)?.let {
                emit(PetReactionStreamEvent.Done(it))
                return@flow
            }
        }

        Log.w(TAG, "Pet stream empty or failed; using fallback")
        emit(PetReactionStreamEvent.Done(PetReactionParser.fallback(enriched)))
    }

    override suspend fun warmLoad() {
        liteRtModelPreloader.preloadPrimaryModel()
    }

    override suspend fun activeModelDisplayName(): String {
        if (!modelAvailabilityGate.hasUsableModel()) return "No model"
        val modelId = petModelResolver.resolve()
        return ModelCatalog.find(modelId)?.displayName ?: modelId
    }

    private suspend fun generateUserTalk(request: PetReactionRequest, modelId: String): PetReaction {
        val onboardingContext = resolveOnboardingContext()
        val parts = PetPromptBuilder.buildHomeTalkParts(request, modelId, onboardingContext)
        if (!ensureHomeTalkSession(modelId)) {
            return PetReactionParser.fallback(request)
        }
        maybeResetHomeTalkSession(modelId)

        val raw = completeHomeTalkTurn(modelId, parts)
        parseModelOutput(raw, request)?.let { reaction ->
            if (PetReactionParser.hasPromptEcho(raw.orEmpty())) {
                inferenceRuntime.resetHomeTalkSession(modelId)
                homeTalkTurnCount = 0
            }
            return reaction
        }

        val compactParts = PetPromptBuilder.buildCompactHomeTalkParts(request, onboardingContext)
        val compactRaw = completeHomeTalkTurn(modelId, compactParts)
        parseModelOutput(compactRaw, request)?.let { return it }

        Log.w(TAG, "User talk empty; using fallback")
        return PetReactionParser.fallback(request)
    }

    private fun streamUserTalkViaHomeSession(
        request: PetReactionRequest,
        modelId: String,
    ): Flow<PetReactionStreamEvent> = flow {
        val onboardingContext = resolveOnboardingContext()
        val parts = PetPromptBuilder.buildHomeTalkParts(request, modelId, onboardingContext)
        if (!ensureHomeTalkSession(modelId)) {
            emit(PetReactionStreamEvent.Done(PetReactionParser.fallback(request)))
            return@flow
        }
        maybeResetHomeTalkSession(modelId)

        val builder = StringBuilder()
        var streamFailed = false
        try {
            inferenceRuntime.streamHomeTalk(homeTalkRequest(modelId, parts)).collect { chunk ->
                if (chunk.isComplete) {
                    if (chunk.token.isNotEmpty()) {
                        builder.append(chunk.token)
                    }
                } else if (chunk.token.isNotEmpty()) {
                    if (isInferenceFailureText(chunk.token)) {
                        streamFailed = true
                        return@collect
                    }
                    builder.append(chunk.token)
                    emit(
                        PetReactionStreamEvent.Token(
                            PetReactionParser.stripForStreaming(builder.toString()),
                        ),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "User talk stream failed; using fallback", e)
            emit(PetReactionStreamEvent.Done(PetReactionParser.fallback(request)))
            return@flow
        }

        val raw = builder.toString().trim()
        if (!streamFailed) {
            parseModelOutput(raw, request)?.let { reaction ->
                if (PetReactionParser.hasPromptEcho(raw)) {
                    inferenceRuntime.resetHomeTalkSession(modelId)
                    homeTalkTurnCount = 0
                }
                emit(PetReactionStreamEvent.Done(reaction))
                return@flow
            }
        }

        if (streamFailed || raw.isBlank()) {
            val compactParts = PetPromptBuilder.buildCompactHomeTalkParts(request, onboardingContext)
            if (ensureHomeTalkSession(modelId)) {
                parseModelOutput(completeHomeTalkTurn(modelId, compactParts), request)?.let {
                    emit(PetReactionStreamEvent.Done(it))
                    return@flow
                }
            }
        }

        Log.w(TAG, "User talk stream empty or failed; using fallback")
        emit(PetReactionStreamEvent.Done(PetReactionParser.fallback(request)))
    }

    private suspend fun enrichRequest(request: PetReactionRequest): PetReactionRequest {
        val personality = userPreferencesRepository.petPersonality.first()
        return request.copy(personality = personality)
    }

    private suspend fun resolveOnboardingContext(): String? =
        PetOnboardingPrompt.formatForSystemInstruction(
            userPreferencesRepository.petOnboardingProfile.first(),
        )

    private suspend fun ensureHomeTalkSession(modelId: String): Boolean {
        val systemInstruction = PetPromptBuilder.homeTalkSystemInstruction(resolveOnboardingContext())
        return when (val result = inferenceRuntime.ensureHomeTalkSession(modelId, systemInstruction)) {
            is RuntimeResult.Success -> true
            is RuntimeResult.Error -> {
                Log.w(TAG, "Home talk session load failed: ${result.message}")
                false
            }
            RuntimeResult.LowMemory -> {
                Log.w(TAG, "Home talk session load failed: low memory")
                false
            }
            RuntimeResult.Unsupported -> {
                Log.w(TAG, "Home talk session load failed: unsupported")
                false
            }
        }
    }

    private fun maybeResetHomeTalkSession(modelId: String) {
        if (homeTalkTurnCount >= HOME_TALK_RESET_EVERY) {
            inferenceRuntime.resetHomeTalkSession(modelId)
            homeTalkTurnCount = 0
        }
        homeTalkTurnCount++
    }

    private suspend fun ensurePetModelReady(modelId: String, systemInstruction: String): Boolean {
        return when (val result = inferenceRuntime.ensurePetModelLoaded(modelId, systemInstruction)) {
            is RuntimeResult.Success -> true
            is RuntimeResult.Error -> {
                Log.w(TAG, "Pet model load failed: ${result.message}")
                false
            }
            RuntimeResult.LowMemory -> {
                Log.w(TAG, "Pet model load failed: low memory")
                false
            }
            RuntimeResult.Unsupported -> {
                Log.w(TAG, "Pet model load failed: unsupported")
                false
            }
        }
    }

    private suspend fun completeHomeTalkTurn(modelId: String, parts: PetPromptParts): String? {
        return try {
            when (
                val result = inferenceRuntime.completeHomeTalk(homeTalkRequest(modelId, parts))
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

    private fun homeTalkRequest(modelId: String, parts: PetPromptParts): PetTurnRequest =
        PetTurnRequest(
            modelId = modelId,
            systemInstruction = parts.systemInstruction,
            userMessage = parts.userMessage,
        )

    private fun parseModelOutput(raw: String?, request: PetReactionRequest): PetReaction? {
        if (raw.isNullOrBlank() || isInferenceFailureText(raw)) return null
        if (request.userMessage != null) {
            val (dialogue, _) = PetReactionParser.splitDialogueAndExpression(
                PetReactionParser.sanitizeModelEcho(raw),
            )
            if (PetReactionParser.hasDegenerateRepetition(dialogue)) {
                Log.w(TAG, "User talk degenerate repetition; treating as empty")
                return null
            }
            return PetReactionParser.parseTalk(raw)
        }
        return PetReactionParser.parse(raw)
    }

    internal fun isInferenceFailureText(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.startsWith(LITERT_ERROR_PREFIX) ||
            trimmed.startsWith("LiteRT error:") ||
            trimmed.startsWith("Install a LiteRT model")
    }

    private fun installRequiredReaction() = PetReaction(
        dialogue = "Install a model under Manage → Models so I can talk to you!",
        suggestedExpression = null,
    )

    companion object {
        private const val TAG = "LiteRtPetReaction"
        const val LITERT_ERROR_PREFIX = "__LITERT_ERROR__:"
        private const val HOME_TALK_RESET_EVERY = 5
    }
}

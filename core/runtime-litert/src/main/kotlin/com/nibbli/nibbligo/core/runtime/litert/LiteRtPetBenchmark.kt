package com.nibbli.nibbligo.core.runtime.litert

import com.nibbli.nibbligo.core.litert.engine.LiteRtBackendResolver
import com.nibbli.nibbligo.core.litert.engine.LiteRtEnginePool
import com.nibbli.nibbligo.core.model.PetBenchmarkMetrics
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.pet.llm.PetPromptBuilder
import com.nibbli.nibbligo.core.pet.llm.PetReactionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtPetBenchmark @Inject constructor(
    private val enginePool: LiteRtEnginePool,
) {
    suspend fun run(modelId: String): PetBenchmarkMetrics {
        val raw = enginePool.benchmarkStreamTurn(
            modelId = modelId,
            userText = LiteRtEnginePool.BENCHMARK_RAW_PROMPT,
            profile = LiteRtEnginePool.SESSION_PROFILE_DEFAULT,
        )

        val talkParts = PetPromptBuilder.buildStreamTalkParts(
            PetReactionRequest(state = PetState(), userMessage = "How are you?"),
            modelId,
        )
        val pet = enginePool.benchmarkStreamTurn(
            modelId = modelId,
            userText = talkParts.userMessage,
            systemInstruction = talkParts.systemInstruction,
            profile = LiteRtEnginePool.SESSION_PROFILE_PET_CHAT,
        )
        val refreshMs = enginePool.measurePetRefresh(modelId)

        val homeTalkSystem = PetPromptBuilder.homeTalkSystemInstruction()
        enginePool.ensureSession(
            modelId = modelId,
            tools = emptyList(),
            systemInstruction = homeTalkSystem,
            profile = LiteRtEnginePool.SESSION_PROFILE_HOME_TALK,
        )
        val homeTalkFast = enginePool.benchmarkStreamTurn(
            modelId = modelId,
            userText = LiteRtEnginePool.BENCHMARK_HOME_TALK_FAST_PROMPT,
            systemInstruction = homeTalkSystem,
            profile = LiteRtEnginePool.SESSION_PROFILE_HOME_TALK,
        )

        val combinedUser = buildString {
            appendLine(homeTalkSystem)
            appendLine()
            append(LiteRtEnginePool.BENCHMARK_HOME_TALK_FAST_PROMPT)
        }.trim()
        val homeTalkCombined = enginePool.benchmarkStreamTurn(
            modelId = modelId,
            userText = combinedUser,
            profile = LiteRtEnginePool.SESSION_PROFILE_DEFAULT,
        )

        val backend = enginePool.activeBackendFor(modelId) ?: "unknown"
        val emulatorNote = if (LiteRtBackendResolver.isProbablyEmulator()) {
            "Emulator (CPU)"
        } else {
            "Device ($backend)"
        }

        return PetBenchmarkMetrics(
            rawTimeToFirstTokenMs = raw.timeToFirstTokenMs,
            rawTokensPerSecond = raw.tokensPerSecond,
            petPathTimeToFirstTokenMs = pet.timeToFirstTokenMs,
            petPathTokensPerSecond = pet.tokensPerSecond,
            refreshMs = refreshMs,
            backendName = backend,
            thermalNote = emulatorNote,
            homeTalkFastTierTimeToFirstTokenMs = homeTalkFast.timeToFirstTokenMs,
            homeTalkCombinedPromptTimeToFirstTokenMs = homeTalkCombined.timeToFirstTokenMs,
        )
    }
}

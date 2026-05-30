package com.nibbli.nibbligo.core.runtime

import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.PetBenchmarkMetrics
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.PetTurnRequest
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface InferenceRuntime {
    val runtimeKind: RuntimeKind

    suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean = true): RuntimeResult<Unit>

    suspend fun ensurePetModelLoaded(
        modelId: String,
        systemInstruction: String,
    ): RuntimeResult<Unit> = RuntimeResult.Unsupported

    suspend fun ensureHomeTalkSession(
        modelId: String,
        systemInstruction: String,
    ): RuntimeResult<Unit> = RuntimeResult.Unsupported

    suspend fun ensureAgentModelLoaded(modelId: String): RuntimeResult<Unit> = RuntimeResult.Unsupported

    fun unloadModel(modelId: String)

    /** Clears in-memory chat turns so the next message starts fresh (Local Chat). */
    fun resetChatSession(modelId: String) {}

    /** Clears Home talk turns while keeping cached system instruction. */
    fun resetHomeTalkSession(modelId: String) {}

    fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk>

    suspend fun complete(request: CompletionRequest): RuntimeResult<String>

    suspend fun completePetTurn(request: PetTurnRequest): RuntimeResult<String> =
        complete(
            CompletionRequest(
                modelId = request.modelId,
                prompt = "${request.systemInstruction}\n\n${request.userMessage}",
                includeTools = false,
            ),
        )

    fun streamPetTurn(request: PetTurnRequest): Flow<InferenceChunk> = flow {
        when (val result = completePetTurn(request)) {
            is RuntimeResult.Success -> {
                if (result.data.isNotEmpty()) {
                    emit(InferenceChunk(result.data))
                }
                emit(InferenceChunk("", isComplete = true))
            }
            is RuntimeResult.Error -> emit(InferenceChunk("Error: ${result.message}", isComplete = true))
            RuntimeResult.LowMemory -> emit(InferenceChunk("Not enough memory.", isComplete = true))
            RuntimeResult.Unsupported -> emit(InferenceChunk("Unsupported.", isComplete = true))
        }
    }

    fun streamHomeTalk(request: PetTurnRequest): Flow<InferenceChunk> = streamPetTurn(request)

    suspend fun completeHomeTalk(request: PetTurnRequest): RuntimeResult<String> = completePetTurn(request)

    suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String>

    suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String>

    suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics>

    suspend fun runPetBenchmark(modelId: String): RuntimeResult<PetBenchmarkMetrics> =
        RuntimeResult.Unsupported

    fun capabilitiesFor(modelId: String): ModelCapabilities

    suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn>
}

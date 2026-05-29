package com.nibbli.nibbligo.core.runtime

import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import kotlinx.coroutines.flow.Flow

interface InferenceRuntime {
    val runtimeKind: RuntimeKind

    suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean = true): RuntimeResult<Unit>

    fun unloadModel(modelId: String)

    fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk>

    suspend fun complete(request: CompletionRequest): RuntimeResult<String>

    suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String>

    suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String>

    suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics>

    fun capabilitiesFor(modelId: String): ModelCapabilities

    suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn>
}

package com.nibbli.nibbligo.di

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
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import com.nibbli.nibbligo.core.runtime.RuntimePreference
import com.nibbli.nibbligo.core.runtime.fake.FakeInferenceRuntime
import com.nibbli.nibbligo.core.runtime.litert.LiteRTInferenceRuntime
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwitchingInferenceRuntime @Inject constructor(
    private val fake: FakeInferenceRuntime,
    private val liteRt: LiteRTInferenceRuntime,
    private val preference: RuntimePreference,
) : InferenceRuntime {

    private val active: InferenceRuntime
        get() = if (
            preference.preferredKind() == RuntimeKind.LITERT &&
            preference.isLiteRtModelPresent()
        ) {
            liteRt
        } else {
            fake
        }

    override val runtimeKind: RuntimeKind get() = active.runtimeKind
    override suspend fun ensureModelLoaded(modelId: String) = active.ensureModelLoaded(modelId)
    override fun unloadModel(modelId: String) = active.unloadModel(modelId)
    override fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk> = active.streamChat(request)
    override suspend fun complete(request: CompletionRequest): RuntimeResult<String> = active.complete(request)
    override suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String> = active.analyzeImage(request)
    override suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String> =
        active.transcribeAudio(request)
    override suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics> = active.runBenchmark(modelId)
    override fun capabilitiesFor(modelId: String): ModelCapabilities = active.capabilitiesFor(modelId)
    override suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn> =
        active.generateWithTools(request)
}

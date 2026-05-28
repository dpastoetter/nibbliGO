package com.nibbli.nibbligo.core.runtime.litert

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LiteRT-LM bridge: when a `.litertlm` model exists under filesDir/models,
 * this runtime is selected via [RuntimePreference]. Until full SDK wiring,
 * delegates to [FakeInferenceRuntime] with LITERT kind and enhanced capabilities.
 *
 * Replace inner calls with `com.google.ai.edge.litertlm` Engine APIs when models are present.
 */
@Singleton
class LiteRTInferenceRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fakeInferenceRuntime: FakeInferenceRuntime,
) : InferenceRuntime {

    override val runtimeKind: RuntimeKind = RuntimeKind.LITERT

    private fun hasLiteRtFile(modelId: String): Boolean {
        val dir = File(context.filesDir, "models")
        return dir.listFiles()?.any { f ->
            f.name.contains(modelId.replace("-", ""), ignoreCase = true) &&
                (f.extension == "litertlm" || f.name.endsWith(".litertlm"))
        } == true
    }

    override suspend fun ensureModelLoaded(modelId: String): RuntimeResult<Unit> {
        if (!hasLiteRtFile(modelId) && modelId.startsWith("gemma")) {
            return RuntimeResult.Error(
                "Download ${modelId}.litertlm to files/models/ to use LiteRT runtime.",
            )
        }
        return fakeInferenceRuntime.ensureModelLoaded(modelId)
    }

    override fun unloadModel(modelId: String) = fakeInferenceRuntime.unloadModel(modelId)
    override fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk> =
        fakeInferenceRuntime.streamChat(request)
    override suspend fun complete(request: CompletionRequest): RuntimeResult<String> =
        fakeInferenceRuntime.complete(request)
    override suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String> =
        fakeInferenceRuntime.analyzeImage(request)
    override suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String> =
        fakeInferenceRuntime.transcribeAudio(request)
    override suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics> =
        fakeInferenceRuntime.runBenchmark(modelId)

    override fun capabilitiesFor(modelId: String): ModelCapabilities {
        val base = fakeInferenceRuntime.capabilitiesFor(modelId)
        return base.copy(
            supportsToolCalling = modelId == "gemma-4-e2b-it" || modelId == "functiongemma-270m" || base.supportsToolCalling,
            supportsThinkingTrace = modelId == "gemma-4-e2b-it",
        )
    }

    override suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn> =
        fakeInferenceRuntime.generateWithTools(request)
}

package com.nibbli.nibbligo.core.runtime.litert

import com.nibbli.nibbligo.core.litert.engine.LiteRtEnginePool
import com.nibbli.nibbligo.core.litert.engine.StreamEvent
import com.nibbli.nibbligo.core.model.AgentMessageRole
import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRTInferenceRuntime @Inject constructor(
    private val enginePool: LiteRtEnginePool,
) : InferenceRuntime {

    override val runtimeKind: RuntimeKind = RuntimeKind.LITERT

    private fun useLiteRt(modelId: String): Boolean = enginePool.hasModel(modelId)

    private fun modelNotInstalled(modelId: String): RuntimeResult.Error =
        RuntimeResult.Error("Model $modelId is not installed. Download it under Manage → Models.")

    private fun mobileTools(modelId: String) = toolProvidersForModel(modelId) { }

    override suspend fun ensureModelLoaded(modelId: String): RuntimeResult<Unit> {
        if (!useLiteRt(modelId)) return modelNotInstalled(modelId)
        return try {
            enginePool.ensureSession(modelId, mobileTools(modelId))
            RuntimeResult.Success(Unit)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT load failed", e)
        }
    }

    override fun unloadModel(modelId: String) {
        enginePool.unload(modelId)
    }

    override fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk> {
        if (!useLiteRt(request.modelId)) {
            return flow {
                emit(
                    InferenceChunk(
                        "Install a LiteRT model under Manage → Models to use chat.",
                        isComplete = true,
                    ),
                )
            }
        }
        val lastUser = request.messages.lastOrNull { it.role == MessageRole.USER }?.content ?: ""
        return flow {
            try {
                enginePool.streamMessage(request.modelId, lastUser, mobileTools(request.modelId))
                    .collect { event ->
                        when (event) {
                            is StreamEvent.Token -> emit(InferenceChunk(event.text))
                            is StreamEvent.Done -> emit(InferenceChunk("", isComplete = true))
                        }
                    }
            } catch (e: Exception) {
                emit(InferenceChunk("LiteRT error: ${e.message}", isComplete = true))
            }
        }
    }

    override suspend fun complete(request: CompletionRequest): RuntimeResult<String> {
        if (!useLiteRt(request.modelId)) return modelNotInstalled(request.modelId)
        return try {
            val result = enginePool.sendTurn(request.modelId, request.prompt, mobileTools(request.modelId))
            RuntimeResult.Success(result.text)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT complete failed", e)
        }
    }

    override suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String> =
        RuntimeResult.Unsupported

    override suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String> =
        RuntimeResult.Unsupported

    override suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics> =
        RuntimeResult.Unsupported

    override fun capabilitiesFor(modelId: String): ModelCapabilities =
        ModelCapabilities(
            modelId = modelId,
            supportsChat = useLiteRt(modelId),
            supportsVision = false,
            supportsAudio = false,
            supportsStreaming = useLiteRt(modelId),
            supportsToolCalling = useLiteRt(modelId),
            supportsThinkingTrace = modelId == "gemma-4-e2b-it" && useLiteRt(modelId),
        )

    override suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn> {
        if (!useLiteRt(request.modelId)) return modelNotInstalled(request.modelId)
        if (request.toolResults.isNotEmpty()) {
            val last = request.toolResults.last()
            val prompt = "Tool result for ${last.toolId}: ${last.outputJson}. Summarize for the user."
            return try {
                val result = enginePool.sendTurn(request.modelId, prompt, mobileTools(request.modelId))
                RuntimeResult.Success(
                    AgentTurn.FinalText(
                        text = result.text.ifBlank { "Done on-device." },
                        thinkingTrace = result.thinkingTrace,
                    ),
                )
            } catch (e: Exception) {
                RuntimeResult.Error(e.message ?: "LiteRT agent turn failed", e)
            }
        }
        val lastUser = request.messages.lastOrNull { it.role == AgentMessageRole.USER }?.content.orEmpty()
        return try {
            val result = enginePool.sendTurn(request.modelId, lastUser, mobileTools(request.modelId))
            RuntimeResult.Success(
                AgentTurn.FinalText(
                    text = result.text,
                    thinkingTrace = result.thinkingTrace,
                ),
            )
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT agent turn failed", e)
        }
    }
}

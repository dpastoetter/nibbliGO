package com.nibbli.nibbligo.core.runtime.litert

import android.content.Context
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
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.ToolCall
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import com.nibbli.nibbligo.core.runtime.fake.FakeInferenceRuntime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRTInferenceRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val enginePool: LiteRtEnginePool,
    private val fakeInferenceRuntime: FakeInferenceRuntime,
) : InferenceRuntime {

    override val runtimeKind: RuntimeKind = RuntimeKind.LITERT

    private fun useLiteRt(modelId: String): Boolean = enginePool.hasModel(modelId)

    private fun mobileTools(modelId: String) = toolProvidersForModel(modelId) { /* UI layer handles */ }

    override suspend fun ensureModelLoaded(modelId: String): RuntimeResult<Unit> {
        if (!useLiteRt(modelId)) {
            return fakeInferenceRuntime.ensureModelLoaded(modelId)
        }
        return try {
            enginePool.ensureSession(modelId, mobileTools(modelId))
            RuntimeResult.Success(Unit)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT load failed", e)
        }
    }

    override fun unloadModel(modelId: String) {
        enginePool.unload(modelId)
        fakeInferenceRuntime.unloadModel(modelId)
    }

    override fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk> {
        if (!useLiteRt(request.modelId)) {
            return fakeInferenceRuntime.streamChat(request)
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
        if (!useLiteRt(request.modelId)) {
            return fakeInferenceRuntime.complete(request)
        }
        return try {
            val result = enginePool.sendTurn(request.modelId, request.prompt, mobileTools(request.modelId))
            RuntimeResult.Success(result.text)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT complete failed", e)
        }
    }

    override suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String> =
        fakeInferenceRuntime.analyzeImage(request)

    override suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String> =
        fakeInferenceRuntime.transcribeAudio(request)

    override suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics> =
        fakeInferenceRuntime.runBenchmark(modelId)

    override fun capabilitiesFor(modelId: String): ModelCapabilities {
        val base = fakeInferenceRuntime.capabilitiesFor(modelId)
        return base.copy(
            supportsToolCalling = useLiteRt(modelId) || base.supportsToolCalling ||
                modelId == "gemma-4-e2b-it" || modelId == "functiongemma-270m",
            supportsThinkingTrace = modelId == "gemma-4-e2b-it" || useLiteRt(modelId),
        )
    }

    override suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn> {
        if (!useLiteRt(request.modelId)) {
            return fakeInferenceRuntime.generateWithTools(request)
        }
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
                return fakeInferenceRuntime.generateWithTools(request)
            }
        }
        val lastUser = request.messages.lastOrNull { it.role == AgentMessageRole.USER }?.content.orEmpty()
        return try {
            val result = enginePool.sendTurn(request.modelId, lastUser, mobileTools(request.modelId))
            val toolCall = parseToolCallFromText(result.text)
            if (toolCall != null) {
                RuntimeResult.Success(
                    AgentTurn.ToolCalls(
                        calls = listOf(toolCall),
                        thinkingTrace = result.thinkingTrace.ifEmpty {
                            listOf("Planning next step on-device…")
                        },
                    ),
                )
            } else {
                RuntimeResult.Success(
                    AgentTurn.FinalText(
                        text = result.text,
                        thinkingTrace = result.thinkingTrace,
                    ),
                )
            }
        } catch (e: Exception) {
            fakeInferenceRuntime.generateWithTools(request)
        }
    }

    private fun parseToolCallFromText(text: String): ToolCall? {
        val lower = text.lowercase()
        return when {
            "remind" in lower -> ToolCall("reminder_create", """{"title":"Reminder","notes":"$text"}""")
            "clipboard" in lower || "summarize" in lower ->
                ToolCall("clipboard_summarize", "{}")
            "flashlight" in lower || "torch" in lower ->
                ToolCall("flashlight_toggle", """{"on":true}""")
            "settings" in lower -> ToolCall("open_settings", """{"screen":"main"}""")
            else -> null
        }
    }
}

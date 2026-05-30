package com.nibbli.nibbligo.core.runtime.litert

import com.nibbli.nibbligo.core.litert.engine.LiteRtEnginePool
import com.nibbli.nibbligo.core.litert.engine.StreamEvent
import com.nibbli.nibbligo.core.litert.engine.TurnResult
import com.nibbli.nibbligo.core.litert.engine.liteRtToolArgumentsToJson
import com.nibbli.nibbligo.core.model.AgentMessageRole
import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.PetTurnRequest
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.ToolCall
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import com.nibbli.nibbligo.core.mobileactions.MobileActionsPerformer
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRTInferenceRuntime @Inject constructor(
    private val enginePool: LiteRtEnginePool,
    private val mobileActionsPerformer: MobileActionsPerformer,
) : InferenceRuntime {

    override val runtimeKind: RuntimeKind = RuntimeKind.LITERT

    private fun useLiteRt(modelId: String): Boolean = enginePool.hasModel(modelId)

    private fun modelNotInstalled(modelId: String): RuntimeResult.Error =
        RuntimeResult.Error("Model $modelId is not installed. Download it under Manage → Models.")

    private fun mobileTools(modelId: String) = toolProvidersForModel(modelId, mobileActionsPerformer)

    override suspend fun ensureModelLoaded(modelId: String, includeTools: Boolean): RuntimeResult<Unit> {
        if (!useLiteRt(modelId)) return modelNotInstalled(modelId)
        val tools = if (includeTools) mobileTools(modelId) else emptyList()
        val systemInstruction = if (tools.isNotEmpty()) buildStaticMobileActionsSystemInstruction() else null
        return try {
            enginePool.ensureSession(modelId, tools, systemInstruction)
            RuntimeResult.Success(Unit)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT load failed", e)
        }
    }

    override suspend fun ensureAgentModelLoaded(modelId: String): RuntimeResult<Unit> {
        if (!useLiteRt(modelId)) return modelNotInstalled(modelId)
        val tools = agentToolProvidersForModel(modelId)
        if (tools.isEmpty()) {
            return RuntimeResult.Error(
                "Agent mobile actions need FunctionGemma 270M. Install it under Manage → Models.",
            )
        }
        val systemInstruction = agentSystemInstructionForModel(modelId)
        return try {
            enginePool.ensureSession(
                modelId = modelId,
                tools = tools,
                systemInstruction = systemInstruction,
                profile = LiteRtEnginePool.SESSION_PROFILE_MOBILE_ACTIONS,
            )
            RuntimeResult.Success(Unit)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT agent load failed", e)
        }
    }

    override suspend fun ensurePetModelLoaded(
        modelId: String,
        systemInstruction: String,
    ): RuntimeResult<Unit> {
        if (!useLiteRt(modelId)) return modelNotInstalled(modelId)
        return try {
            enginePool.ensureSession(
                modelId = modelId,
                tools = emptyList(),
                systemInstruction = systemInstruction,
                profile = LiteRtEnginePool.SESSION_PROFILE_PET_CHAT,
            )
            RuntimeResult.Success(Unit)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT pet load failed", e)
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
        val tools = if (request.includeTools) mobileTools(request.modelId) else emptyList()
        return try {
            val result = enginePool.sendTurn(request.modelId, request.prompt, tools)
            RuntimeResult.Success(result.text)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT complete failed", e)
        }
    }

    override suspend fun completePetTurn(request: PetTurnRequest): RuntimeResult<String> {
        if (!useLiteRt(request.modelId)) return modelNotInstalled(request.modelId)
        return try {
            val result = enginePool.sendTurn(
                modelId = request.modelId,
                userText = request.userMessage,
                tools = emptyList(),
                systemInstruction = request.systemInstruction,
                profile = LiteRtEnginePool.SESSION_PROFILE_PET_CHAT,
            )
            RuntimeResult.Success(result.text)
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT pet turn failed", e)
        }
    }

    override fun streamPetTurn(request: PetTurnRequest): Flow<InferenceChunk> = flow {
        if (!useLiteRt(request.modelId)) {
            emit(
                InferenceChunk(
                    "Install a LiteRT model under Manage → Models to use chat.",
                    isComplete = true,
                ),
            )
            return@flow
        }
        try {
            enginePool.streamMessage(
                modelId = request.modelId,
                userText = request.userMessage,
                tools = emptyList(),
                systemInstruction = request.systemInstruction,
                profile = LiteRtEnginePool.SESSION_PROFILE_PET_CHAT,
            ).collect { event ->
                when (event) {
                    is StreamEvent.Token -> emit(InferenceChunk(event.text))
                    is StreamEvent.Done -> emit(InferenceChunk("", isComplete = true))
                }
            }
        } catch (e: Exception) {
            val message = e.message?.takeIf { it.isNotBlank() } ?: "LiteRT pet error"
            emit(InferenceChunk(message))
            emit(InferenceChunk("", isComplete = true))
        }
    }

    override suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String> =
        RuntimeResult.Unsupported

    override suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String> =
        RuntimeResult.Unsupported

    override suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics> =
        RuntimeResult.Unsupported

    override fun capabilitiesFor(modelId: String): ModelCapabilities {
        val installed = useLiteRt(modelId)
        return ModelCapabilities(
            modelId = modelId,
            supportsChat = installed,
            supportsVision = false,
            supportsAudio = false,
            supportsStreaming = installed,
            supportsToolCalling = installed && modelId == FUNCTION_GEMMA_MODEL_ID,
            supportsThinkingTrace = installed && (
                modelId == "gemma-4-e2b-it" || modelId == "deepseek-r1-distill-qwen-1.5b"
                ),
        )
    }

    override suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn> {
        if (!useLiteRt(request.modelId)) return modelNotInstalled(request.modelId)
        val tools = agentToolProvidersForModel(request.modelId)
        if (tools.isEmpty()) {
            return RuntimeResult.Error(
                "This model cannot call phone tools. Select FunctionGemma 270M for email and calendar actions.",
            )
        }
        val systemInstruction = agentSystemInstructionForModel(request.modelId)
        return try {
            val result = if (request.toolResults.isNotEmpty()) {
                val last = request.toolResults.last()
                val toolMeta = request.tools.find { it.id == last.toolId || it.name == last.toolId }
                val toolName = toolMeta?.name ?: last.toolId
                val prompt = formatAgentUserMessage(
                    "The user approved $toolName. Execution result: ${last.outputJson}. " +
                        "Reply briefly to confirm what happened.",
                )
                enginePool.sendAgentTurn(request.modelId, prompt, tools, systemInstruction)
            } else {
                val lastUser =
                    request.messages.lastOrNull { it.role == AgentMessageRole.USER }?.content.orEmpty()
                enginePool.sendAgentTurn(
                    request.modelId,
                    formatAgentUserMessage(lastUser),
                    tools,
                    systemInstruction,
                )
            }
            RuntimeResult.Success(parseAgentTurn(result))
        } catch (e: Exception) {
            RuntimeResult.Error(e.message ?: "LiteRT agent turn failed", e)
        }
    }

    private fun parseAgentTurn(result: TurnResult): AgentTurn {
        if (result.toolCalls.isNotEmpty()) {
            val calls = result.toolCalls.map { toolCall ->
                ToolCall(
                    toolId = litertFunctionToToolId(toolCall.name),
                    argumentsJson = liteRtToolArgumentsToJson(toolCall.arguments),
                )
            }
            return AgentTurn.ToolCalls(calls, result.thinkingTrace)
        }
        return AgentTurn.FinalText(
            text = result.text.ifBlank { "Done on-device." },
            thinkingTrace = result.thinkingTrace,
        )
    }

    private companion object {
        const val FUNCTION_GEMMA_MODEL_ID = "functiongemma-270m"
    }
}

package com.nibbli.nibbligo.core.agent

import com.nibbli.nibbligo.core.agent.execution.ToolExecutor
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.model.AgentMessage
import com.nibbli.nibbligo.core.model.AgentMessageRole
import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentSessionState
import com.nibbli.nibbligo.core.model.AgentStep
import com.nibbli.nibbligo.core.model.AgentStepKind
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.ToolCall
import com.nibbli.nibbligo.core.model.ToolResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import javax.inject.Inject
import javax.inject.Singleton

data class PendingConfirmation(
    val call: ToolCall,
    val stepIndex: Int,
)

@Singleton
class AgentOrchestrator @Inject constructor(
    private val inferenceRuntime: InferenceRuntime,
    private val toolRegistry: ToolRegistry,
    private val toolExecutor: ToolExecutor,
    private val petEventBus: PetEventBus,
) {
    companion object {
        const val MAX_STEPS = 5
    }

    suspend fun runTurn(
        modelId: String,
        userMessage: String,
        session: AgentSessionState,
        params: GenerationParams = GenerationParams(),
        autoApproveSafeTools: Boolean = true,
    ): AgentRunResult {
        inferenceRuntime.ensureAgentModelLoaded(modelId).let { load ->
            if (load is RuntimeResult.Error) {
                return AgentRunResult(
                    session.copy(isRunning = false),
                    pendingConfirmation = null,
                    finalText = null,
                    error = load.message,
                )
            }
            if (load == RuntimeResult.LowMemory) {
                return AgentRunResult(
                    session.copy(isRunning = false),
                    pendingConfirmation = null,
                    finalText = null,
                    error = "Not enough memory to load the agent model.",
                )
            }
            if (load == RuntimeResult.Unsupported) {
                return AgentRunResult(
                    session.copy(isRunning = false),
                    pendingConfirmation = null,
                    finalText = null,
                    error = "Agent tools are not supported for this runtime.",
                )
            }
        }
        var messages = session.messages + AgentMessage(AgentMessageRole.USER, userMessage)
        val steps = session.steps.toMutableList()
        var stepIndex = steps.size
        val toolResults = mutableListOf<ToolResult>()

        repeat(MAX_STEPS) {
            val request = AgentRequest(
                modelId = modelId,
                messages = messages,
                tools = toolRegistry.allTools(),
                params = params,
                toolResults = toolResults,
            )
            when (val turn = inferenceRuntime.generateWithTools(request)) {
                is RuntimeResult.Success -> when (val data = turn.data) {
                    is AgentTurn.FinalText -> {
                        messages = messages + AgentMessage(AgentMessageRole.ASSISTANT, data.text)
                        data.thinkingTrace.forEach { trace ->
                            steps.add(step(stepIndex++, AgentStepKind.THINKING, trace))
                        }
                        steps.add(step(stepIndex, AgentStepKind.FINAL_ANSWER, data.text.take(120)))
                        petEventBus.emit(PetEvent.AgentStepCompleted)
                        return AgentRunResult(
                            session.copy(messages = messages, steps = steps, isRunning = false),
                            pendingConfirmation = null,
                            finalText = data.text,
                        )
                    }
                    is AgentTurn.ToolCalls -> {
                        data.thinkingTrace.forEach { trace ->
                            steps.add(step(stepIndex++, AgentStepKind.THINKING, trace))
                        }
                        val call = data.calls.first()
                        val tool = toolRegistry.findTool(call.toolId)
                        steps.add(
                            step(
                                stepIndex++,
                                AgentStepKind.TOOL_PROPOSED,
                                "Proposed: ${tool?.name ?: call.toolId}",
                            ),
                        )
                        val needsConfirm = tool != null && when (tool.riskLevel) {
                            com.nibbli.nibbligo.core.model.ToolRiskLevel.SENSITIVE -> true
                            com.nibbli.nibbligo.core.model.ToolRiskLevel.SAFE -> !autoApproveSafeTools
                        }
                        if (needsConfirm) {
                            return AgentRunResult(
                                session.copy(messages = messages, steps = steps, isRunning = false),
                                pendingConfirmation = PendingConfirmation(call, stepIndex),
                                finalText = null,
                            )
                        }
                        val result = toolExecutor.execute(call)
                        toolResults.add(result)
                        tool?.skillId?.let { sid ->
                            petEventBus.emit(PetEvent.SkillInvoked(sid))
                        }
                        steps.add(step(stepIndex++, AgentStepKind.TOOL_CONFIRMED, call.toolId))
                        steps.add(step(stepIndex++, AgentStepKind.TOOL_RESULT, result.outputJson.take(120)))
                        messages = messages + AgentMessage(
                            AgentMessageRole.TOOL,
                            result.outputJson,
                            toolCallId = call.toolId,
                        )
                    }
                }
                is RuntimeResult.Error -> {
                    return AgentRunResult(
                        session.copy(isRunning = false),
                        pendingConfirmation = null,
                        finalText = null,
                        error = turn.message,
                    )
                }
                else -> {
                    return AgentRunResult(
                        session.copy(isRunning = false),
                        pendingConfirmation = null,
                        finalText = null,
                        error = "Agent turn unsupported",
                    )
                }
            }
        }
        return AgentRunResult(
            session.copy(messages = messages, steps = steps, isRunning = false),
            pendingConfirmation = null,
            finalText = "Reached max agent steps.",
        )
    }

    suspend fun confirmAndContinue(
        modelId: String,
        session: AgentSessionState,
        pending: PendingConfirmation,
        params: GenerationParams = GenerationParams(),
    ): AgentRunResult {
        val result = toolExecutor.execute(pending.call)
        val steps = session.steps.toMutableList()
        var stepIndex = pending.stepIndex
        steps.add(step(stepIndex++, AgentStepKind.TOOL_CONFIRMED, pending.call.toolId))
        steps.add(step(stepIndex++, AgentStepKind.TOOL_RESULT, result.outputJson.take(120)))
        val tool = toolRegistry.findTool(pending.call.toolId)
        tool?.skillId?.let { sid ->
            petEventBus.emit(PetEvent.SkillInvoked(sid))
        }
        val messages = session.messages + AgentMessage(
            AgentMessageRole.TOOL,
            result.outputJson,
            toolCallId = pending.call.toolId,
        )
        return continueAfterTool(modelId, session.copy(messages = messages, steps = steps), listOf(result), params)
    }

    private suspend fun continueAfterTool(
        modelId: String,
        session: AgentSessionState,
        toolResults: List<ToolResult>,
        params: GenerationParams,
    ): AgentRunResult {
        val request = AgentRequest(
            modelId = modelId,
            messages = session.messages,
            tools = toolRegistry.allTools(),
            params = params,
            toolResults = toolResults,
        )
        return when (val turn = inferenceRuntime.generateWithTools(request)) {
            is RuntimeResult.Success -> when (val data = turn.data) {
                is AgentTurn.FinalText -> {
                    val messages = session.messages + AgentMessage(AgentMessageRole.ASSISTANT, data.text)
                    val steps = session.steps + step(
                        session.steps.size,
                        AgentStepKind.FINAL_ANSWER,
                        data.text.take(120),
                    )
                    petEventBus.emit(PetEvent.AgentStepCompleted)
                    AgentRunResult(
                        session.copy(messages = messages, steps = steps, isRunning = false),
                        pendingConfirmation = null,
                        finalText = data.text,
                    )
                }
                is AgentTurn.ToolCalls -> {
                    val call = data.calls.first()
                    val tool = toolRegistry.findTool(call.toolId)
                    if (tool != null && toolRegistry.requiresConfirmation(tool)) {
                        AgentRunResult(
                            session.copy(isRunning = false),
                            pendingConfirmation = PendingConfirmation(call, session.steps.size),
                            finalText = null,
                        )
                    } else {
                        val result = toolExecutor.execute(call)
                        val messages = session.messages + AgentMessage(
                            AgentMessageRole.TOOL,
                            result.outputJson,
                            toolCallId = call.toolId,
                        )
                        continueAfterTool(modelId, session.copy(messages = messages), listOf(result), params)
                    }
                }
            }
            is RuntimeResult.Error -> AgentRunResult(
                session.copy(isRunning = false),
                pendingConfirmation = null,
                finalText = null,
                error = turn.message,
            )
            else -> AgentRunResult(
                session.copy(isRunning = false),
                pendingConfirmation = null,
                finalText = null,
                error = "Agent turn unsupported",
            )
        }
    }

    private fun step(index: Int, kind: AgentStepKind, summary: String) = AgentStep(
        index = index,
        kind = kind,
        summary = summary,
        timestampMillis = System.currentTimeMillis(),
    )
}

data class AgentRunResult(
    val session: AgentSessionState,
    val pendingConfirmation: PendingConfirmation?,
    val finalText: String?,
    val error: String? = null,
)

package com.nibbli.nibbligo.core.model

enum class ToolSource {
    BUILTIN,
    SKILL_PACKAGE,
    ACTION,
}

enum class ToolRiskLevel {
    SAFE,
    SENSITIVE,
}

data class AgentTool(
    val id: String,
    val name: String,
    val description: String,
    val parametersJsonSchema: String,
    val riskLevel: ToolRiskLevel,
    val source: ToolSource,
    val skillId: String? = null,
)

data class ToolCall(
    val toolId: String,
    val argumentsJson: String,
)

data class ToolResult(
    val toolId: String,
    val success: Boolean,
    val outputJson: String,
)

data class AgentMessage(
    val role: AgentMessageRole,
    val content: String,
    val toolCallId: String? = null,
)

enum class AgentMessageRole {
    USER,
    ASSISTANT,
    TOOL,
    SYSTEM,
}

data class AgentRequest(
    val modelId: String,
    val messages: List<AgentMessage>,
    val tools: List<AgentTool>,
    val params: GenerationParams = GenerationParams(),
    val toolResults: List<ToolResult> = emptyList(),
)

sealed class AgentTurn {
    data class FinalText(
        val text: String,
        val thinkingTrace: List<String> = emptyList(),
    ) : AgentTurn()

    data class ToolCalls(
        val calls: List<ToolCall>,
        val thinkingTrace: List<String> = emptyList(),
    ) : AgentTurn()
}

data class AgentStep(
    val index: Int,
    val kind: AgentStepKind,
    val summary: String,
    val timestampMillis: Long,
)

enum class AgentStepKind {
    THINKING,
    TOOL_PROPOSED,
    TOOL_CONFIRMED,
    TOOL_RESULT,
    FINAL_ANSWER,
}

data class AgentSessionState(
    val conversationId: Long? = null,
    val modelId: String,
    val messages: List<AgentMessage> = emptyList(),
    val steps: List<AgentStep> = emptyList(),
    val pendingToolCall: ToolCall? = null,
    val isRunning: Boolean = false,
)

data class InstalledSkillPackage(
    val skillId: String,
    val displayName: String,
    val description: String,
    val localPath: String,
    val version: String,
    val permissions: String,
    val enabled: Boolean,
    val hasJsRuntime: Boolean,
    val installedAtMillis: Long,
)

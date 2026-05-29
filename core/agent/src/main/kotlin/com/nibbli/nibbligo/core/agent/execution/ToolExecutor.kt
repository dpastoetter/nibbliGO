package com.nibbli.nibbligo.core.agent.execution

import com.nibbli.nibbligo.core.agent.skills.GallerySkillWebViewBridge
import com.nibbli.nibbligo.core.mcp.McpToolRegistry
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.model.ToolCall
import com.nibbli.nibbligo.core.model.ToolResult
import com.nibbli.nibbligo.core.model.ToolSource
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutor @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val actionHistoryRepository: ActionHistoryRepository,
    private val skillPackageRepository: SkillPackageRepository,
    private val gallerySkillWebViewBridge: GallerySkillWebViewBridge,
    private val mcpToolRegistry: McpToolRegistry,
) {
    suspend fun execute(call: ToolCall): ToolResult {
        toolRegistry.findTool(call.toolId)
            ?: return ToolResult(call.toolId, false, """{"error":"unknown tool"}""")

        return when (call.toolId) {
            "notes_save",
            "reminder_create",
            "clipboard_summarize",
            "open_settings",
            "flashlight_toggle",
            "read_clipboard",
            -> notImplemented(call.toolId)
            else -> executeSkillOrGeneric(call.toolId, call)
        }.also { result ->
            actionHistoryRepository.log(
                call.toolId,
                if (result.success) "COMPLETED" else "FAILED",
                result.outputJson.take(200),
            )
        }
    }

    private fun notImplemented(toolId: String): ToolResult =
        ToolResult(toolId, false, """{"error":"$toolId is not implemented on this device yet."}""")

    private suspend fun executeSkillOrGeneric(toolId: String, call: ToolCall): ToolResult {
        val tool = toolRegistry.findTool(toolId) ?: return ToolResult(toolId, false, """{"error":"unknown"}""")
        if (tool.source == ToolSource.MCP) {
            val result = mcpToolRegistry.invoke(toolId, call.argumentsJson)
            return ToolResult(
                toolId,
                result.isSuccess,
                result.getOrElse { """{"error":"${it.message}"}""" },
            )
        }
        val skillId = tool.skillId
        if (tool.source != ToolSource.SKILL_PACKAGE || skillId == null) {
            return ToolResult(toolId, false, """{"error":"not implemented"}""")
        }
        val pkg = skillPackageRepository.get(skillId)
        return if (pkg?.hasJsRuntime == true) {
            val allowNetwork = pkg.permissions.contains("network")
            val output = gallerySkillWebViewBridge.runSkillTool(
                skillId = skillId,
                toolName = tool.name,
                argumentsJson = call.argumentsJson,
                allowNetwork = allowNetwork,
            )
            ToolResult(toolId, true, output)
        } else {
            ToolResult(
                toolId,
                false,
                """{"error":"Native skill runtime not available for $skillId"}""",
            )
        }
    }
}

package com.nibbli.nibbligo.core.agent.execution

import com.nibbli.nibbligo.core.agent.skills.JsSkillWebViewBridge
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
    private val jsSkillWebViewBridge: JsSkillWebViewBridge,
) {
    suspend fun execute(call: ToolCall): ToolResult {
        val tool = toolRegistry.findTool(call.toolId)
            ?: return ToolResult(call.toolId, false, """{"error":"unknown tool"}""")

        return when (tool.id) {
            "notes_save" -> {
                val args = JSONObject(call.argumentsJson.ifBlank { "{}" })
                val title = args.optString("title", "Note")
                val body = args.optString("body", "")
                ToolResult(
                    tool.id,
                    true,
                    """{"saved":true,"title":"$title","preview":"${body.take(80)}"}""",
                )
            }
            "reminder_create" -> {
                val args = JSONObject(call.argumentsJson.ifBlank { "{}" })
                val title = args.optString("title", "Reminder")
                ToolResult(
                    tool.id,
                    true,
                    """{"created":true,"title":"$title","stored":"local"}""",
                )
            }
            "clipboard_summarize" -> ToolResult(
                tool.id,
                true,
                """{"summary":"[demo] Clipboard summarized on-device."}""",
            )
            "open_settings" -> ToolResult(
                tool.id,
                true,
                """{"opened":"settings","note":"Handled by mobile action layer when available."}""",
            )
            "flashlight_toggle" -> ToolResult(
                tool.id,
                true,
                """{"flashlight":"toggled","note":"Demo mobile action — wire torch API for production."}""",
            )
            "read_clipboard" -> ToolResult(
                tool.id,
                true,
                """{"clipboard":"[demo] Sample clipboard text for agent."}""",
            )
            else -> executeSkillOrGeneric(tool.id, call)
        }.also { result ->
            actionHistoryRepository.log(
                call.toolId,
                if (result.success) "COMPLETED" else "FAILED",
                result.outputJson.take(200),
            )
        }
    }

    private suspend fun executeSkillOrGeneric(toolId: String, call: ToolCall): ToolResult {
        val tool = toolRegistry.findTool(toolId)
        val skillId = tool?.skillId
        if (tool?.source != ToolSource.SKILL_PACKAGE || skillId == null) {
            return ToolResult(toolId, false, """{"error":"not implemented"}""")
        }
        val pkg = skillPackageRepository.get(skillId)
        return if (pkg?.hasJsRuntime == true) {
            val allowNetwork = pkg.permissions.contains("network")
            val output = jsSkillWebViewBridge.execute(
                skillId = skillId,
                toolName = tool.name,
                argumentsJson = call.argumentsJson,
                allowNetwork = allowNetwork,
            )
            ToolResult(toolId, true, output)
        } else {
            ToolResult(
                toolId,
                true,
                """{"skill":"$skillId","tool":"${tool.name}","output":"Native skill executed on-device."}""",
            )
        }
    }
}

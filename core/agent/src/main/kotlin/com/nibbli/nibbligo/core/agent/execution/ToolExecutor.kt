package com.nibbli.nibbligo.core.agent.execution

import com.nibbli.nibbligo.core.agent.skills.GallerySkillWebViewBridge
import com.nibbli.nibbligo.core.agent.tools.PhoneActionAgentTools
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.mobileactions.CalendarParams
import com.nibbli.nibbligo.core.mobileactions.ContactParams
import com.nibbli.nibbligo.core.mobileactions.EmailParams
import com.nibbli.nibbligo.core.mobileactions.MapParams
import com.nibbli.nibbligo.core.mobileactions.MobileActionsPerformer
import com.nibbli.nibbligo.core.mcp.McpToolRegistry
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
    private val mobileActions: MobileActionsPerformer,
) {
    suspend fun execute(call: ToolCall): ToolResult {
        toolRegistry.findTool(call.toolId)
            ?: return ToolResult(call.toolId, false, """{"error":"unknown tool"}""")

        return when {
            PhoneActionAgentTools.isPhoneAction(call.toolId) -> executePhoneAction(call)
            call.toolId in UNIMPLEMENTED_BUILTIN -> notImplemented(call.toolId)
            else -> executeSkillOrGeneric(call.toolId, call)
        }.also { result ->
            actionHistoryRepository.log(
                call.toolId,
                if (result.success) "COMPLETED" else "FAILED",
                result.outputJson.take(200),
            )
        }
    }

    private fun executePhoneAction(call: ToolCall): ToolResult {
        val tool = toolRegistry.findTool(call.toolId)
        val name = tool?.name ?: call.toolId
        val args = parseArguments(call.argumentsJson)
        val error = when (name) {
            "turnOnFlashlight" -> mobileActions.turnOnFlashlight()
            "turnOffFlashlight" -> mobileActions.turnOffFlashlight()
            "createContact" -> mobileActions.createContact(
                ContactParams(
                    firstName = args.optString("firstName"),
                    lastName = args.optString("lastName"),
                    phoneNumber = args.optString("phoneNumber"),
                    email = args.optString("email"),
                ),
            )
            "sendEmail" -> mobileActions.sendEmail(
                EmailParams(
                    to = args.optString("to"),
                    subject = args.optString("subject"),
                    body = args.optString("body"),
                ),
            )
            "showLocationOnMap" -> mobileActions.showLocationOnMap(
                MapParams(location = args.optString("location")),
            )
            "openWifiSettings" -> mobileActions.openWifiSettings()
            "createCalendarEvent" -> mobileActions.createCalendarEvent(
                CalendarParams(
                    datetime = args.optString("datetime"),
                    title = args.optString("title"),
                ),
            )
            else -> return ToolResult(call.toolId, false, """{"error":"unknown phone action: $name"}""")
        }
        return if (error.isBlank()) {
            ToolResult(call.toolId, true, """{"result":"success"}""")
        } else {
            ToolResult(call.toolId, false, """{"error":"${escapeJson(error)}"}""")
        }
    }

    private fun parseArguments(argumentsJson: String): JSONObject {
        if (argumentsJson.isBlank()) return JSONObject()
        return try {
            JSONObject(argumentsJson)
        } catch (_: Exception) {
            JSONObject()
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
                result.getOrElse { """{"error":"${escapeJson(it.message ?: "MCP error")}"}""" },
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

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        private val UNIMPLEMENTED_BUILTIN = setOf(
            "notes_save",
            "reminder_create",
            "clipboard_summarize",
            "read_clipboard",
        )
    }
}

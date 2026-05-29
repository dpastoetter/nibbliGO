package com.nibbli.nibbligo.core.agent.tools

import com.nibbli.nibbligo.core.model.AgentTool
import com.nibbli.nibbligo.core.model.ToolRiskLevel
import com.nibbli.nibbligo.core.model.ToolSource

object BuiltinTools {
    val all: List<AgentTool> = listOf(
        AgentTool(
            id = "notes_save",
            name = "save_note",
            description = "Save a short note locally on the device.",
            parametersJsonSchema = """{"type":"object","properties":{"title":{"type":"string"},"body":{"type":"string"}},"required":["title","body"]}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.BUILTIN,
        ),
        AgentTool(
            id = "reminder_create",
            name = "create_reminder",
            description = "Create a local reminder entry.",
            parametersJsonSchema = """{"type":"object","properties":{"title":{"type":"string"},"notes":{"type":"string"}},"required":["title"]}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.BUILTIN,
        ),
        AgentTool(
            id = "clipboard_summarize",
            name = "summarize_clipboard",
            description = "Summarize clipboard text on-device (demo).",
            parametersJsonSchema = """{"type":"object","properties":{}}""",
            riskLevel = ToolRiskLevel.SAFE,
            source = ToolSource.BUILTIN,
        ),
        AgentTool(
            id = "read_clipboard",
            name = "read_clipboard",
            description = "Read clipboard for agent context (requires confirmation).",
            parametersJsonSchema = """{"type":"object","properties":{}}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.BUILTIN,
        ),
    )
}

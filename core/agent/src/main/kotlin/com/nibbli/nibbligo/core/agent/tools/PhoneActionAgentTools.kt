package com.nibbli.nibbligo.core.agent.tools

import com.nibbli.nibbligo.core.model.AgentTool
import com.nibbli.nibbligo.core.model.ToolRiskLevel
import com.nibbli.nibbligo.core.model.ToolSource

/**
 * Gallery Mobile Actions tools for Agent Chat — names match FunctionGemma / [NibbliMobileActionsToolSet].
 */
object PhoneActionAgentTools {
    val all: List<AgentTool> = listOf(
        AgentTool(
            id = "phone_turn_on_flashlight",
            name = "turnOnFlashlight",
            description = "Turns the flashlight on",
            parametersJsonSchema = """{"type":"object","properties":{}}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.ACTION,
        ),
        AgentTool(
            id = "phone_turn_off_flashlight",
            name = "turnOffFlashlight",
            description = "Turns the flashlight off",
            parametersJsonSchema = """{"type":"object","properties":{}}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.ACTION,
        ),
        AgentTool(
            id = "phone_create_contact",
            name = "createContact",
            description = "Creates a contact in the phone's contact list.",
            parametersJsonSchema = """{"type":"object","properties":{"firstName":{"type":"string"},"lastName":{"type":"string"},"phoneNumber":{"type":"string"},"email":{"type":"string"}},"required":["firstName","lastName","phoneNumber","email"]}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.ACTION,
        ),
        AgentTool(
            id = "phone_send_email",
            name = "sendEmail",
            description = "Sends an email.",
            parametersJsonSchema = """{"type":"object","properties":{"to":{"type":"string"},"subject":{"type":"string"},"body":{"type":"string"}},"required":["to","subject","body"]}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.ACTION,
        ),
        AgentTool(
            id = "phone_show_map",
            name = "showLocationOnMap",
            description = "Shows a location on the map.",
            parametersJsonSchema = """{"type":"object","properties":{"location":{"type":"string"}},"required":["location"]}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.ACTION,
        ),
        AgentTool(
            id = "phone_open_wifi",
            name = "openWifiSettings",
            description = "Opens the WiFi settings.",
            parametersJsonSchema = """{"type":"object","properties":{}}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.ACTION,
        ),
        AgentTool(
            id = "phone_create_calendar",
            name = "createCalendarEvent",
            description = "Creates a new calendar event.",
            parametersJsonSchema = """{"type":"object","properties":{"datetime":{"type":"string"},"title":{"type":"string"}},"required":["datetime","title"]}""",
            riskLevel = ToolRiskLevel.SENSITIVE,
            source = ToolSource.ACTION,
        ),
    )

    val ids: Set<String> = all.map { it.id }.toSet()
    val names: Set<String> = all.map { it.name }.toSet()

    fun isPhoneAction(toolId: String): Boolean =
        toolId in ids || toolId in names
}

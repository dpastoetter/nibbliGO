package com.nibbli.nibbligo.core.runtime.litert

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool
import com.nibbli.nibbligo.core.mobileactions.CalendarParams
import com.nibbli.nibbligo.core.mobileactions.ContactParams
import com.nibbli.nibbligo.core.mobileactions.EmailParams
import com.nibbli.nibbligo.core.mobileactions.MapParams
import com.nibbli.nibbligo.core.mobileactions.MobileActionsPerformer
import com.nibbli.nibbligo.core.model.ToolRiskLevel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Gallery-style mobile actions ToolSet for FunctionGemma / tool-capable models.
 * Ported from gallery@main: MobileActionsTools.kt (Apache 2.0)
 */
class NibbliMobileActionsToolSet(
    private val performer: MobileActionsPerformer,
) : ToolSet {

    @Tool(description = "Turns the flashlight on")
    fun turnOnFlashlight(): Map<String, String> {
        performer.turnOnFlashlight()
        return mapOf("result" to "success")
    }

    @Tool(description = "Turns the flashlight off")
    fun turnOffFlashlight(): Map<String, String> {
        performer.turnOffFlashlight()
        return mapOf("result" to "success")
    }

    @Tool(description = "Creates a contact in the phone's contact list.")
    fun createContact(
        @ToolParam(description = "The first name of the contact.") firstName: String,
        @ToolParam(description = "The last name of the contact.") lastName: String,
        @ToolParam(description = "The phone number of the contact.") phoneNumber: String,
        @ToolParam(description = "The email address of the contact.") email: String,
    ): Map<String, String> {
        performer.createContact(
            ContactParams(
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                email = email,
            ),
        )
        return mapOf(
            "result" to "success",
            "first_name" to firstName,
            "last_name" to lastName,
            "phone_number" to phoneNumber,
            "email" to email,
        )
    }

    @Tool(description = "Sends an email.")
    fun sendEmail(
        @ToolParam(description = "The email address of the recipient.") to: String,
        @ToolParam(description = "The subject of the email.") subject: String,
        @ToolParam(description = "The body of the email.") body: String,
    ): Map<String, String> {
        performer.sendEmail(EmailParams(to = to, subject = subject, body = body))
        return mapOf("result" to "success", "to" to to, "subject" to subject, "body" to body)
    }

    @Tool(description = "Shows a location on the map.")
    fun showLocationOnMap(
        @ToolParam(
            description =
                "The location to search for. May be the name of a place, a business, or an address.",
        )
        location: String,
    ): Map<String, String> {
        performer.showLocationOnMap(MapParams(location = location))
        return mapOf("result" to "success", "location" to location)
    }

    @Tool(description = "Opens the WiFi settings.")
    fun openWifiSettings(): Map<String, String> {
        performer.openWifiSettings()
        return mapOf("result" to "success")
    }

    @Tool(description = "Creates a new calendar event.")
    fun createCalendarEvent(
        @ToolParam(description = "The date and time of the event in the format YYYY-MM-DDTHH:MM:SS.")
        datetime: String,
        @ToolParam(description = "The title of the event.") title: String,
    ): Map<String, String> {
        performer.createCalendarEvent(CalendarParams(datetime = datetime, title = title))
        return mapOf("result" to "success", "datetime" to datetime, "title" to title)
    }
}

/**
 * Same schemas as [NibbliMobileActionsToolSet] but does not run device actions.
 * Used by Agent Chat so [ToolExecutor] runs actions after user confirmation.
 */
class NibbliMobileActionsToolSetDeferred : ToolSet {

    @Tool(description = "Turns the flashlight on")
    fun turnOnFlashlight(): Map<String, String> = mapOf("result" to "pending")

    @Tool(description = "Turns the flashlight off")
    fun turnOffFlashlight(): Map<String, String> = mapOf("result" to "pending")

    @Tool(description = "Creates a contact in the phone's contact list.")
    fun createContact(
        @ToolParam(description = "The first name of the contact.") firstName: String,
        @ToolParam(description = "The last name of the contact.") lastName: String,
        @ToolParam(description = "The phone number of the contact.") phoneNumber: String,
        @ToolParam(description = "The email address of the contact.") email: String,
    ): Map<String, String> = mapOf("result" to "pending")

    @Tool(description = "Sends an email.")
    fun sendEmail(
        @ToolParam(description = "The email address of the recipient.") to: String,
        @ToolParam(description = "The subject of the email.") subject: String,
        @ToolParam(description = "The body of the email.") body: String,
    ): Map<String, String> = mapOf("result" to "pending")

    @Tool(description = "Shows a location on the map.")
    fun showLocationOnMap(
        @ToolParam(
            description =
                "The location to search for. May be the name of a place, a business, or an address.",
        )
        location: String,
    ): Map<String, String> = mapOf("result" to "pending")

    @Tool(description = "Opens the WiFi settings.")
    fun openWifiSettings(): Map<String, String> = mapOf("result" to "pending")

    @Tool(description = "Creates a new calendar event.")
    fun createCalendarEvent(
        @ToolParam(description = "The date and time of the event in the format YYYY-MM-DDTHH:MM:SS.")
        datetime: String,
        @ToolParam(description = "The title of the event.") title: String,
    ): Map<String, String> = mapOf("result" to "pending")
}

fun mobileActionsSystemInstruction(): String {
    val now = LocalDateTime.now()
    val dateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
    val dayOfWeek = now.format(DateTimeFormatter.ofPattern("EEEE"))
    return buildString {
        appendLine("You are a model that can do function calling with the following functions.")
        append("Current date and time (YYYY-MM-DDTHH:MM:SS): ")
        appendLine(dateTime)
        append("Day of week: ")
        append(dayOfWeek)
    }
}

private val LITERT_FUNCTION_TO_TOOL_ID = mapOf(
    "turnOnFlashlight" to "phone_turn_on_flashlight",
    "turnOffFlashlight" to "phone_turn_off_flashlight",
    "createContact" to "phone_create_contact",
    "sendEmail" to "phone_send_email",
    "showLocationOnMap" to "phone_show_map",
    "openWifiSettings" to "phone_open_wifi",
    "createCalendarEvent" to "phone_create_calendar",
)

fun litertFunctionToToolId(functionName: String): String =
    LITERT_FUNCTION_TO_TOOL_ID[functionName] ?: functionName

fun toolProvidersForModel(
    modelId: String,
    performer: MobileActionsPerformer,
): List<ToolProvider> = when (modelId) {
    "functiongemma-270m" -> listOf(tool(NibbliMobileActionsToolSet(performer)))
    else -> emptyList()
}

fun agentToolProvidersForModel(modelId: String): List<ToolProvider> = when (modelId) {
    "functiongemma-270m" -> listOf(tool(NibbliMobileActionsToolSetDeferred()))
    else -> emptyList()
}

fun agentSystemInstructionForModel(modelId: String): String? = when (modelId) {
    "functiongemma-270m" -> mobileActionsSystemInstruction()
    else -> null
}

fun ToolRiskLevel.requiresConfirmation(): Boolean = this == ToolRiskLevel.SENSITIVE

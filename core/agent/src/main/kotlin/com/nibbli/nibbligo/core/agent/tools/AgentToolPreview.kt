package com.nibbli.nibbligo.core.agent.tools

import org.json.JSONObject

object AgentToolPreview {
    fun title(toolId: String): String = when (toolId) {
        "phone_send_email", "sendEmail" -> "Draft email"
        "phone_create_calendar", "createCalendarEvent" -> "Draft calendar event"
        "phone_create_contact", "createContact" -> "Create contact"
        "phone_show_map", "showLocationOnMap" -> "Show on map"
        "phone_turn_on_flashlight", "turnOnFlashlight" -> "Turn flashlight on"
        "phone_turn_off_flashlight", "turnOffFlashlight" -> "Turn flashlight off"
        "phone_open_wifi", "openWifiSettings" -> "Open Wi‑Fi settings"
        else -> "Use tool: $toolId"
    }

    fun description(toolId: String, argumentsJson: String): String {
        val args = parseArguments(argumentsJson)
        return when (toolId) {
            "phone_send_email", "sendEmail" -> {
                val to = args.optString("to").ifBlank { "recipient" }
                val subject = args.optString("subject").ifBlank { "no subject" }
                "Open your email app with a draft to $to — \"$subject\"."
            }
            "phone_create_calendar", "createCalendarEvent" -> {
                val title = args.optString("title").ifBlank { "event" }
                val datetime = args.optString("datetime").ifBlank { "unspecified time" }
                "Open your calendar app with \"$title\" at $datetime."
            }
            "phone_create_contact", "createContact" -> {
                val name = listOf(
                    args.optString("firstName"),
                    args.optString("lastName"),
                ).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "contact" }
                "Open Contacts to create $name."
            }
            "phone_show_map", "showLocationOnMap" -> {
                val location = args.optString("location").ifBlank { "location" }
                "Open Maps and search for \"$location\"."
            }
            "phone_turn_on_flashlight", "turnOnFlashlight" -> "Turn the device flashlight on."
            "phone_turn_off_flashlight", "turnOffFlashlight" -> "Turn the device flashlight off."
            "phone_open_wifi", "openWifiSettings" -> "Open system Wi‑Fi settings."
            else -> argumentsJson.ifBlank { "Review this action before confirming." }
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
}

package com.nibbli.nibbligo.feature.actions.domain

import com.nibbli.nibbligo.core.mobileactions.CalendarParams
import com.nibbli.nibbligo.core.mobileactions.ContactParams
import com.nibbli.nibbligo.core.mobileactions.EmailParams
import com.nibbli.nibbligo.core.mobileactions.MapParams
import com.nibbli.nibbligo.core.mobileactions.MobileActionsPerformer
import com.nibbli.nibbligo.core.model.ActionCategory
import com.nibbli.nibbligo.core.model.ActionDraft
import com.nibbli.nibbligo.core.model.SafeAction
import javax.inject.Inject
import javax.inject.Singleton

object ActionRegistry {
    val phoneActions: List<SafeAction> = listOf(
        SafeAction("phone_flashlight_on", "Flashlight on", "Turns the device flashlight on.", ActionCategory.PHONE),
        SafeAction("phone_flashlight_off", "Flashlight off", "Turns the device flashlight off.", ActionCategory.PHONE),
        SafeAction(
            "phone_create_contact",
            "Create contact",
            "Opens the contacts app with a new contact prefilled.",
            ActionCategory.PHONE,
        ),
        SafeAction("phone_send_email", "Send email", "Opens your email app with a draft message.", ActionCategory.PHONE),
        SafeAction("phone_show_map", "Show on map", "Opens Maps and searches for a location.", ActionCategory.PHONE),
        SafeAction("phone_open_wifi", "Wi‑Fi settings", "Opens the system Wi‑Fi settings screen.", ActionCategory.PHONE),
        SafeAction(
            "phone_create_calendar",
            "Calendar event",
            "Opens the calendar app with a new event prefilled.",
            ActionCategory.PHONE,
        ),
    )

    val productivityActions: List<SafeAction> = listOf(
        SafeAction(
            "clipboard_summarize",
            "Summarize clipboard",
            "Reads clipboard text and summarizes on-device.",
            ActionCategory.CLIPBOARD,
        ),
        SafeAction(
            "notes_to_tasks",
            "Notes → tasks",
            "Turns bullet notes into a task checklist.",
            ActionCategory.NOTES,
        ),
        SafeAction(
            "rename_preview",
            "Rename by rule",
            "Previews file renames from a pattern — no changes until you confirm.",
            ActionCategory.FILES,
        ),
        SafeAction(
            "reminder_entry",
            "Quick reminder",
            "Creates a local reminder entry.",
            ActionCategory.REMINDERS,
        ),
    )

    val actions: List<SafeAction> = phoneActions + productivityActions

    fun find(id: String): SafeAction? = actions.find { it.id == id }

    fun requiresParams(id: String): Boolean = when (id) {
        "phone_create_contact",
        "phone_send_email",
        "phone_show_map",
        "phone_create_calendar",
        -> true
        else -> false
    }
}

data class PhoneActionInput(
    val contact: ContactParams = ContactParams(),
    val email: EmailParams = EmailParams(),
    val map: MapParams = MapParams(),
    val calendar: CalendarParams = CalendarParams(),
)

@Singleton
class ActionExecutor @Inject constructor(
    private val mobileActions: MobileActionsPerformer,
) {
    fun buildDraft(
        actionId: String,
        petTaskMode: Boolean,
        input: PhoneActionInput = PhoneActionInput(),
    ): ActionDraft? {
        val action = ActionRegistry.find(actionId) ?: return null
        val preview = previewFor(actionId, input)
        return ActionDraft(
            actionId = actionId,
            title = if (petTaskMode) "nibbli task: ${action.title}" else action.title,
            description = action.description,
            preview = preview,
            requiresConfirmation = true,
        )
    }

    fun executeConfirmed(
        actionId: String,
        petTaskMode: Boolean,
        input: PhoneActionInput = PhoneActionInput(),
    ): String {
        val prefix = if (petTaskMode) "nibbli says: " else ""
        val error = when (actionId) {
            "phone_flashlight_on" -> mobileActions.turnOnFlashlight()
            "phone_flashlight_off" -> mobileActions.turnOffFlashlight()
            "phone_create_contact" -> mobileActions.createContact(input.contact)
            "phone_send_email" -> mobileActions.sendEmail(input.email)
            "phone_show_map" -> mobileActions.showLocationOnMap(input.map)
            "phone_open_wifi" -> mobileActions.openWifiSettings()
            "phone_create_calendar" -> mobileActions.createCalendarEvent(input.calendar)
            else -> return "${prefix}Action \"$actionId\" is not implemented on this device yet."
        }
        return if (error.isBlank()) {
            "${prefix}${successMessage(actionId)}"
        } else {
            "${prefix}Failed: $error"
        }
    }

    private fun previewFor(actionId: String, input: PhoneActionInput): String = when (actionId) {
        "phone_flashlight_on" -> "Turn the flashlight on?"
        "phone_flashlight_off" -> "Turn the flashlight off?"
        "phone_create_contact" ->
            "Create contact ${input.contact.firstName} ${input.contact.lastName} " +
                "(${input.contact.phoneNumber}, ${input.contact.email})?"
        "phone_send_email" ->
            "Email ${input.email.to} — \"${input.email.subject}\"?"
        "phone_show_map" -> "Open map for \"${input.map.location}\"?"
        "phone_open_wifi" -> "Open Wi‑Fi settings?"
        "phone_create_calendar" ->
            "Create \"${input.calendar.title}\" at ${input.calendar.datetime}?"
        else -> "This action is not implemented yet."
    }

    private fun successMessage(actionId: String): String = when (actionId) {
        "phone_flashlight_on" -> "Flashlight turned on."
        "phone_flashlight_off" -> "Flashlight turned off."
        "phone_create_contact" -> "Contacts app opened."
        "phone_send_email" -> "Email app opened."
        "phone_show_map" -> "Maps opened."
        "phone_open_wifi" -> "Wi‑Fi settings opened."
        "phone_create_calendar" -> "Calendar app opened."
        else -> "Done."
    }
}

package com.nibbli.nibbligo.feature.actions.domain

import com.nibbli.nibbligo.core.model.ActionCategory
import com.nibbli.nibbligo.core.model.ActionDraft
import com.nibbli.nibbligo.core.model.SafeAction

object ActionRegistry {
    val actions: List<SafeAction> = listOf(
        SafeAction("clipboard_summarize", "Summarize clipboard", "Reads clipboard text and summarizes on-device.", ActionCategory.CLIPBOARD),
        SafeAction("notes_to_tasks", "Notes → tasks", "Turns bullet notes into a task checklist.", ActionCategory.NOTES),
        SafeAction("rename_preview", "Rename by rule", "Previews file renames from a pattern — no changes until you confirm.", ActionCategory.FILES),
        SafeAction("screenshot_notes", "Screenshot → note", "Parses a screenshot into structured notes (demo).", ActionCategory.NOTES),
        SafeAction("reminder_entry", "Quick reminder", "Creates a local reminder entry.", ActionCategory.REMINDERS),
    )

    fun find(id: String): SafeAction? = actions.find { it.id == id }
}

object ActionExecutor {
    fun buildDraft(actionId: String, petTaskMode: Boolean): ActionDraft? {
        val action = ActionRegistry.find(actionId) ?: return null
        val preview = when (actionId) {
            "clipboard_summarize" -> "Clipboard: [demo] Meeting notes from Tuesday…"
            "notes_to_tasks" -> "• Task 1: Review model\n• Task 2: Feed nibbli"
            "rename_preview" -> "photo_001.jpg → vacation_001.jpg"
            "screenshot_notes" -> "Title: Settings screen\nBullets: Privacy on-device enabled"
            "reminder_entry" -> "Reminder: Try Prompt Lab at 6pm"
            else -> "Preview unavailable"
        }
        val title = if (petTaskMode) "nibbli task: ${action.title}" else action.title
        return ActionDraft(
            actionId = actionId,
            title = title,
            description = action.description,
            preview = preview,
            requiresConfirmation = true,
        )
    }

    fun executeConfirmed(actionId: String, petTaskMode: Boolean): String {
        val prefix = if (petTaskMode) "nibbli says: " else ""
        return when (actionId) {
            "clipboard_summarize" -> "${prefix}Summary saved to local notes (demo)."
            "notes_to_tasks" -> "${prefix}Task list saved locally (demo)."
            "rename_preview" -> "${prefix}No files renamed — preview only in v1."
            "screenshot_notes" -> "${prefix}Note draft saved locally (demo)."
            "reminder_entry" -> "${prefix}Reminder stored on-device (demo)."
            else -> "${prefix}Done."
        }
    }
}

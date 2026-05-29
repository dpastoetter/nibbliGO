package com.nibbli.nibbligo.feature.actions.domain

import com.nibbli.nibbligo.core.model.ActionCategory
import com.nibbli.nibbligo.core.model.ActionDraft
import com.nibbli.nibbligo.core.model.SafeAction

object ActionRegistry {
    val actions: List<SafeAction> = listOf(
        SafeAction("clipboard_summarize", "Summarize clipboard", "Reads clipboard text and summarizes on-device.", ActionCategory.CLIPBOARD),
        SafeAction("notes_to_tasks", "Notes → tasks", "Turns bullet notes into a task checklist.", ActionCategory.NOTES),
        SafeAction("rename_preview", "Rename by rule", "Previews file renames from a pattern — no changes until you confirm.", ActionCategory.FILES),
        SafeAction("reminder_entry", "Quick reminder", "Creates a local reminder entry.", ActionCategory.REMINDERS),
    )

    fun find(id: String): SafeAction? = actions.find { it.id == id }
}

object ActionExecutor {
    fun buildDraft(actionId: String, petTaskMode: Boolean): ActionDraft? {
        val action = ActionRegistry.find(actionId) ?: return null
        return ActionDraft(
            actionId = actionId,
            title = if (petTaskMode) "nibbli task: ${action.title}" else action.title,
            description = action.description,
            preview = "This action is not implemented yet.",
            requiresConfirmation = true,
        )
    }

    fun executeConfirmed(actionId: String, petTaskMode: Boolean): String {
        val prefix = if (petTaskMode) "nibbli says: " else ""
        return "${prefix}Action \"$actionId\" is not implemented on this device yet."
    }
}

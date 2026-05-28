package com.nibbli.nibbligo.core.model

data class ActionDraft(
    val actionId: String,
    val title: String,
    val description: String,
    val preview: String,
    val requiresConfirmation: Boolean = true,
)

data class SafeAction(
    val id: String,
    val title: String,
    val description: String,
    val category: ActionCategory,
)

enum class ActionCategory {
    CLIPBOARD,
    NOTES,
    FILES,
    REMINDERS,
}

data class ActionHistoryEntry(
    val id: Long = 0,
    val actionId: String,
    val status: ActionStatus,
    val timestampMillis: Long,
    val summary: String,
)

enum class ActionStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
}

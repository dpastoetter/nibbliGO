package com.nibbli.nibbligo.feature.pet.presentation

enum class TalkHistoryRole {
    USER,
    PET,
}

data class TalkHistoryEntry(
    val role: TalkHistoryRole,
    val text: String,
)

private const val MAX_TALK_HISTORY = 20

fun appendTalkHistory(
    history: List<TalkHistoryEntry>,
    entry: TalkHistoryEntry,
): List<TalkHistoryEntry> =
    (history + entry).takeLast(MAX_TALK_HISTORY)

fun updateLastPetTalkEntry(
    history: List<TalkHistoryEntry>,
    text: String,
): List<TalkHistoryEntry> {
    if (history.isEmpty()) {
        return listOf(TalkHistoryEntry(TalkHistoryRole.PET, text))
    }
    val last = history.last()
    return if (last.role == TalkHistoryRole.PET) {
        history.dropLast(1) + last.copy(text = text)
    } else {
        history + TalkHistoryEntry(TalkHistoryRole.PET, text)
    }
}

fun talkHistoryToRecentLines(history: List<TalkHistoryEntry>): List<String> =
    history.takeLast(2).map { entry ->
        when (entry.role) {
            TalkHistoryRole.USER -> "You: ${entry.text}"
            TalkHistoryRole.PET -> entry.text
        }
    }

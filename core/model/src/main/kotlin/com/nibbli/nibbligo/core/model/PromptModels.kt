package com.nibbli.nibbligo.core.model

enum class PromptPreset(val label: String) {
    SUMMARIZE("Summarize"),
    REWRITE("Rewrite"),
    CLASSIFY("Classify"),
    EXTRACT("Extract"),
    BRAINSTORM("Brainstorm"),
    TRANSLATE("Translate"),
    PLAN("Plan"),
}

data class SavedPrompt(
    val id: Long = 0,
    val title: String,
    val body: String,
    val preset: PromptPreset?,
    val isFavorite: Boolean,
    val createdAtMillis: Long,
)

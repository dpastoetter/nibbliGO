package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.CompanionMemoryLimits

/** Formats long-term companion facts for home-talk prompts. */
object CompanionMemoryRenderer {
    const val MAX_FACTS = CompanionMemoryLimits.MAX_FACTS
    const val MAX_RENDER_CHARS = CompanionMemoryLimits.MAX_RENDER_CHARS
    const val MAX_FACT_CHARS = CompanionMemoryLimits.MAX_FACT_CHARS
    const val FACT_SEPARATOR = CompanionMemoryLimits.FACT_SEPARATOR

    fun parseFacts(summary: String): List<String> =
        summary.split(FACT_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

    fun joinFacts(facts: List<String>): String =
        facts.take(MAX_FACTS)
            .joinToString(FACT_SEPARATOR)
            .take(MAX_RENDER_CHARS)

    fun renderKnownAboutCaretaker(memorySummary: String): String? {
        val facts = parseFacts(memorySummary).take(8)
        if (facts.isEmpty()) return null
        return buildString {
            appendLine("Known about caretaker:")
            facts.forEach { appendLine("• $it") }
        }.trim().take(MAX_RENDER_CHARS)
    }

    fun renderRecentTurns(
        turns: List<TalkTurnPair>,
        include: Boolean = true,
    ): String? {
        if (!include || turns.isEmpty()) return null
        return buildString {
            appendLine("Recent:")
            turns.takeLast(6).forEach { turn ->
                appendLine("Caretaker: ${turn.userMessage.take(120)}")
                appendLine("You: ${turn.petDialogue.take(120)}")
            }
        }.trim()
    }
}

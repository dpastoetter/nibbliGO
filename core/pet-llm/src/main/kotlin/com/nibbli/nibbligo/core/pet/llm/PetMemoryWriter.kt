package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetState

/** User-approved and manual facts for companion talk context. */
object PetMemoryWriter {
    private const val MAX_MEMORY_CHARS = CompanionMemoryRenderer.MAX_RENDER_CHARS
    private const val MAX_FACT_CHARS = CompanionMemoryRenderer.MAX_FACT_CHARS

    fun appendFact(current: String, fact: String): String {
        val trimmedFact = fact.trim().take(MAX_FACT_CHARS)
        if (trimmedFact.isBlank()) return current
        val facts = CompanionMemoryRenderer.parseFacts(current).toMutableList()
        if (facts.none { it.equals(trimmedFact, ignoreCase = true) }) {
            facts.add(trimmedFact)
        }
        while (CompanionMemoryRenderer.joinFacts(facts).length > MAX_MEMORY_CHARS && facts.size > 1) {
            facts.removeAt(0)
        }
        return CompanionMemoryRenderer.joinFacts(facts)
    }

    /** Suggests a user-approved memory line after Home talk (never auto-saved). */
    fun proposeUserFact(userMessage: String): String? {
        val trimmed = userMessage.trim()
        if (trimmed.length < 10) return null
        val lower = trimmed.lowercase()
        val triggers = listOf(
            "i'm ",
            "i am ",
            "my name",
            "i like ",
            "i love ",
            "i work ",
            "i study ",
            "i'm studying",
            "remember that",
            "call me ",
        )
        if (triggers.none { lower.contains(it) }) return null
        return trimmed.take(MAX_FACT_CHARS)
    }

    @Deprecated("Ambient reactions no longer auto-write memory")
    fun withNewFact(state: PetState, request: PetReactionRequest, dialogue: String): PetState = state
}

package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetState

/** Appends short facts to [PetState.memorySummary] after successful LLM reactions. */
object PetMemoryWriter {
    private const val MAX_MEMORY_CHARS = 400
    private const val MAX_FACT_CHARS = 120
    private const val SEPARATOR = " • "

    fun appendFact(current: String, fact: String): String {
        val trimmedFact = fact.trim().take(MAX_FACT_CHARS)
        if (trimmedFact.isBlank()) return current

        val facts = if (current.isBlank()) {
            mutableListOf()
        } else {
            current.split(SEPARATOR).filter { it.isNotBlank() }.toMutableList()
        }
        facts.add(trimmedFact)
        while (facts.joinToString(SEPARATOR).length > MAX_MEMORY_CHARS && facts.size > 1) {
            facts.removeAt(0)
        }
        return facts.joinToString(SEPARATOR).take(MAX_MEMORY_CHARS)
    }

    /** Builds a one-line memory fact from a reaction, or null if not worth remembering. */
    fun factFromReaction(request: PetReactionRequest, dialogue: String): String? {
        if (request.moodPulse) return null
        val line = dialogue.trim().take(80)
        if (line.isBlank()) return null

        return when {
            request.userMessage != null ->
                "User said \"${request.userMessage.take(40)}\"; I replied \"${line.take(50)}\""
            request.activityHint != null ->
                "After ${request.activityHint.take(60)}, I said \"${line.take(50)}\""
            request.lastAction != null ->
                "When ${request.lastAction.take(40)}, I said \"${line.take(50)}\""
            else -> null
        }
    }

    fun withNewFact(state: PetState, request: PetReactionRequest, dialogue: String): PetState {
        val fact = factFromReaction(request, dialogue) ?: return state
        return state.copy(memorySummary = appendFact(state.memorySummary, fact))
    }
}

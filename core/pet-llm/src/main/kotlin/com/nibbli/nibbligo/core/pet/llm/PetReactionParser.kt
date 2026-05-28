package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression

object PetReactionParser {
    private const val MAX_LEN = 160

    fun parse(raw: String): PetReaction {
        val trimmed = raw.trim().removeSurrounding("\"").trim()
        val dialogue = trimmed.lines().firstOrNull { it.isNotBlank() }?.take(MAX_LEN)
            ?: trimmed.take(MAX_LEN)
        return PetReaction(dialogue = dialogue.ifBlank { "…" })
    }

    fun fallback(request: PetReactionRequest): PetReaction {
        val line = request.lastAction?.let { "After $it, I'm glad you're here!" }
            ?: request.userMessage?.let { "Heard you! Still on-device with you." }
            ?: "Beep! I'm your pocket AI friend."
        return PetReaction(dialogue = line.take(MAX_LEN))
    }
}

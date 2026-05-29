package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression

object PetReactionParser {
    private const val MAX_LEN = 160

    fun parse(raw: String): PetReaction {
        val trimmed = raw.trim().removeSurrounding("\"").trim()
        val lines = trimmed.lines().filter { it.isNotBlank() }
        val primary = lines.firstOrNull() ?: trimmed
        val (dialogue, expression) = splitDialogueAndExpression(primary)
        val cleanDialogue = dialogue.take(MAX_LEN).ifBlank { "…" }
        return PetReaction(
            dialogue = cleanDialogue,
            suggestedExpression = expression,
        )
    }

    internal fun splitDialogueAndExpression(line: String): Pair<String, PetExpression?> {
        val pipe = line.lastIndexOf('|')
        if (pipe <= 0 || pipe >= line.length - 1) {
            return line.trim() to null
        }
        val text = line.substring(0, pipe).trim()
        val tag = line.substring(pipe + 1).trim().uppercase()
        val expression = runCatching { PetExpression.valueOf(tag) }.getOrNull()
        return text to expression
    }

    fun fallback(request: PetReactionRequest): PetReaction {
        val line = when {
            request.moodPulse -> PetMoodDescriber.templateLine(request.state)
            request.userMessage != null ->
                "Hmm, my on-device brain hiccuped — ask again in a sec?"
            request.lastAction != null -> "After ${request.lastAction}, I'm glad you're here!"
            else -> "Beep! I'm your pocket AI friend."
        }
        return PetReaction(dialogue = line.take(MAX_LEN))
    }
}

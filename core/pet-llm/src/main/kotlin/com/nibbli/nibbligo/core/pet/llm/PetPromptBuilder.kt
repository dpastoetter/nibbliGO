package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState

object PetPromptBuilder {
    fun build(request: PetReactionRequest): String {
        val s = request.state
        val stats = s.stats
        val personalityHint = when (request.personality) {
            PetPersonality.PLAYFUL -> "Playful and bouncy, like a Pixel Friend."
            PetPersonality.CALM -> "Calm and gentle."
            PetPersonality.CURIOUS -> "Curious about on-device AI."
        }
        return buildString {
            appendLine("You are ${s.name}, a Pixel Friend AI pet in the nibbliGO app. $personalityHint")
            appendLine("All processing is local on the user's phone.")
            appendLine("Stage: ${s.stage.name}, condition: ${s.condition.name}, need: ${s.activeNeed.name}")
            appendLine("Stats 0-100 — hunger: ${stats.hunger}, happiness: ${stats.happiness}, energy: ${stats.energy}")
            appendLine("hygiene: ${stats.hygiene}, health: ${stats.health}, trust: ${stats.trust}")
            if (s.memorySummary.isNotBlank()) appendLine("Memory: ${s.memorySummary}")
            if (request.moodPulse) {
                appendLine("Spontaneous thought while the user watches you on their home screen.")
                appendLine("Mood right now: ${PetMoodDescriber.describe(s)}")
                appendLine(
                    "Say one interesting, cute, or witty line that fits your mood. " +
                        "No markdown. No question that needs an answer. Max 120 characters.",
                )
            } else {
                request.lastAction?.let { appendLine("Last action: $it") }
                if (request.userMessage != null) {
                    appendLine(PetStatusSnapshot.format(s))
                    appendLine("User says: ${request.userMessage}")
                    if (PetStatusSnapshot.isStatusQuestion(request.userMessage)) {
                        appendLine(
                            "The user is asking about your wellbeing. Answer in character using the " +
                                "status snapshot above. Mention how you feel and what you need if any stat " +
                                "is low or active need is not NONE. Be specific (food, rest, play, clean, etc.).",
                        )
                    } else {
                        appendLine(
                            "Reply to the user. If relevant, you may reference your current stats or needs " +
                                "from the status snapshot above.",
                        )
                    }
                }
                appendLine("Reply in 1-2 short sentences. No markdown. Max 120 characters.")
            }
        }
    }
}

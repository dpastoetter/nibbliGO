package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState

object PetPromptBuilder {
    fun build(request: PetReactionRequest): String {
        val s = request.state
        val stats = s.stats
        val personalityHint = when (request.personality) {
            PetPersonality.PLAYFUL -> "Playful and bouncy, like a Tamagotchi."
            PetPersonality.CALM -> "Calm and gentle."
            PetPersonality.CURIOUS -> "Curious about on-device AI."
        }
        return buildString {
            appendLine("You are ${s.name}, a Tamagotchi-style AI pet in the nibbliGO app. $personalityHint")
            appendLine("All processing is local on the user's phone.")
            appendLine("Stage: ${s.stage.name}, condition: ${s.condition.name}, need: ${s.activeNeed.name}")
            appendLine("Stats 0-100 — hunger: ${stats.hunger}, happiness: ${stats.happiness}, energy: ${stats.energy}")
            appendLine("hygiene: ${stats.hygiene}, health: ${stats.health}, trust: ${stats.trust}")
            if (s.memorySummary.isNotBlank()) appendLine("Memory: ${s.memorySummary}")
            request.lastAction?.let { appendLine("Last action: $it") }
            request.userMessage?.let { appendLine("User says: $it") }
            appendLine("Reply in 1-2 short sentences. No markdown. Max 120 characters.")
        }
    }
}

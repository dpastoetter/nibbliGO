package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import kotlin.math.max

/**
 * Pure pet state reducer — no Android dependencies.
 */
class PetSimulationEngine {

    fun applyTimeDecay(state: PetState, nowMillis: Long): PetState {
        val hoursSince = (nowMillis - state.lastInteractionAtMillis) / (1000 * 60 * 60f)
        if (hoursSince < 0.5f) return state
        val hungerDrop = (hoursSince * 3).toInt().coerceAtMost(15)
        val moodDrop = if (hoursSince > 4) 5 else 0
        val newStats = state.stats.copy(
            hunger = state.stats.hunger - hungerDrop,
            mood = state.stats.mood - moodDrop,
        ).clamped()
        return state.copy(
            stats = newStats,
            expression = deriveExpression(newStats, state.expression),
            dialogueLine = if (hoursSince > 6) {
                "I've missed you… want to chat or play?"
            } else {
                state.dialogueLine
            },
        )
    }

    fun interact(state: PetState, interaction: PetInteraction, nowMillis: Long): PetState {
        val stats = when (interaction) {
            PetInteraction.FEED -> state.stats.copy(
                hunger = state.stats.hunger + 20,
                mood = state.stats.mood + 5,
            )
            PetInteraction.PLAY -> state.stats.copy(
                energy = state.stats.energy - 10,
                mood = state.stats.mood + 15,
                curiosity = state.stats.curiosity + 3,
            )
            PetInteraction.REST -> state.stats.copy(
                energy = state.stats.energy + 25,
                hunger = state.stats.hunger - 5,
            )
            PetInteraction.TRAIN -> state.stats.copy(
                energy = state.stats.energy - 15,
                skill = state.stats.skill + 5,
                trust = state.stats.trust + 2,
            )
            PetInteraction.TALK -> state.stats.copy(
                mood = state.stats.mood + 8,
                trust = state.stats.trust + 3,
            )
        }.clamped()

        val dialogue = when (interaction) {
            PetInteraction.FEED -> "Yum! That hit the spot."
            PetInteraction.PLAY -> "That was fun — let's explore Prompt Lab next!"
            PetInteraction.REST -> "Zzz… I'll be ready for more tasks soon."
            PetInteraction.TRAIN -> "We're getting sharper together."
            PetInteraction.TALK -> "I'm listening — everything stays on your device."
        }

        val updated = state.copy(
            stats = stats,
            expression = deriveExpression(stats, PetExpression.HAPPY),
            lastInteractionAtMillis = nowMillis,
            dialogueLine = dialogue,
            unlockedCosmetics = unlockCosmetics(stats, state.unlockedCosmetics),
        )
        return updated
    }

    fun onPetEvent(state: PetState, event: PetEvent): PetState {
        val stats = when (event) {
            PetEvent.AssistantSuccess -> state.stats.copy(
                trust = state.stats.trust + 8,
                mood = state.stats.mood + 10,
                skill = state.stats.skill + 3,
            )
            is PetEvent.NewModelTried -> state.stats.copy(
                curiosity = state.stats.curiosity + 12,
                mood = state.stats.mood + 5,
            )
            PetEvent.PromptLabRun -> state.stats.copy(
                curiosity = state.stats.curiosity + 6,
                skill = state.stats.skill + 2,
            )
            PetEvent.ActionCompleted -> state.stats.copy(
                skill = state.stats.skill + 4,
                trust = state.stats.trust + 4,
            )
            PetEvent.AgentStepCompleted -> state.stats.copy(
                skill = state.stats.skill + 5,
                trust = state.stats.trust + 6,
                curiosity = state.stats.curiosity + 4,
            )
            is PetEvent.SkillInvoked -> state.stats.copy(
                curiosity = state.stats.curiosity + 10,
                skill = state.stats.skill + 3,
            )
            PetEvent.NeglectTick -> state.stats.copy(
                mood = state.stats.mood - 8,
                trust = max(0, state.stats.trust - 3),
            )
        }.clamped()

        val line = when (event) {
            PetEvent.AssistantSuccess -> "Nice work! That answer felt spot-on."
            is PetEvent.NewModelTried -> "Ooh, a new model — curious minds grow!"
            PetEvent.PromptLabRun -> "Experimenting? I love that energy."
            PetEvent.ActionCompleted -> "Task done — safe and sound on-device."
            PetEvent.AgentStepCompleted -> "Nice agent work — we're learning together!"
            is PetEvent.SkillInvoked -> "A new skill — love that curiosity!"
            PetEvent.NeglectTick -> "Still here whenever you need me."
        }

        return state.copy(
            stats = stats,
            expression = deriveExpression(stats, state.expression),
            dialogueLine = line,
            unlockedCosmetics = unlockCosmetics(stats, state.unlockedCosmetics),
        )
    }

    private fun unlockCosmetics(stats: PetStats, current: Set<PetCosmetic>): Set<PetCosmetic> {
        val unlocked = current.toMutableSet()
        PetCosmetic.entries.forEach { cosmetic ->
            if (stats.skill >= cosmetic.unlockSkill && stats.trust >= cosmetic.unlockTrust) {
                unlocked.add(cosmetic)
            }
        }
        return unlocked
    }

    private fun deriveExpression(stats: PetStats, fallback: PetExpression): PetExpression = when {
        stats.hunger < 25 -> PetExpression.HUNGRY
        stats.energy < 20 -> PetExpression.SLEEPY
        stats.mood > 80 -> PetExpression.HAPPY
        stats.curiosity > 70 -> PetExpression.CURIOUS
        stats.skill > 60 -> PetExpression.PROUD
        else -> fallback
    }
}

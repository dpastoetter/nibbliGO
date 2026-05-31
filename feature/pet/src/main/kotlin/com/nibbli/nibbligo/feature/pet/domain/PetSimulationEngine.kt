package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetGameReference
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetInteractionResult
import com.nibbli.nibbligo.core.model.PetLcdItemUnlocks
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetNeedRules
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.core.model.PetTickResult
import kotlin.math.max
import kotlin.random.Random

/**
 * Pure Pixel Friend pet reducer — no Android dependencies.
 */
class PetSimulationEngine {

    private companion object {
        const val ACTION_ANIMATION_HOLD_MS = 4_500L
        val ACTION_HOLD_ANIMATIONS = setOf(
            PetAnimation.EAT,
            PetAnimation.PLAY,
            PetAnimation.HAPPY,
            PetAnimation.EVOLVE,
        )
    }

    fun applyTimeDecay(state: PetState, nowMillis: Long): PetState =
        tick(state, nowMillis).state

    fun tick(state: PetState, nowMillis: Long): PetTickResult {
        if (state.condition == PetCondition.DEAD) {
            return PetTickResult(state)
        }
        val minutesElapsed = ((nowMillis - state.lastTickAtMillis) / 60_000L).coerceAtLeast(0)
        if (minutesElapsed == 0L) {
            val need = PetNeedRules.deriveNeed(state, nowMillis)
            val refreshed = state.copy(
                activeNeed = need,
                expression = deriveExpression(state.stats, state.condition, need),
                animation = deriveAnimation(
                    condition = state.condition,
                    need = need,
                    current = state.animation,
                    lastInteractionAtMillis = state.lastInteractionAtMillis,
                    nowMillis = nowMillis,
                ),
            )
            return PetTickResult(refreshed)
        }

        var stats = state.stats
        val ticks = minutesElapsed.coerceAtMost(24 * 60).toInt()
        repeat(ticks) {
            if (state.condition == PetCondition.SLEEPING) {
                stats = stats.copy(energy = stats.energy + 1, hunger = stats.hunger - 1)
            } else {
                stats = stats.copy(
                    hunger = stats.hunger - 1,
                    hygiene = stats.hygiene - 1,
                    energy = stats.energy - 1,
                    mood = stats.mood - if (stats.hunger < 30) 1 else 0,
                )
            }
        }
        stats = stats.clamped()

        var hasMess = state.hasMess
        var condition = state.condition
        if (condition != PetCondition.SLEEPING &&
            stats.hygiene < PetGameReference.MESS_HYGIENE_THRESHOLD &&
            Random.nextFloat() < 0.08f * ticks
        ) {
            hasMess = true
        }
        if (hasMess && stats.hygiene < PetGameReference.SICK_HYGIENE_THRESHOLD && condition == PetCondition.HEALTHY) {
            condition = PetCondition.SICK
        }

        val ageMinutes = state.ageMinutes + minutesElapsed
        var careScore = state.careScore
        if (stats.hunger > PetGameReference.CARE_SCORE_TICK_THRESHOLD &&
            stats.mood > PetGameReference.CARE_SCORE_TICK_THRESHOLD &&
            stats.hygiene > PetGameReference.CARE_SCORE_TICK_THRESHOLD
        ) {
            careScore = (careScore + 1).coerceAtMost(100)
        }

        var criticalSince = state.criticalNeglectSinceMillis
        val critical = stats.hunger < PetGameReference.CRITICAL_HUNGER_THRESHOLD ||
            stats.health < PetGameReference.CRITICAL_HEALTH_THRESHOLD
        criticalSince = when {
            critical && criticalSince == null -> nowMillis
            !critical -> null
            else -> criticalSince
        }

        var updated = state.copy(
            stats = stats,
            hasMess = hasMess,
            condition = condition,
            ageMinutes = ageMinutes,
            lastTickAtMillis = nowMillis,
            careScore = careScore,
            criticalNeglectSinceMillis = criticalSince,
        )

        var evolved = false
        val evolution = PetGameReference.canEvolve(updated)
        if (evolution != null) {
            updated = updated.copy(
                stage = evolution,
                animation = PetAnimation.EVOLVE,
                expression = PetExpression.HAPPY,
                dialogueLine = evolutionLine(evolution),
                lastInteractionAtMillis = nowMillis,
            )
            evolved = true
            updated = applyLcdItemUnlocks(updated, updated.stats).state
        }

        if (criticalSince != null && nowMillis - criticalSince > PetGameReference.CRITICAL_DEATH_MS) {
            updated = updated.copy(
                condition = PetCondition.DEAD,
                expression = PetExpression.NEUTRAL,
                animation = PetAnimation.IDLE,
                dialogueLine = "nibbli faded away… tap to hatch a new egg.",
            )
        }

        val need = PetNeedRules.deriveNeed(updated, nowMillis)
        updated = updated.copy(
            activeNeed = need,
            expression = deriveExpression(updated.stats, updated.condition, need),
            animation = deriveAnimation(
                condition = updated.condition,
                need = need,
                current = updated.animation,
                lastInteractionAtMillis = updated.lastInteractionAtMillis,
                nowMillis = nowMillis,
            ),
        )
        updated = applyLcdItemUnlocks(updated, updated.stats).state

        val hoursAway = (nowMillis - state.lastInteractionAtMillis) / (1000 * 60 * 60f)
        val welcomeBack = hoursAway >= 6f
        val templateDialogue = when {
            welcomeBack -> "I've missed you! Everything still runs on-device here."
            hoursAway > 2 && need != PetNeed.NONE -> attentionLine(need)
            else -> null
        }

        return PetTickResult(
            state = updated,
            templateDialogue = templateDialogue,
            shouldNotifyAttention = need != PetNeed.NONE && need != PetNeed.LONELY,
            welcomeBack = welcomeBack,
            evolved = evolved,
        )
    }

    fun interact(state: PetState, interaction: PetInteraction, nowMillis: Long): PetInteractionResult {
        if (state.condition == PetCondition.DEAD) {
            return PetInteractionResult(state, state.dialogueLine)
        }

        var workingState = state
        if (state.condition == PetCondition.SLEEPING &&
            interaction != PetInteraction.SLEEP &&
            interaction != PetInteraction.WAKE
        ) {
            workingState = wakeFromSleep(workingState, nowMillis)
        }

        var stats = workingState.stats
        var condition = workingState.condition
        var hasMess = workingState.hasMess
        var animation = workingState.animation
        var careScore = (workingState.careScore + PetGameReference.CARE_SCORE_PER_INTERACTION).coerceAtMost(100)

        val templateDialogue = when (interaction) {
            PetInteraction.FEED_MEAL -> {
                if (stats.hunger > 85) {
                    stats = stats.copy(weight = stats.weight + 3, mood = stats.mood - 2)
                    "I'm stuffed! Maybe a snack later?"
                } else {
                    val boosted = stats.hunger + 22
                    val hunger = if (stats.hunger < PetGameReference.NEED_HUNGRY_THRESHOLD) {
                        boosted.coerceAtLeast(50)
                    } else {
                        boosted
                    }
                    stats = stats.copy(hunger = hunger, weight = stats.weight + 2, mood = stats.mood + 3)
                    animation = PetAnimation.EAT
                    "Yum! That meal hit the spot."
                }
            }
            PetInteraction.FEED_SNACK -> {
                stats = stats.copy(hunger = stats.hunger + 8, mood = stats.mood + 12)
                animation = PetAnimation.EAT
                "Treat time! So happy."
            }
            PetInteraction.PLAY -> {
                if (stats.hunger < PetGameReference.NEED_HUNGRY_THRESHOLD) {
                    stats = stats.copy(energy = stats.energy - 5)
                    "Too hungry to play… feed me first?"
                } else {
                    stats = stats.copy(energy = stats.energy - 12, mood = stats.mood + 18, curiosity = stats.curiosity + 4)
                    animation = PetAnimation.PLAY
                    "That was fun! Want to chat or play Catch next?"
                }
            }
            PetInteraction.CLEAN -> {
                if (!hasMess) {
                    stats = stats.copy(hygiene = stats.hygiene + 5)
                    "Already sparkly clean!"
                } else {
                    hasMess = false
                    stats = stats.copy(hygiene = stats.hygiene + 25, mood = stats.mood + 5)
                    if (condition == PetCondition.SICK && stats.health > 40) {
                        condition = PetCondition.HEALTHY
                    }
                    animation = PetAnimation.HAPPY
                    "Fresh and tidy! Thank you."
                }
            }
            PetInteraction.MEDICINE -> {
                if (condition != PetCondition.SICK) {
                    stats = stats.copy(trust = stats.trust - 3, health = stats.health - 5)
                    "I'm not sick… that tasted weird."
                } else {
                    stats = stats.copy(health = stats.health + 30, mood = stats.mood + 5)
                    condition = PetCondition.HEALTHY
                    animation = PetAnimation.HAPPY
                    "Feeling better! Thanks for the medicine."
                }
            }
            PetInteraction.SLEEP -> {
                condition = PetCondition.SLEEPING
                animation = PetAnimation.SLEEP
                stats = stats.copy(energy = stats.energy + 5)
                "Night-night… lights out."
            }
            PetInteraction.WAKE -> {
                if (condition == PetCondition.SLEEPING) {
                    condition = PetCondition.HEALTHY
                    stats = stats.copy(energy = stats.energy + 15)
                    animation = PetAnimation.HAPPY
                    "Good morning! Ready to help on-device."
                } else {
                    "I'm already awake and curious!"
                }
            }
            PetInteraction.TALK -> {
                stats = stats.copy(mood = stats.mood + 8, trust = stats.trust + 4)
                "I'm listening — everything stays on your device."
            }
            PetInteraction.TRAIN -> {
                stats = stats.copy(energy = stats.energy - 15, skill = stats.skill + 6, trust = stats.trust + 3)
                animation = PetAnimation.HAPPY
                "We're getting sharper together."
            }
            PetInteraction.GUIDE -> {
                stats = stats.copy(discipline = stats.discipline + 8, trust = stats.trust + 2)
                "Got it! I'll behave better."
            }
            PetInteraction.ITEMS -> {
                "Use ◀ to leave · ▶ browse · ● equip."
            }
        }

        stats = stats.clamped()
        val need = PetNeedRules.deriveNeed(
            workingState.copy(stats = stats, condition = condition, hasMess = hasMess),
            nowMillis,
        )
        val lcdUnlocks = applyLcdItemUnlocks(workingState, stats)
        val updated = workingState.copy(
            stats = stats,
            condition = condition,
            hasMess = hasMess,
            animation = animation,
            expression = deriveExpression(stats, condition, need),
            activeNeed = need,
            lastInteractionAtMillis = nowMillis,
            lastTickAtMillis = nowMillis,
            careScore = careScore,
            criticalNeglectSinceMillis = null,
            unlockedCosmetics = lcdUnlocks.unlockedCosmetics,
            equippedCosmetic = lcdUnlocks.equippedCosmetic,
            unlockedScenes = lcdUnlocks.unlockedScenes,
            unlockedProps = lcdUnlocks.unlockedProps,
        )
        return PetInteractionResult(updated, templateDialogue)
    }

    private fun wakeFromSleep(state: PetState, nowMillis: Long): PetState {
        if (state.condition != PetCondition.SLEEPING) return state
        val stats = state.stats.copy(energy = (state.stats.energy + 15).coerceAtMost(100))
        val condition = PetCondition.HEALTHY
        val need = PetNeedRules.deriveNeed(
            state.copy(stats = stats, condition = condition),
            nowMillis,
        )
        return state.copy(
            stats = stats,
            condition = condition,
            animation = PetAnimation.HAPPY,
            expression = deriveExpression(stats, condition, need),
            activeNeed = need,
            lastInteractionAtMillis = nowMillis,
            lastTickAtMillis = nowMillis,
        )
    }

    fun onPetEvent(state: PetState, event: PetEvent): PetState {
        if (state.condition == PetCondition.DEAD) return state

        var stats = state.stats
        var becameSick = false
        val line = when (event) {
            PetEvent.AssistantSuccess -> {
                stats = stats.copy(trust = stats.trust + 8, mood = stats.mood + 10, skill = stats.skill + 3)
                "Nice work! That answer felt spot-on."
            }
            is PetEvent.NewModelTried -> {
                stats = stats.copy(curiosity = stats.curiosity + 12, mood = stats.mood + 5)
                "Ooh, a new model — curious minds grow!"
            }
            PetEvent.PromptLabRun -> {
                stats = stats.copy(curiosity = stats.curiosity + 6, skill = stats.skill + 2)
                "Experimenting? I love that energy."
            }
            PetEvent.ActionCompleted -> {
                stats = stats.copy(skill = stats.skill + 4, trust = stats.trust + 4)
                "Task done — safe and sound on-device."
            }
            PetEvent.AgentStepCompleted -> {
                stats = stats.copy(skill = stats.skill + 5, trust = stats.trust + 6, curiosity = stats.curiosity + 4)
                "Nice chat — we're learning together!"
            }
            is PetEvent.SkillInvoked -> {
                stats = stats.copy(curiosity = stats.curiosity + 10, skill = stats.skill + 3)
                "A new skill — love that curiosity!"
            }
            PetEvent.NeglectTick -> {
                stats = stats.copy(mood = stats.mood - 8, trust = max(0, stats.trust - 3))
                "Still here whenever you need me."
            }
            is PetEvent.Evolution -> {
                "I evolved into ${event.stage.name.lowercase()}! What a day."
            }
            PetEvent.BecameSick -> {
                becameSick = true
                stats = stats.copy(health = stats.health - 10)
                "I don't feel so good…"
            }
        }

        stats = stats.clamped()
        var condition = state.condition
        if (becameSick) condition = PetCondition.SICK

        val need = PetNeedRules.deriveNeed(state.copy(stats = stats, condition = condition))
        val lcdUnlocks = applyLcdItemUnlocks(state, stats)
        return state.copy(
            stats = stats,
            condition = condition,
            expression = deriveExpression(stats, condition, need),
            activeNeed = need,
            dialogueLine = line,
            unlockedCosmetics = lcdUnlocks.unlockedCosmetics,
            equippedCosmetic = lcdUnlocks.equippedCosmetic,
            unlockedScenes = lcdUnlocks.unlockedScenes,
            unlockedProps = lcdUnlocks.unlockedProps,
        )
    }

    fun hatchNewEgg(previous: PetState?): PetState {
        val memory = previous?.memorySummary?.takeIf { it.isNotBlank() }
            ?: previous?.let { "Remembers you cared for ${it.name}." }
            ?: ""
        return PetState(
            name = "nibbli",
            stage = LifeStage.EGG,
            condition = PetCondition.HEALTHY,
            stats = PetStats(),
            expression = PetExpression.CURIOUS,
            animation = PetAnimation.IDLE,
            dialogueLine = "A new egg! Tap and talk to help me hatch.",
            memorySummary = memory,
            bornAtMillis = System.currentTimeMillis(),
            lastInteractionAtMillis = System.currentTimeMillis(),
            lastTickAtMillis = System.currentTimeMillis(),
        )
    }

    fun applyMinigameWin(state: PetState, nowMillis: Long): PetState {
        val stats = state.stats.copy(mood = state.stats.mood + 20, skill = state.stats.skill + 2).clamped()
        var updated = state.copy(
            stats = stats,
            animation = PetAnimation.HAPPY,
            expression = PetExpression.HAPPY,
            lastInteractionAtMillis = nowMillis,
            dialogueLine = "You win! Best caretaker ever.",
            unlockedProps = PetLcdItemUnlocks.grantNextProp(state.unlockedProps),
        )
        updated = applyLcdItemUnlocks(updated, stats).state
        return updated
    }

    fun applyLcdItemUnlocks(state: PetState, stats: PetStats): LcdItemUnlockResult {
        val unlockedCosmetics = PetLcdItemUnlocks.unlockCosmetics(stats, state.unlockedCosmetics)
        val newlyUnlockedCosmetics = unlockedCosmetics - state.unlockedCosmetics
        val equippedCosmetic = when {
            newlyUnlockedCosmetics.isNotEmpty() && state.equippedCosmetic == null ->
                newlyUnlockedCosmetics.maxBy { it.ordinal }
            else -> state.equippedCosmetic
        }
        val unlockedScenes = PetLcdItemUnlocks.unlockScenes(state)
        return LcdItemUnlockResult(
            state = state.copy(
                unlockedCosmetics = unlockedCosmetics,
                equippedCosmetic = equippedCosmetic,
                unlockedScenes = unlockedScenes,
            ),
            unlockedCosmetics = unlockedCosmetics,
            equippedCosmetic = equippedCosmetic,
            unlockedScenes = unlockedScenes,
            unlockedProps = state.unlockedProps,
            newlyUnlockedScenes = unlockedScenes - state.unlockedScenes,
            newlyUnlockedCosmetics = newlyUnlockedCosmetics,
            newlyUnlockedProps = emptySet(),
        )
    }

    fun grantPropUnlock(state: PetState): PetState {
        val props = PetLcdItemUnlocks.grantNextProp(state.unlockedProps)
        if (props == state.unlockedProps) return state
        return state.copy(unlockedProps = props)
    }

    private fun evolutionLine(stage: LifeStage): String = when (stage) {
        LifeStage.BABY -> "I hatched! Hi hi!"
        LifeStage.CHILD -> "Growing up so fast on your phone!"
        LifeStage.TEEN -> "Teen nibbli reporting for duty."
        LifeStage.ADULT -> "All grown up — ready for big on-device tasks!"
        LifeStage.EGG -> "…"
    }

    private fun deriveExpression(stats: PetStats, condition: PetCondition, need: PetNeed): PetExpression =
        when {
            condition == PetCondition.DEAD -> PetExpression.NEUTRAL
            condition == PetCondition.SICK || need == PetNeed.SICK -> PetExpression.SICK
            need == PetNeed.HUNGRY || stats.hunger < 25 -> PetExpression.HUNGRY
            condition == PetCondition.SLEEPING || stats.energy < 20 -> PetExpression.SLEEPY
            need == PetNeed.NONE && stats.mood > 80 -> PetExpression.HAPPY
            stats.curiosity > 70 -> PetExpression.CURIOUS
            stats.skill > 60 -> PetExpression.PROUD
            need != PetNeed.NONE -> PetExpression.ATTENTION
            else -> PetExpression.NEUTRAL
        }

    private fun deriveAnimation(
        condition: PetCondition,
        need: PetNeed,
        current: PetAnimation,
        lastInteractionAtMillis: Long,
        nowMillis: Long,
    ): PetAnimation {
        val withinActionHold =
            nowMillis - lastInteractionAtMillis < ACTION_ANIMATION_HOLD_MS &&
                current in ACTION_HOLD_ANIMATIONS
        return when {
            condition == PetCondition.SLEEPING -> PetAnimation.SLEEP
            condition == PetCondition.SICK -> PetAnimation.SICK
            withinActionHold -> current
            need != PetNeed.NONE -> PetAnimation.ATTENTION
            else -> PetAnimation.IDLE
        }
    }

    private fun attentionLine(need: PetNeed): String = when (need) {
        PetNeed.HUNGRY -> "My tummy is rumbling…"
        PetNeed.DIRTY -> "It's messy in here!"
        PetNeed.TIRED -> "So sleepy…"
        PetNeed.SICK -> "I need medicine!"
        PetNeed.UNHAPPY -> "I'm feeling blue."
        PetNeed.LONELY -> "Come say hi?"
        else -> "Beep! I need you!"
    }

    data class LcdItemUnlockResult(
        val state: PetState,
        val unlockedCosmetics: Set<PetCosmetic>,
        val equippedCosmetic: PetCosmetic?,
        val unlockedScenes: Set<PetLcdScene>,
        val unlockedProps: Set<PetLcdProp>,
        val newlyUnlockedCosmetics: Set<PetCosmetic> = emptySet(),
        val newlyUnlockedScenes: Set<PetLcdScene> = emptySet(),
        val newlyUnlockedProps: Set<PetLcdProp> = emptySet(),
    )
}

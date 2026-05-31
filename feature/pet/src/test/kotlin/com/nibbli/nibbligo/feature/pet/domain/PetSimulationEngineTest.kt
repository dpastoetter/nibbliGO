package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.core.model.equippedScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSimulationEngineTest {

    private val engine = PetSimulationEngine()

    @Test
    fun feed_meal_increases_hunger_and_mood() {
        val state = PetState(stats = PetStats(hunger = 40, mood = 50))
        val result = engine.interact(state, PetInteraction.FEED_MEAL, nowMillis = 1000L)
        assertTrue(result.state.stats.hunger > 40)
        assertTrue(result.state.stats.mood > 50)
    }

    @Test
    fun feed_meal_when_very_hungry_restores_to_satisfying_level() {
        val state = PetState(stats = PetStats(hunger = 8, mood = 50))
        val result = engine.interact(state, PetInteraction.FEED_MEAL, nowMillis = 1000L)
        assertTrue(result.state.stats.hunger >= 50)
        assertEquals(PetNeed.NONE, result.state.activeNeed)
    }

    @Test
    fun tick_before_feed_applies_elapsed_decay_then_meal_boost() {
        val state = PetState(
            stats = PetStats(hunger = 55),
            lastTickAtMillis = 0L,
            lastInteractionAtMillis = 0L,
        )
        val now = 25 * 60 * 1000L
        val ticked = engine.tick(state, now).state
        assertEquals(30, ticked.stats.hunger)
        val fed = engine.interact(ticked, PetInteraction.FEED_MEAL, now).state
        assertEquals(52, fed.stats.hunger)
    }

    @Test
    fun tick_reduces_hunger_over_time() {
        val state = PetState(
            stats = PetStats(hunger = 80),
            lastTickAtMillis = 0L,
            lastInteractionAtMillis = 0L,
        )
        val result = engine.tick(state, nowMillis = 3 * 60 * 60 * 1000L)
        assertTrue(result.state.stats.hunger < 80)
    }

    @Test
    fun medicine_only_when_sick() {
        val healthy = PetState(condition = PetCondition.HEALTHY, stats = PetStats(health = 90))
        val wrong = engine.interact(healthy, PetInteraction.MEDICINE, 1000L)
        assertTrue(wrong.state.stats.trust < healthy.stats.trust)

        val sick = PetState(condition = PetCondition.SICK, stats = PetStats(health = 30))
        val fixed = engine.interact(sick, PetInteraction.MEDICINE, 2000L)
        assertEquals(PetCondition.HEALTHY, fixed.state.condition)
    }

    @Test
    fun clean_clears_mess() {
        val messy = PetState(hasMess = true, stats = PetStats(hygiene = 20))
        val result = engine.interact(messy, PetInteraction.CLEAN, 1000L)
        assertTrue(!result.state.hasMess)
        assertTrue(result.state.stats.hygiene > 20)
    }

    @Test
    fun evolution_egg_to_baby() {
        val egg = PetState(stage = LifeStage.EGG, ageMinutes = 0, careScore = 60, lastTickAtMillis = 0L)
        val result = engine.tick(egg, nowMillis = 40 * 60 * 1000L)
        assertEquals(LifeStage.BABY, result.state.stage)
        assertTrue(result.evolved)
    }

    @Test
    fun assistant_success_boosts_trust_and_skill() {
        val state = PetState(stats = PetStats(trust = 50, skill = 10))
        val result = engine.onPetEvent(state, PetEvent.AssistantSuccess)
        assertTrue(result.stats.trust > 50)
        assertTrue(result.stats.skill > 10)
    }

    @Test
    fun unlocks_cosmetic_when_thresholds_met() {
        val state = PetState(stats = PetStats(skill = 50, trust = 60))
        val result = engine.onPetEvent(state, PetEvent.ActionCompleted)
        assertTrue(result.unlockedCosmetics.contains(PetCosmetic.SPARKLE_COLLAR))
    }

    @Test
    fun auto_equips_highest_tier_on_first_unlock() {
        val state = PetState(stats = PetStats(skill = 14, trust = 19))
        val result = engine.onPetEvent(state, PetEvent.ActionCompleted)
        assertTrue(result.unlockedCosmetics.contains(PetCosmetic.SPARKLE_COLLAR))
        assertEquals(PetCosmetic.SPARKLE_COLLAR, result.equippedCosmetic)
    }

    @Test
    fun does_not_override_existing_equipped_cosmetic_on_new_unlock() {
        val state = PetState(
            stats = PetStats(skill = 50, trust = 60),
            equippedCosmetic = PetCosmetic.SPARKLE_COLLAR,
            unlockedCosmetics = setOf(PetCosmetic.SPARKLE_COLLAR),
        )
        val result = engine.onPetEvent(
            state.copy(stats = PetStats(skill = 55, trust = 65)),
            PetEvent.ActionCompleted,
        )
        assertEquals(PetCosmetic.SPARKLE_COLLAR, result.equippedCosmetic)
    }

    @Test
    fun hungry_need_when_hunger_low() {
        val state = PetState(stats = PetStats(hunger = 10, energy = 80, mood = 50))
        val result = engine.interact(state, PetInteraction.PLAY, 1000L)
        assertEquals(PetExpression.HUNGRY, result.state.expression)
        assertEquals(PetNeed.HUNGRY, result.state.activeNeed)
    }

    @Test
    fun hatch_new_egg_keeps_memory() {
        val old = PetState(memorySummary = "Loved prompt lab.", condition = PetCondition.DEAD)
        val egg = engine.hatchNewEgg(old)
        assertEquals(LifeStage.EGG, egg.stage)
        assertTrue(egg.memorySummary.contains("Loved"))
    }

    @Test
    fun eat_animation_holds_for_a_few_seconds_after_feed() {
        val state = PetState(
            stats = PetStats(hunger = 40),
            lastInteractionAtMillis = 0L,
            lastTickAtMillis = 0L,
        )
        val fed = engine.interact(state, PetInteraction.FEED_SNACK, nowMillis = 1_000L)
        assertEquals(PetAnimation.EAT, fed.state.animation)

        val tickSoon = engine.tick(fed.state, nowMillis = 2_000L)
        assertEquals(PetAnimation.EAT, tickSoon.state.animation)

        val tickLater = engine.tick(fed.state, nowMillis = 6_000L)
        assertEquals(PetAnimation.IDLE, tickLater.state.animation)
    }

    @Test
    fun play_animation_holds_for_a_few_seconds_after_play() {
        val state = PetState(
            stats = PetStats(hunger = 60, energy = 80),
            lastInteractionAtMillis = 0L,
            lastTickAtMillis = 0L,
        )
        val played = engine.interact(state, PetInteraction.PLAY, nowMillis = 1_000L)
        assertEquals(PetAnimation.PLAY, played.state.animation)

        val tickSoon = engine.tick(played.state, nowMillis = 2_000L)
        assertEquals(PetAnimation.PLAY, tickSoon.state.animation)

        val tickLater = engine.tick(played.state, nowMillis = 6_000L)
        assertEquals(PetAnimation.IDLE, tickLater.state.animation)
    }

    @Test
    fun feed_meal_while_sleeping_wakes_and_feeds() {
        val sleeping = PetState(
            condition = PetCondition.SLEEPING,
            stats = PetStats(hunger = 40, mood = 50),
        )
        val result = engine.interact(sleeping, PetInteraction.FEED_MEAL, 1000L)
        assertEquals(PetCondition.HEALTHY, result.state.condition)
        assertTrue(result.state.stats.hunger > 40)
        assertTrue(!result.templateDialogue.contains("Zzz"))
    }

    @Test
    fun play_while_sleeping_wakes_and_plays() {
        val sleeping = PetState(
            condition = PetCondition.SLEEPING,
            stats = PetStats(hunger = 60, energy = 80, mood = 50),
        )
        val result = engine.interact(sleeping, PetInteraction.PLAY, 1000L)
        assertEquals(PetCondition.HEALTHY, result.state.condition)
        assertEquals(PetAnimation.PLAY, result.state.animation)
    }

    @Test
    fun wake_while_sleeping_still_works() {
        val sleeping = PetState(condition = PetCondition.SLEEPING)
        val result = engine.interact(sleeping, PetInteraction.WAKE, 1000L)
        assertEquals(PetCondition.HEALTHY, result.state.condition)
        assertTrue(result.templateDialogue.contains("Good morning"))
    }

    @Test
    fun sleep_while_already_sleeping_stays_asleep() {
        val sleeping = PetState(condition = PetCondition.SLEEPING)
        val result = engine.interact(sleeping, PetInteraction.SLEEP, 1000L)
        assertEquals(PetCondition.SLEEPING, result.state.condition)
    }

    @Test
    fun unlocks_stars_scene_when_care_score_high_enough() {
        val state = PetState(careScore = 65, stats = PetStats())
        val unlocks = engine.applyLcdItemUnlocks(state, state.stats)
        assertTrue(unlocks.unlockedScenes.contains(PetLcdScene.STARS))
    }

    @Test
    fun minigame_win_grants_prop_unlock() {
        val state = PetState(stats = PetStats(mood = 50))
        val result = engine.applyMinigameWin(state, nowMillis = 1000L)
        assertTrue(result.unlockedProps.contains(PetLcdProp.BALL))
    }
}

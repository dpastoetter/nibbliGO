package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSimulationEngineTest {

    private val engine = PetSimulationEngine()

    @Test
    fun feed_increases_hunger_and_mood() {
        val state = PetState(stats = PetStats(hunger = 40, mood = 50))
        val result = engine.interact(state, PetInteraction.FEED, nowMillis = 1000L)
        assertTrue(result.stats.hunger > 40)
        assertTrue(result.stats.mood > 50)
    }

    @Test
    fun time_decay_reduces_hunger_after_hours() {
        val state = PetState(
            stats = PetStats(hunger = 80),
            lastInteractionAtMillis = 0L,
        )
        val result = engine.applyTimeDecay(state, nowMillis = 5 * 60 * 60 * 1000L)
        assertTrue(result.stats.hunger < 80)
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
    fun neglect_tick_lowers_mood() {
        val state = PetState(stats = PetStats(mood = 70))
        val result = engine.onPetEvent(state, PetEvent.NeglectTick)
        assertTrue(result.stats.mood < 70)
    }

    @Test
    fun hungry_expression_when_hunger_low() {
        val state = PetState(stats = PetStats(hunger = 10, energy = 80, mood = 50))
        val result = engine.interact(state, PetInteraction.PLAY, 1000L)
        assertEquals(PetExpression.HUNGRY, result.expression)
    }
}

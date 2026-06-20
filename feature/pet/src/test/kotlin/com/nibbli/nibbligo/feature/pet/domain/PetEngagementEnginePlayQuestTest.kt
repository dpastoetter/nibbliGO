package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEngagementRules
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetEngagementEnginePlayQuestTest {

    private val engine = PetSimulationEngine()
    private val now = System.currentTimeMillis()
    private val today = PetEngagementRules.dayEpoch(now)

    @Test
    fun recordCatchGame_marksPlayQuestWithoutWin() {
        val base = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
            engagement = com.nibbli.nibbligo.core.model.PetEngagement(
                dailyQuestDayEpoch = today,
            ),
        )
        val after = PetEngagementEngine.recordCatchGame(
            state = base,
            score = 5,
            bestCombo = 1,
            won = false,
            nowMillis = now,
        )
        assertTrue(after.engagement.dailyQuestPlay)
    }

    @Test
    fun recordCatchGame_dailyTargetComplete_boostsMood() {
        val target = PetEngagementRules.catchTargetForDay(today)
        val base = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
            stats = com.nibbli.nibbligo.core.model.PetStats(mood = 40),
            engagement = com.nibbli.nibbligo.core.model.PetEngagement(
                dailyCatchDayEpoch = today,
                dailyCatchTargetScore = target,
            ),
        )
        val after = PetEngagementEngine.recordCatchGame(
            state = base,
            score = target,
            bestCombo = 3,
            won = false,
            nowMillis = now,
        )
        assertTrue(after.engagement.dailyCatchChallengeCompleted)
        assertEquals(43, after.stats.mood)
    }

    @Test
    fun questBonus_reappliesCosmeticUnlocksAfterTrustBump() {
        val base = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
            stats = PetStats(skill = 15, trust = 15),
            engagement = com.nibbli.nibbligo.core.model.PetEngagement(
                dailyQuestDayEpoch = today,
                dailyQuestFeed = true,
                dailyQuestPlay = true,
            ),
        )
        val interacted = engine.interact(base, PetInteraction.TALK, now).state
        assertFalse(PetCosmetic.SPARKLE_COLLAR in interacted.unlockedCosmetics)
        val after = PetEngagementEngine.recordTalk(interacted, now)
        assertTrue(after.engagement.dailyQuestBonusClaimed)
        assertTrue(PetCosmetic.SPARKLE_COLLAR in after.unlockedCosmetics)
        assertEquals(PetCosmetic.SPARKLE_COLLAR, after.equippedCosmetic)
    }

    @Test
    fun questBonus_grantsNextFloorProp() {
        val base = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
            engagement = com.nibbli.nibbligo.core.model.PetEngagement(
                dailyQuestDayEpoch = today,
                dailyQuestFeed = true,
                dailyQuestPlay = true,
            ),
        )
        val interacted = engine.interact(base, PetInteraction.TALK, now).state
        val after = PetEngagementEngine.recordTalk(interacted, now)
        assertTrue(PetLcdProp.BALL in after.unlockedProps)
        assertEquals(PetLcdProp.BALL, after.equippedProp)
    }

    @Test
    fun recordTalk_marksDailyQuestTalk() {
        val base = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
            engagement = com.nibbli.nibbligo.core.model.PetEngagement(
                dailyQuestDayEpoch = today,
            ),
        )
        val interacted = engine.interact(base, PetInteraction.TALK, now).state
        val after = PetEngagementEngine.recordTalk(interacted, now)
        assertTrue(after.engagement.dailyQuestTalk)
        assertTrue(after.stats.trust > base.stats.trust)
    }
}

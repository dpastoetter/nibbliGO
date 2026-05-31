package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.PetEngagement
import com.nibbli.nibbligo.core.model.PetEngagementRules
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetEngagementEngineTest {

    @Test
    fun recordCareInteraction_increments_streak_on_new_day() {
        val day = PetEngagementRules.dayEpoch(NOW)
        val state = PetState(
            engagement = PetEngagement(lastCareDayEpoch = day - 1, careStreakDays = 2),
        )
        val updated = PetEngagementEngine.recordCareInteraction(state, PetInteraction.FEED_MEAL, NOW)
        assertEquals(3, updated.engagement.careStreakDays)
        assertEquals(day, updated.engagement.lastCareDayEpoch)
        assertTrue(updated.engagement.dailyQuestFeed)
    }

    @Test
    fun recordCatchGame_updates_high_score_and_daily_challenge() {
        val day = PetEngagementRules.dayEpoch(NOW)
        val state = PetState(
            engagement = PetEngagement(
                dailyCatchDayEpoch = day,
                dailyCatchTargetScore = 25,
            ),
        )
        val updated = PetEngagementEngine.recordCatchGame(state, score = 30, bestCombo = 4, won = true, NOW)
        assertEquals(30, updated.engagement.catchHighScore)
        assertEquals(4, updated.engagement.catchBestCombo)
        assertTrue(updated.engagement.dailyCatchChallengeCompleted)
    }

    @Test
    fun onSessionOpen_rotates_daily_catch_target() {
        val state = PetState(
            engagement = PetEngagement(dailyCatchDayEpoch = 0),
        )
        val updated = PetEngagementEngine.onSessionOpen(state, NOW)
        assertEquals(PetEngagementRules.dayEpoch(NOW), updated.engagement.dailyCatchDayEpoch)
        assertTrue(updated.engagement.dailyCatchTargetScore in intArrayOf(22, 25, 28, 30))
    }

    companion object {
        private const val NOW = 1_700_000_000_000L
    }
}

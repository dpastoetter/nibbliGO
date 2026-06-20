package com.nibbli.nibbligo.feature.pet.ui

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEngagement
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetCelebrationDetectorTest {

    @Test
    fun detect_noEventsWhenNothingChanged() {
        val state = PetState()
        assertTrue(PetCelebrationDetector.detect(state, state).isEmpty())
    }

    @Test
    fun detect_cosmeticUnlock() {
        val before = PetState(unlockedCosmetics = emptySet())
        val after = before.copy(unlockedCosmetics = setOf(PetCosmetic.SPARKLE_COLLAR))
        val events = PetCelebrationDetector.detect(before, after)
        assertEquals(1, events.size)
        assertTrue(events[0] is PetCelebrationEvent.ItemUnlocked)
    }

    @Test
    fun detect_dailyQuestComplete() {
        val before = PetState(
            engagement = PetEngagement(
                dailyQuestFeed = true,
                dailyQuestPlay = true,
                dailyQuestTalk = false,
                dailyQuestBonusClaimed = false,
            ),
        )
        val after = before.copy(
            engagement = before.engagement.copy(
                dailyQuestTalk = true,
                dailyQuestBonusClaimed = true,
            ),
        )
        val events = PetCelebrationDetector.detect(before, after)
        assertTrue(events.any { it is PetCelebrationEvent.DailyQuestComplete })
    }

    @Test
    fun detect_sceneAndPropUnlocks() {
        val before = PetState()
        val after = before.copy(
            unlockedScenes = setOf(PetLcdScene.STARS),
            unlockedProps = setOf(PetLcdProp.BALL),
        )
        val events = PetCelebrationDetector.detect(before, after)
        assertEquals(2, events.size)
        assertTrue(events.all { it is PetCelebrationEvent.ItemUnlocked })
    }

    @Test
    fun detect_doesNotFireQuestCompleteWhenAlreadyClaimed() {
        val engagement = PetEngagement(
            dailyQuestFeed = true,
            dailyQuestPlay = true,
            dailyQuestTalk = true,
            dailyQuestBonusClaimed = true,
        )
        val before = PetState(engagement = engagement)
        val after = before.copy(stats = before.stats.copy(mood = before.stats.mood + 1))
        val events = PetCelebrationDetector.detect(before, after)
        assertTrue(events.none { it is PetCelebrationEvent.DailyQuestComplete })
    }
}

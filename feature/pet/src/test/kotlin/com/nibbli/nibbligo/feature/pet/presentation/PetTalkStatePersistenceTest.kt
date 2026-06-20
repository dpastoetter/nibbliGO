package com.nibbli.nibbligo.feature.pet.presentation

import com.nibbli.nibbligo.core.model.PetEngagementRules
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.domain.PetEngagementEngine
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: talk path must persist interaction + engagement, not only dialogue. */
class PetTalkStatePersistenceTest {

    private val simulationEngine = PetSimulationEngine()
    private val now = System.currentTimeMillis()

    @Test
    fun talkCompletingDailyQuest_unlocksFloorPropInPipelineState() {
        val before = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
            engagement = com.nibbli.nibbligo.core.model.PetEngagement(
                dailyQuestDayEpoch = PetEngagementRules.dayEpoch(now),
                dailyQuestFeed = true,
                dailyQuestPlay = true,
            ),
        )
        val tick = simulationEngine.tick(before, now)
        val interacted = simulationEngine.interact(tick.state, PetInteraction.TALK, now).state
        val afterTalk = PetEngagementEngine.recordTalk(interacted, now)
        assertTrue(afterTalk.engagement.dailyQuestBonusClaimed)
        assertTrue(PetLcdProp.BALL in afterTalk.unlockedProps)
    }

    @Test
    fun talkPipeline_persistsMoodTrustAndQuest() {
        val before = PetState(
            lastTickAtMillis = now,
            lastInteractionAtMillis = now,
            stats = com.nibbli.nibbligo.core.model.PetStats(mood = 50, trust = 10),
            engagement = com.nibbli.nibbligo.core.model.PetEngagement(
                dailyQuestDayEpoch = PetEngagementRules.dayEpoch(now),
            ),
        )
        val tick = simulationEngine.tick(before, now)
        val interacted = simulationEngine.interact(tick.state, PetInteraction.TALK, now).state
        val afterTalk = PetEngagementEngine.recordTalk(interacted, now)
        val persisted = afterTalk.copy(
            dialogueLine = "Hi caretaker!",
            expression = afterTalk.expression,
        )
        assertTrue(persisted.stats.mood > before.stats.mood)
        assertTrue(persisted.stats.trust > before.stats.trust)
        assertTrue(persisted.engagement.dailyQuestTalk)
        assertEquals("Hi caretaker!", persisted.dialogueLine)
    }
}

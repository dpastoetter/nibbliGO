package com.nibbli.nibbligo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetLcdItemUnlocksTest {

    @Test
    fun unlockScenes_requiresStageForCloudsAndNight() {
        val childHighCare = PetState(stage = LifeStage.CHILD, careScore = 50)
        assertFalse(PetLcdScene.CLOUDS.meetsUnlockThresholds(childHighCare))

        val teenReady = PetState(stage = LifeStage.TEEN, careScore = 45)
        assertTrue(PetLcdScene.CLOUDS.meetsUnlockThresholds(teenReady))

        val childForNight = PetState(stage = LifeStage.CHILD, careScore = 80)
        assertFalse(PetLcdScene.NIGHT.meetsUnlockThresholds(childForNight))

        val adultReady = PetState(stage = LifeStage.ADULT, careScore = 75)
        assertTrue(PetLcdScene.NIGHT.meetsUnlockThresholds(adultReady))
    }

    @Test
    fun unlockCosmetics_requiresBothSkillAndTrust() {
        val stats = PetStats(skill = 30, trust = 39)
        val unlocked = PetLcdItemUnlocks.unlockCosmetics(stats, emptySet())
        assertFalse(unlocked.contains(PetCosmetic.STAR_PATCH))

        val ready = PetStats(skill = 30, trust = 40)
        val withPatch = PetLcdItemUnlocks.unlockCosmetics(ready, emptySet())
        assertTrue(withPatch.contains(PetCosmetic.STAR_PATCH))
    }

    @Test
    fun grantNextProp_followsBallPlantBlanketOrder() {
        assertEquals(
            setOf(PetLcdProp.BALL),
            PetLcdItemUnlocks.grantNextProp(emptySet()),
        )
        assertEquals(
            setOf(PetLcdProp.BALL, PetLcdProp.PLANT),
            PetLcdItemUnlocks.grantNextProp(setOf(PetLcdProp.BALL)),
        )
        assertEquals(
            setOf(PetLcdProp.BALL, PetLcdProp.PLANT, PetLcdProp.BLANKET),
            PetLcdItemUnlocks.grantNextProp(setOf(PetLcdProp.BALL, PetLcdProp.PLANT)),
        )
        assertEquals(
            setOf(PetLcdProp.BALL, PetLcdProp.PLANT, PetLcdProp.BLANKET),
            PetLcdItemUnlocks.grantNextProp(setOf(PetLcdProp.BALL, PetLcdProp.PLANT, PetLcdProp.BLANKET)),
        )
    }
}

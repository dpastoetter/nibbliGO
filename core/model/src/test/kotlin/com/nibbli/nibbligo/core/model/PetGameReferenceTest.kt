package com.nibbli.nibbligo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetGameReferenceTest {

    @Test
    fun canEvolve_egg_to_baby_by_age_or_care() {
        val byAge = PetState(stage = LifeStage.EGG, ageMinutes = 30, careScore = 0)
        assertEquals(LifeStage.BABY, PetGameReference.canEvolve(byAge))

        val byCare = PetState(stage = LifeStage.EGG, ageMinutes = 0, careScore = 55)
        assertEquals(LifeStage.BABY, PetGameReference.canEvolve(byCare))

        val notYet = PetState(stage = LifeStage.EGG, ageMinutes = 10, careScore = 10)
        assertNull(PetGameReference.canEvolve(notYet))
    }

    @Test
    fun canEvolve_baby_requires_age_and_care() {
        val ready = PetState(stage = LifeStage.BABY, ageMinutes = 24 * 60, careScore = 45)
        assertEquals(LifeStage.CHILD, PetGameReference.canEvolve(ready))

        val young = PetState(stage = LifeStage.BABY, ageMinutes = 60, careScore = 80)
        assertNull(PetGameReference.canEvolve(young))
    }
}

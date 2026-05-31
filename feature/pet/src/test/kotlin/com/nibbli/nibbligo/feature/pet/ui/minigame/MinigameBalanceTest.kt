package com.nibbli.nibbligo.feature.pet.ui.minigame

import org.junit.Assert.assertEquals
import org.junit.Test

class MinigameBalanceTest {

    @Test
    fun effectiveDailyTarget_scales_per_game() {
        assertEquals(25, MinigameBalance.effectiveDailyTarget(PetMinigameId.SNACK_DROP, 25))
        assertEquals(15, MinigameBalance.effectiveDailyTarget(PetMinigameId.TIDY_TAP, 25))
    }

    @Test
    fun baseWinScore_matches_design() {
        assertEquals(18, MinigameBalance.baseWinScore(PetMinigameId.SNACK_DROP))
        assertEquals(16, MinigameBalance.baseWinScore(PetMinigameId.TIDY_TAP))
    }
}

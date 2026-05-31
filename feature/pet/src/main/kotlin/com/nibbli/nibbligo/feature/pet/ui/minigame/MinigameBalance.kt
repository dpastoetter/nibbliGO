package com.nibbli.nibbligo.feature.pet.ui.minigame

object MinigameBalance {
    fun baseWinScore(game: PetMinigameId): Int = when (game) {
        PetMinigameId.SNACK_DROP -> 18
        PetMinigameId.TIDY_TAP -> 16
    }

    fun effectiveDailyTarget(game: PetMinigameId, dailyTarget: Int): Int = when (game) {
        PetMinigameId.SNACK_DROP -> dailyTarget
        PetMinigameId.TIDY_TAP -> minOf(dailyTarget, 15)
    }

    fun durationMs(game: PetMinigameId): Long = when (game) {
        PetMinigameId.SNACK_DROP -> 28_000L
        PetMinigameId.TIDY_TAP -> 24_000L
    }
}

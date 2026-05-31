package com.nibbli.nibbligo.core.model

/** Daily streaks, Catch records, and challenge state — local-only engagement data. */
data class PetEngagement(
    val careStreakDays: Int = 0,
    val lastCareDayEpoch: Int = 0,
    val dailyQuestDayEpoch: Int = 0,
    val dailyQuestFeed: Boolean = false,
    val dailyQuestPlay: Boolean = false,
    val dailyQuestTalk: Boolean = false,
    val dailyQuestBonusClaimed: Boolean = false,
    val catchHighScore: Int = 0,
    val catchBestCombo: Int = 0,
    val dailyCatchTargetScore: Int = PetEngagementRules.DEFAULT_CATCH_TARGET,
    val dailyCatchDayEpoch: Int = 0,
    val dailyCatchChallengeCompleted: Boolean = false,
    val friendCode: String = "",
)

object PetEngagementRules {
    const val DEFAULT_CATCH_TARGET = 25
    private val CATCH_TARGETS = intArrayOf(22, 25, 28, 30)

    fun dayEpoch(nowMillis: Long): Int =
        (nowMillis / MILLIS_PER_DAY).toInt()

    fun catchTargetForDay(dayEpoch: Int): Int =
        CATCH_TARGETS[dayEpoch.mod(CATCH_TARGETS.size)]

    fun dailyQuestComplete(engagement: PetEngagement): Boolean =
        engagement.dailyQuestFeed && engagement.dailyQuestPlay && engagement.dailyQuestTalk

    private const val MILLIS_PER_DAY = 86_400_000L
}

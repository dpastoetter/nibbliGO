package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.PetEngagement
import com.nibbli.nibbligo.core.model.PetEngagementRules
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetLcdItemUnlocks
import com.nibbli.nibbligo.core.model.PetState
import java.security.MessageDigest

object PetEngagementEngine {

    fun onSessionOpen(state: PetState, nowMillis: Long): PetState {
        var engagement = refreshDailyCatchChallenge(state.engagement, nowMillis)
        engagement = reconcileStreak(engagement, nowMillis)
        engagement = refreshDailyQuest(engagement, nowMillis)
        val friendCode = engagement.friendCode.ifBlank { computeFriendCode(state) }
        return state.copy(engagement = engagement.copy(friendCode = friendCode))
    }

    fun recordCareInteraction(state: PetState, interaction: PetInteraction, nowMillis: Long): PetState {
        if (!state.isAlive) return state
        val today = PetEngagementRules.dayEpoch(nowMillis)
        var engagement = refreshDailyCatchChallenge(state.engagement, nowMillis)
        engagement = bumpCareStreak(engagement, today)
        engagement = markQuestProgress(engagement, interaction, today)
        var updated = state.copy(engagement = engagement)
        if (PetEngagementRules.dailyQuestComplete(engagement)) {
            updated = applyQuestBonusIfNew(updated, today)
        }
        return updated
    }

    fun recordTalk(state: PetState, nowMillis: Long): PetState =
        recordCareInteraction(state, PetInteraction.TALK, nowMillis)

    fun recordCatchGame(
        state: PetState,
        score: Int,
        bestCombo: Int,
        won: Boolean,
        nowMillis: Long,
    ): PetState {
        if (!state.isAlive) return state
        val today = PetEngagementRules.dayEpoch(nowMillis)
        var engagement = refreshDailyCatchChallenge(state.engagement, nowMillis)
        engagement = engagement.copy(
            catchHighScore = maxOf(engagement.catchHighScore, score),
            catchBestCombo = maxOf(engagement.catchBestCombo, bestCombo),
        )
        if (score >= engagement.dailyCatchTargetScore && !engagement.dailyCatchChallengeCompleted) {
            engagement = engagement.copy(dailyCatchChallengeCompleted = true)
        }
        engagement = markQuestProgress(engagement, PetInteraction.PLAY, today)
        var updated = state.copy(engagement = engagement)
        if (engagement.dailyCatchChallengeCompleted && !state.engagement.dailyCatchChallengeCompleted) {
            updated = updated.copy(
                stats = updated.stats.copy(mood = (updated.stats.mood + 3).coerceAtMost(100)).clamped(),
            )
        }
        if (PetEngagementRules.dailyQuestComplete(engagement)) {
            updated = applyQuestBonusIfNew(updated, today)
        }
        return updated
    }

    fun isStreakAtRisk(state: PetState, nowMillis: Long): Boolean {
        val today = PetEngagementRules.dayEpoch(nowMillis)
        val last = state.engagement.lastCareDayEpoch
        return state.engagement.careStreakDays > 0 && last > 0 && last < today
    }

    fun computeFriendCode(state: PetState): String {
        val raw = "${state.name}|${state.stage}|${state.careScore}|${state.bornAtMillis}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.take(4).joinToString("") { "%02X".format(it) }
    }

    private fun refreshDailyCatchChallenge(engagement: PetEngagement, nowMillis: Long): PetEngagement {
        val today = PetEngagementRules.dayEpoch(nowMillis)
        if (engagement.dailyCatchDayEpoch == today) return engagement
        return engagement.copy(
            dailyCatchDayEpoch = today,
            dailyCatchTargetScore = PetEngagementRules.catchTargetForDay(today),
            dailyCatchChallengeCompleted = false,
        )
    }

    private fun refreshDailyQuest(engagement: PetEngagement, nowMillis: Long): PetEngagement =
        refreshDailyQuestForDay(engagement, PetEngagementRules.dayEpoch(nowMillis))

    private fun refreshDailyQuestForDay(engagement: PetEngagement, today: Int): PetEngagement {
        if (engagement.dailyQuestDayEpoch == today) return engagement
        return engagement.copy(
            dailyQuestDayEpoch = today,
            dailyQuestFeed = false,
            dailyQuestPlay = false,
            dailyQuestTalk = false,
            dailyQuestBonusClaimed = false,
        )
    }

    private fun reconcileStreak(engagement: PetEngagement, nowMillis: Long): PetEngagement {
        val today = PetEngagementRules.dayEpoch(nowMillis)
        val last = engagement.lastCareDayEpoch
        if (last == 0 || today <= last + 1) return engagement
        return engagement.copy(careStreakDays = 0)
    }

    private fun bumpCareStreak(engagement: PetEngagement, today: Int): PetEngagement {
        val last = engagement.lastCareDayEpoch
        val streak = when {
            last == today -> engagement.careStreakDays
            last == today - 1 -> engagement.careStreakDays + 1
            else -> 1
        }
        return engagement.copy(
            careStreakDays = streak.coerceAtLeast(1),
            lastCareDayEpoch = today,
        )
    }

    private fun markQuestProgress(
        engagement: PetEngagement,
        interaction: PetInteraction,
        today: Int,
    ): PetEngagement {
        var e = refreshDailyQuestForDay(engagement, today)
        e = when (interaction) {
            PetInteraction.FEED_MEAL, PetInteraction.FEED_SNACK ->
                e.copy(dailyQuestFeed = true)
            PetInteraction.PLAY -> e.copy(dailyQuestPlay = true)
            PetInteraction.TALK -> e.copy(dailyQuestTalk = true)
            else -> e
        }
        return e.copy(dailyQuestDayEpoch = today)
    }

    private fun applyQuestBonusIfNew(state: PetState, today: Int): PetState {
        val engagement = state.engagement
        if (engagement.dailyQuestBonusClaimed || engagement.dailyQuestDayEpoch != today) {
            return state
        }
        val stats = state.stats.copy(
            mood = (state.stats.mood + 5).coerceAtMost(100),
            trust = (state.stats.trust + 3).coerceAtMost(100),
        )
        return state.copy(
            stats = stats.clamped(),
            careScore = (state.careScore + 1).coerceAtMost(100),
            unlockedProps = PetLcdItemUnlocks.grantNextProp(state.unlockedProps),
            engagement = engagement.copy(dailyQuestBonusClaimed = true),
        )
    }
}

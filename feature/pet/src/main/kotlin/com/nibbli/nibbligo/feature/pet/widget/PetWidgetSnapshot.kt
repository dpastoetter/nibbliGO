package com.nibbli.nibbligo.feature.pet.widget

import android.content.Context
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.share.stageGlyph

object PetWidgetSnapshot {
    private const val PREFS = "pet_widget_snapshot"

    fun write(context: Context, state: PetState) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, state.name)
            .putString(KEY_STAGE, state.stage.name)
            .putString(KEY_NEED, state.activeNeed.name)
            .putString(KEY_GLYPH, stageGlyph(state.stage))
            .putString(KEY_COSMETIC, state.equippedCosmetic?.name ?: "")
            .putInt(KEY_STREAK, state.engagement.careStreakDays)
            .putInt(KEY_MOOD, state.stats.mood)
            .putInt(
                KEY_QUEST_DONE,
                listOf(
                    state.engagement.dailyQuestFeed,
                    state.engagement.dailyQuestPlay,
                    state.engagement.dailyQuestTalk,
                ).count { it },
            )
            .apply()
    }

    fun read(context: Context): Snapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Snapshot(
            name = prefs.getString(KEY_NAME, "Nibbl") ?: "Nibbl",
            stage = prefs.getString(KEY_STAGE, "EGG") ?: "EGG",
            need = prefs.getString(KEY_NEED, "NONE") ?: "NONE",
            glyph = prefs.getString(KEY_GLYPH, "(o)") ?: "(o)",
            cosmetic = prefs.getString(KEY_COSMETIC, "") ?: "",
            streakDays = prefs.getInt(KEY_STREAK, 0),
            mood = prefs.getInt(KEY_MOOD, 0),
            questStepsDone = prefs.getInt(KEY_QUEST_DONE, 0),
        )
    }

    data class Snapshot(
        val name: String,
        val stage: String,
        val need: String,
        val glyph: String,
        val cosmetic: String,
        val streakDays: Int,
        val mood: Int,
        val questStepsDone: Int,
    )

    private const val KEY_NAME = "name"
    private const val KEY_STAGE = "stage"
    private const val KEY_NEED = "need"
    private const val KEY_GLYPH = "glyph"
    private const val KEY_COSMETIC = "cosmetic"
    private const val KEY_STREAK = "streak"
    private const val KEY_MOOD = "mood"
    private const val KEY_QUEST_DONE = "quest_done"
}

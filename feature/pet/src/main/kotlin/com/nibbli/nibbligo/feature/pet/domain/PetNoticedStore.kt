package com.nibbli.nibbligo.feature.pet.domain

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

private val Context.petNoticedDataStore: DataStore<Preferences> by preferencesDataStore("pet_noticed")

/** Local-only daily log of what nibbli noticed (Assist, Agent, care). */
class PetNoticedStore(context: Context) {
    private val dataStore = context.applicationContext.petNoticedDataStore

    fun observeToday(): Flow<List<String>> = dataStore.data.map { prefs ->
        val day = prefs[Keys.day] ?: return@map emptyList()
        if (day != todayKey()) return@map emptyList()
        listOfNotNull(
            prefs[Keys.line1]?.takeIf { it.isNotBlank() },
            prefs[Keys.line2]?.takeIf { it.isNotBlank() },
            prefs[Keys.line3]?.takeIf { it.isNotBlank() },
        )
    }

    suspend fun record(notice: String) {
        val trimmed = notice.trim().take(120)
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            val day = todayKey()
            if (prefs[Keys.day] != day) {
                prefs[Keys.day] = day
                prefs[Keys.line1] = trimmed
                prefs.remove(Keys.line2)
                prefs.remove(Keys.line3)
                return@edit
            }
            val existing = listOfNotNull(
                prefs[Keys.line1],
                prefs[Keys.line2],
                prefs[Keys.line3],
            )
            if (existing.contains(trimmed)) return@edit
            val updated = (existing + trimmed).takeLast(3)
            prefs[Keys.line1] = updated.getOrNull(0) ?: ""
            if (updated.size > 1) prefs[Keys.line2] = updated[1] else prefs.remove(Keys.line2)
            if (updated.size > 2) prefs[Keys.line3] = updated[2] else prefs.remove(Keys.line3)
        }
    }

    private fun todayKey(): String =
        LocalDate.now(ZoneId.systemDefault()).toString()

    private object Keys {
        val day = stringPreferencesKey("day")
        val line1 = stringPreferencesKey("line1")
        val line2 = stringPreferencesKey("line2")
        val line3 = stringPreferencesKey("line3")
        val visitDay = stringPreferencesKey("visit_day")
        val visitStreak = longPreferencesKey("visit_streak")
    }

    suspend fun recordVisitCheckIn(friendName: String) {
        dataStore.edit { prefs ->
            val day = todayKey()
            val lastDay = prefs[Keys.visitDay]
            val streak = prefs[Keys.visitStreak] ?: 0L
            prefs[Keys.visitDay] = day
            prefs[Keys.visitStreak] = when (lastDay) {
                day -> streak
                yesterdayKey() -> streak + 1
                else -> 1L
            }
            val label = "Checked on ${friendName.trim().ifBlank { "a friend" }}'s nibbli"
            // Also add to noticed lines via separate edit would race — inline:
            val noticeDay = prefs[Keys.day]
            if (noticeDay != day) {
                prefs[Keys.day] = day
                prefs[Keys.line1] = label
                prefs.remove(Keys.line2)
                prefs.remove(Keys.line3)
            } else {
                val existing = listOfNotNull(prefs[Keys.line1], prefs[Keys.line2], prefs[Keys.line3])
                if (!existing.contains(label)) {
                    val updated = (existing + label).takeLast(3)
                    prefs[Keys.line1] = updated.getOrNull(0) ?: ""
                    if (updated.size > 1) prefs[Keys.line2] = updated[1] else prefs.remove(Keys.line2)
                    if (updated.size > 2) prefs[Keys.line3] = updated[2] else prefs.remove(Keys.line3)
                }
            }
        }
    }

    fun observeVisitStreak(): Flow<Long> = dataStore.data.map { prefs ->
        if (prefs[Keys.visitDay] != todayKey() && prefs[Keys.visitDay] != yesterdayKey()) {
            0L
        } else {
            prefs[Keys.visitStreak] ?: 0L
        }
    }

    private fun yesterdayKey(): String =
        LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString()
}

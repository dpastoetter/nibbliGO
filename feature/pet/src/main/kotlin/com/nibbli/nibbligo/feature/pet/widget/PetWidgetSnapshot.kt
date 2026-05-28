package com.nibbli.nibbligo.feature.pet.widget

import android.content.Context
import com.nibbli.nibbligo.core.model.PetState

object PetWidgetSnapshot {
    private const val PREFS = "pet_widget_snapshot"

    fun write(context: Context, state: PetState) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, state.name)
            .putString(KEY_STAGE, state.stage.name)
            .putString(KEY_NEED, state.activeNeed.name)
            .apply()
    }

    fun read(context: Context): Snapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Snapshot(
            name = prefs.getString(KEY_NAME, "Nibbl") ?: "Nibbl",
            stage = prefs.getString(KEY_STAGE, "EGG") ?: "EGG",
            need = prefs.getString(KEY_NEED, "NONE") ?: "NONE",
        )
    }

    data class Snapshot(val name: String, val stage: String, val need: String)

    private const val KEY_NAME = "name"
    private const val KEY_STAGE = "stage"
    private const val KEY_NEED = "need"
}

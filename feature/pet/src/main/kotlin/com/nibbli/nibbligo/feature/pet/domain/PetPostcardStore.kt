package com.nibbli.nibbligo.feature.pet.domain

import android.content.Context
import android.content.SharedPreferences

class PetPostcardStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(postcard: PetPostcard) {
        prefs.edit()
            .putString(KEY_POSTCARD, postcard.toJson())
            .apply()
    }

    fun load(): PetPostcard? {
        val raw = prefs.getString(KEY_POSTCARD, null) ?: return null
        val postcard = PetPostcardCodec.decode(raw) ?: return null
        if (PetPostcard.isExpired(postcard)) {
            clear()
            return null
        }
        return postcard
    }

    fun clear() {
        prefs.edit().remove(KEY_POSTCARD).apply()
    }

    companion object {
        private const val PREFS = "pet_postcard_store"
        private const val KEY_POSTCARD = "active_postcard"
    }
}

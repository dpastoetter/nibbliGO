package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nibbli.nibbligo.core.domain.repository.AccessibilityPreferencesRepository
import com.nibbli.nibbligo.core.domain.repository.ParentalControlsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appControlsDataStore: DataStore<Preferences> by preferencesDataStore("nibbli_app_controls")

/** Fixed salt; PIN entropy is low (4 digits) so this only guards against casual plaintext reads. */
private const val PIN_SALT = "nibbligo-parental-v1"

internal fun hashPin(rawPin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("$PIN_SALT:$rawPin".toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Singleton
class ParentalControlsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ParentalControlsRepository {

    private object Keys {
        val pinHash = stringPreferencesKey("parental_pin_hash")
        val restrictAdultFeatures = booleanPreferencesKey("parental_restrict_adult_features")
    }

    override val pinHash: Flow<String?> =
        context.appControlsDataStore.data.map { it[Keys.pinHash] }

    override val restrictAdultFeatures: Flow<Boolean> =
        context.appControlsDataStore.data.map { it[Keys.restrictAdultFeatures] ?: false }

    override suspend fun setPin(rawPin: String?) {
        context.appControlsDataStore.edit { prefs ->
            val trimmed = rawPin?.trim()
            if (trimmed.isNullOrEmpty()) {
                prefs.remove(Keys.pinHash)
                prefs[Keys.restrictAdultFeatures] = false
            } else {
                prefs[Keys.pinHash] = hashPin(trimmed)
            }
        }
    }

    override suspend fun verifyPin(rawPin: String): Boolean {
        val stored = pinHash.first() ?: return false
        return stored == hashPin(rawPin.trim())
    }

    override suspend fun isPinSet(): Boolean = pinHash.first() != null

    override suspend fun setRestrictAdultFeatures(enabled: Boolean) {
        context.appControlsDataStore.edit { it[Keys.restrictAdultFeatures] = enabled }
    }
}

@Singleton
class AccessibilityPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AccessibilityPreferencesRepository {

    private object Keys {
        val fontScale = floatPreferencesKey("accessibility_font_scale")
    }

    override val fontScale: Flow<Float> =
        context.appControlsDataStore.data.map { (it[Keys.fontScale] ?: 1.0f).coerceIn(0.85f, 1.6f) }

    override suspend fun setFontScale(scale: Float) {
        context.appControlsDataStore.edit { it[Keys.fontScale] = scale.coerceIn(0.85f, 1.6f) }
    }
}

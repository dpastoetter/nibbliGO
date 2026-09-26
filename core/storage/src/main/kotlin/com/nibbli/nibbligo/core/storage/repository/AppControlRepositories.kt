package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nibbli.nibbligo.core.domain.repository.AccessibilityPreferencesRepository
import com.nibbli.nibbligo.core.domain.repository.ParentalControlsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appControlsDataStore: DataStore<Preferences> by preferencesDataStore("nibbli_app_controls")

private const val PBKDF2_ITERATIONS = 120_000
private const val PBKDF2_KEY_BITS = 256
private const val SALT_BYTES = 16
private const val MAX_FAILED_ATTEMPTS = 5
private const val LOCKOUT_BASE_MS = 30_000L
private const val LEGACY_PIN_SALT = "nibbligo-parental-v1"

internal fun hashPinPbkdf2(rawPin: String, salt: ByteArray): String {
    val spec = PBEKeySpec(rawPin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = skf.generateSecret(spec).encoded
    return Base64.encodeToString(hash, Base64.NO_WRAP)
}

/** Legacy fixed-salt SHA-256 used only to migrate existing installs. */
internal fun legacyHashPin(rawPin: String): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("$LEGACY_PIN_SALT:$rawPin".toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Singleton
class ParentalControlsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ParentalControlsRepository {

    private object Keys {
        val pinHash = stringPreferencesKey("parental_pin_hash")
        val pinSalt = stringPreferencesKey("parental_pin_salt")
        val pinAlgo = stringPreferencesKey("parental_pin_algo")
        val failedAttempts = intPreferencesKey("parental_pin_failed_attempts")
        val lockedUntil = longPreferencesKey("parental_pin_locked_until")
        val restrictAdultFeatures = booleanPreferencesKey("parental_restrict_adult_features")
    }

    private val secureRandom = SecureRandom()

    override val pinHash: Flow<String?> =
        context.appControlsDataStore.data.map { it[Keys.pinHash] }

    override val restrictAdultFeatures: Flow<Boolean> =
        context.appControlsDataStore.data.map { it[Keys.restrictAdultFeatures] ?: false }

    override val pinLockedUntilEpochMs: Flow<Long> =
        context.appControlsDataStore.data.map { it[Keys.lockedUntil] ?: 0L }

    override suspend fun setPin(rawPin: String, currentPin: String?): Boolean {
        val trimmed = rawPin.trim()
        if (trimmed.length < 4 || !trimmed.all { it.isDigit() }) return false

        val prefs = context.appControlsDataStore.data.first()
        val existing = prefs[Keys.pinHash]
        if (existing != null) {
            if (currentPin == null) return false
            if (!verifyPinInternal(currentPin, prefs, recordFailure = true)) return false
        }

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val hash = hashPinPbkdf2(trimmed, salt)
        context.appControlsDataStore.edit { editor ->
            editor[Keys.pinHash] = hash
            editor[Keys.pinSalt] = Base64.encodeToString(salt, Base64.NO_WRAP)
            editor[Keys.pinAlgo] = "pbkdf2"
            editor[Keys.failedAttempts] = 0
            editor.remove(Keys.lockedUntil)
        }
        return true
    }

    override suspend fun clearPin(currentPin: String): Boolean {
        val prefs = context.appControlsDataStore.data.first()
        if (prefs[Keys.pinHash] == null) return true
        if (!verifyPinInternal(currentPin, prefs, recordFailure = true)) return false
        context.appControlsDataStore.edit { editor ->
            editor.remove(Keys.pinHash)
            editor.remove(Keys.pinSalt)
            editor.remove(Keys.pinAlgo)
            editor[Keys.restrictAdultFeatures] = false
            editor[Keys.failedAttempts] = 0
            editor.remove(Keys.lockedUntil)
        }
        return true
    }

    override suspend fun verifyPin(rawPin: String): Boolean {
        val prefs = context.appControlsDataStore.data.first()
        return verifyPinInternal(rawPin, prefs, recordFailure = true)
    }

    override suspend fun isPinSet(): Boolean = pinHash.first() != null

    override suspend fun setRestrictAdultFeatures(enabled: Boolean) {
        context.appControlsDataStore.edit { it[Keys.restrictAdultFeatures] = enabled }
    }

    /** Test-only: wipe parental control preferences without knowing the PIN. */
    internal suspend fun clearAllForTests() {
        context.appControlsDataStore.edit { it.clear() }
    }

    private suspend fun verifyPinInternal(
        rawPin: String,
        prefs: Preferences,
        recordFailure: Boolean,
    ): Boolean {
        val stored = prefs[Keys.pinHash] ?: return false
        val now = System.currentTimeMillis()
        val lockedUntil = prefs[Keys.lockedUntil] ?: 0L
        if (lockedUntil > now) return false

        val trimmed = rawPin.trim()
        val algo = prefs[Keys.pinAlgo]
        val matches = when (algo) {
            "pbkdf2" -> {
                val saltB64 = prefs[Keys.pinSalt] ?: return false
                val salt = Base64.decode(saltB64, Base64.NO_WRAP)
                stored == hashPinPbkdf2(trimmed, salt)
            }
            else -> stored == legacyHashPin(trimmed)
        }

        if (matches) {
            context.appControlsDataStore.edit { editor ->
                editor[Keys.failedAttempts] = 0
                editor.remove(Keys.lockedUntil)
                // Migrate legacy SHA-256 hashes to PBKDF2 on successful verify.
                if (algo != "pbkdf2") {
                    val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
                    editor[Keys.pinHash] = hashPinPbkdf2(trimmed, salt)
                    editor[Keys.pinSalt] = Base64.encodeToString(salt, Base64.NO_WRAP)
                    editor[Keys.pinAlgo] = "pbkdf2"
                }
            }
            return true
        }

        if (recordFailure) {
            context.appControlsDataStore.edit { editor ->
                val attempts = (prefs[Keys.failedAttempts] ?: 0) + 1
                editor[Keys.failedAttempts] = attempts
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    val multiplier = 1 shl (attempts - MAX_FAILED_ATTEMPTS).coerceAtMost(4)
                    editor[Keys.lockedUntil] = now + LOCKOUT_BASE_MS * multiplier
                }
            }
        }
        return false
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

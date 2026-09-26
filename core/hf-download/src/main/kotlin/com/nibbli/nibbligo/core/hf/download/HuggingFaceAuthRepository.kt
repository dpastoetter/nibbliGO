// Ported from gallery@main: HF token flow patterns (Apache 2.0)
package com.nibbli.nibbligo.core.hf.download

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HuggingFaceAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: HuggingFaceOAuthConfig,
) {
    private val prefs: SharedPreferences by lazy { encryptedPrefs(context) }
    private val _accessToken = MutableStateFlow(prefs.getString(KEY_ACCESS_TOKEN, null))
    val accessToken: Flow<String?> = _accessToken.asStateFlow()

    fun isConfigured(): Boolean = config.clientId.isNotBlank()

    fun createAuthIntent(): Intent? {
        if (!isConfigured()) return null
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(HuggingFaceConfig.AUTH_ENDPOINT),
            Uri.parse(HuggingFaceConfig.TOKEN_ENDPOINT),
        )
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            config.clientId,
            ResponseTypeValues.CODE,
            Uri.parse(config.redirectUri),
        )
            .setScope("openid profile gated-repos read-repos")
            .build()
        return AuthorizationService(context).getAuthorizationRequestIntent(request)
    }

    suspend fun saveAccessToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_ACCESS_TOKEN, token) }
        _accessToken.value = token
    }

    suspend fun clearToken() = withContext(Dispatchers.IO) {
        prefs.edit { remove(KEY_ACCESS_TOKEN) }
        _accessToken.value = null
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ACCESS_TOKEN, null).also { _accessToken.value = it }
    }

    fun getAccessTokenBlocking(): String? = runBlocking { getAccessToken() }

    companion object {
        private const val PREFS_NAME = "hf_auth_encrypted"
        private const val KEY_ACCESS_TOKEN = "access_token"

        private fun encryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}

data class HuggingFaceOAuthConfig(
    val clientId: String,
    val redirectUri: String,
)

// Ported from gallery@main: HF token flow patterns (Apache 2.0)
package com.nibbli.nibbligo.core.hf.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Singleton

private val Context.hfDataStore by preferencesDataStore("hf_auth")

@Singleton
class HuggingFaceAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: HuggingFaceOAuthConfig,
) {
    private val tokenKey = stringPreferencesKey("access_token")

    val accessToken: Flow<String?> = context.hfDataStore.data.map { it[tokenKey] }

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

    suspend fun saveAccessToken(token: String) {
        context.hfDataStore.edit { it[tokenKey] = token }
    }

    suspend fun clearToken() {
        context.hfDataStore.edit { it.remove(tokenKey) }
    }

    suspend fun getAccessToken(): String? =
        context.hfDataStore.data.first()[tokenKey]

    fun getAccessTokenBlocking(): String? = runBlocking { getAccessToken() }
}

data class HuggingFaceOAuthConfig(
    val clientId: String,
    val redirectUri: String,
)

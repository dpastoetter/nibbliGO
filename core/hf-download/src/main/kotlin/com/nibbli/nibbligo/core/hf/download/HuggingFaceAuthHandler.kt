package com.nibbli.nibbligo.core.hf.download

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class HuggingFaceAuthHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: HuggingFaceAuthRepository,
) {
    suspend fun handleAuthorizationResponse(intent: Intent): Result<Unit> {
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (ex != null) {
            return Result.failure(Exception(ex.errorDescription ?: ex.error ?: "auth failed"))
        }
        if (response == null) {
            return Result.failure(IllegalStateException("No authorization response"))
        }
        return suspendCancellableCoroutine { cont ->
            val authService = AuthorizationService(context)
            val tokenRequest = response.createTokenExchangeRequest()
            authService.performTokenRequest(tokenRequest) { tokenResponse, tokenEx ->
                authService.dispose()
                when {
                    tokenEx != null ->
                        cont.resume(Result.failure(Exception(tokenEx.errorDescription ?: "token exchange failed")))
                    tokenResponse?.accessToken.isNullOrBlank() ->
                        cont.resume(Result.failure(IllegalStateException("No access token in response")))
                    else -> {
                        val token = tokenResponse.accessToken.orEmpty()
                        CoroutineScope(Dispatchers.IO).launch {
                            authRepository.saveAccessToken(token)
                        }
                        cont.resume(Result.success(Unit))
                    }
                }
            }
        }
    }
}

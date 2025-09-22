package com.onedrivesyncer.app.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.*
import org.json.JSONObject
import kotlin.coroutines.resume

class OneDriveAuth(private val context: Context, private val store: TokenStore) {
    private val authService by lazy { AuthorizationService(context) }
    suspend fun ensureAccessToken(): String? {
        // Using MSAL elsewhere; keep placeholder here if needed later
        return store.get(KEY)
    }
    companion object { private const val KEY = "onedrive_access_token" }
}

class GoogleAuth(private val context: Context, private val store: TokenStore) {
    private val authService by lazy { AuthorizationService(context) }
    private val authStateKey = "google_auth_state"

    private fun loadState(): AuthState? = store.get(authStateKey)?.let {
        try { AuthState.jsonDeserialize(it) } catch (_: Throwable) { null }
    }

    private fun saveState(state: AuthState?) {
        store.save(authStateKey, state?.jsonSerializeString())
    }

    fun createAuthIntent(): android.content.Intent? {
        val clientId = context.getString(com.onedrivesyncer.app.R.string.google_client_id)
        val redirect = context.getString(com.onedrivesyncer.app.R.string.google_redirect_uri)
        if (clientId.startsWith("SET_ME") || !redirect.contains("://")) return null

        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
            Uri.parse("https://oauth2.googleapis.com/token")
        )
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirect)
        )
            .setScopes(
                "openid",
                "profile",
                "https://www.googleapis.com/auth/photoslibrary.appendonly"
            )
            .build()
        return authService.getAuthorizationRequestIntent(request)
    }

    fun handleAuthResponse(data: android.content.Intent?, onResult: (Boolean) -> Unit) {
        val resp = AuthorizationResponse.fromIntent(data ?: return onResult(false))
        val ex = AuthorizationException.fromIntent(data)
        val state = AuthState(resp, ex)
        if (resp != null) {
            authService.performTokenRequest(resp.createTokenExchangeRequest()) { tokenResp, tokenEx ->
                if (tokenResp != null && tokenEx == null) {
                    state.update(tokenResp, null)
                    saveState(state)
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        } else {
            onResult(false)
        }
    }

    suspend fun ensureAccessToken(): String? {
        val state = loadState() ?: return null
        return suspendCancellableCoroutine { cont ->
            state.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                if (ex != null) cont.resume(null) else cont.resume(accessToken)
            }
        }
    }
}

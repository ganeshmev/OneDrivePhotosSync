package com.onedrivesyncer.app.auth

import android.content.Context
import net.openid.appauth.AuthorizationService

class OneDriveAuth(private val context: Context, private val store: TokenStore) {
    private val authService by lazy { AuthorizationService(context) }
    suspend fun ensureAccessToken(): String? {
        // TODO: Implement Azure AD auth via AppAuth (PKCE), save/refresh tokens in TokenStore
        return store.get(KEY)
    }
    companion object { private const val KEY = "onedrive_access_token" }
}

class GoogleAuth(private val context: Context, private val store: TokenStore) {
    private val authService by lazy { AuthorizationService(context) }
    suspend fun ensureAccessToken(): String? {
        // TODO: Implement Google OAuth via AppAuth, save/refresh tokens in TokenStore
        return store.get(KEY)
    }
    companion object { private const val KEY = "google_access_token" }
}

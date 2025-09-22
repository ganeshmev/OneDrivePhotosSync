package com.onedrivesyncer.app.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException

class OneDriveMsalAuth(private val context: Context) {
    private val app: ISingleAccountPublicClientApplication by lazy {
        PublicClientApplication.createSingleAccountPublicClientApplication(context, R.raw.msal_config)
    }

    fun signIn(activity: Activity, scopes: Array<String>, onResult: (Boolean, String?) -> Unit) {
        app.signIn(activity, null, scopes, object : AuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult?) {
                onResult(true, result?.accessToken)
            }

            override fun onError(exception: MsalException?) {
                Log.e(TAG, "MSAL sign-in error", exception)
                onResult(false, null)
            }

            override fun onCancel() { onResult(false, null) }
        })
    }

    fun getAccessTokenSilent(scopes: Array<String>, onResult: (String?) -> Unit) {
        app.acquireTokenSilentAsync(scopes, getAuthority(), object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                onResult(authenticationResult?.accessToken)
            }

            override fun onError(exception: MsalException?) {
                Log.w(TAG, "Silent token failed", exception)
                onResult(null)
            }
        })
    }

    private fun getAuthority(): String {
        // Use default authority from msal_config.json
        return (app as? PublicClientApplication)?.configuration?.defaultAuthority?.authorityURL?.toString()
            ?: "https://login.microsoftonline.com/common/"
    }

    companion object { private const val TAG = "OneDriveMsalAuth" }
}

package com.onedrivesyncer.app.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException

class OneDriveMsalAuth(private val context: Context) {

    private fun withApp(cb: (ISingleAccountPublicClientApplication?) -> Unit) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            com.onedrivesyncer.app.R.raw.msal_config,
            object : ISingleAccountPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    cb(application)
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "MSAL create app error", exception)
                    cb(null)
                }
            }
        )
    }

    fun signIn(activity: Activity, scopes: Array<String>, onResult: (Boolean, String?) -> Unit) {
        withApp { app ->
            if (app == null) return@withApp onResult(false, null)
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
    }

    fun getAccessTokenSilent(scopes: Array<String>, onResult: (String?) -> Unit) {
        withApp { app ->
            if (app == null) return@withApp onResult(null)
            app.acquireTokenSilentAsync(scopes, null, object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                    onResult(authenticationResult?.accessToken)
                }

                override fun onError(exception: MsalException?) {
                    Log.w(TAG, "Silent token failed", exception)
                    onResult(null)
                }
            })
        }
    }

    companion object { private const val TAG = "OneDriveMsalAuth" }
}

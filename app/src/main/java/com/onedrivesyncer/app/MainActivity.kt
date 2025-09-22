package com.onedrivesyncer.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.onedrivesyncer.app.databinding.ActivityMainBinding
import com.onedrivesyncer.app.service.SyncService
import com.onedrivesyncer.app.auth.OneDriveMsalAuth
import com.onedrivesyncer.app.auth.GoogleAuth
import com.onedrivesyncer.app.auth.TokenStore
import com.microsoft.identity.client.PublicClientApplication
import net.openid.appauth.AuthState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            getSharedPreferences("saf", MODE_PRIVATE).edit().putString("root", uri.toString()).apply()
        }
    }

    private val googleAuthResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val store = TokenStore(this)
        val gAuth = GoogleAuth(this, store)
        gAuth.handleAuthResponse(result.data) {
            updateStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            startForegroundService(Intent(this, SyncService::class.java))
        }

        binding.btnGrantPerms.setOnClickListener {
            val perms = mutableListOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
            )
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                perms += Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
            requestPermissions.launch(perms.toTypedArray())
        }

        binding.btnBatteryOpt.setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

        binding.btnPickFolder.setOnClickListener { pickFolder.launch(null) }

        binding.btnSignInOneDrive.setOnClickListener {
            val msal = OneDriveMsalAuth(this)
            val scopes = arrayOf("Files.ReadWrite", "offline_access")
            msal.signIn(this, scopes) { ok, _ ->
                // optional: toast/log
            }
        }

        binding.btnSignInGoogle.setOnClickListener {
            val intent = GoogleAuth(this, TokenStore(this)).createAuthIntent()
            if (intent != null) googleAuthResult.launch(intent)
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        // OneDrive signed-in status via MSAL cache
        try {
            val app = PublicClientApplication.createSingleAccountPublicClientApplication(this, com.onedrivesyncer.app.R.raw.msal_config)
            app.getCurrentAccountAsync(object : com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: com.microsoft.identity.client.IAccount?) {
                    val signedIn = activeAccount != null
                    binding.tvStatusOneDrive.text = getString(if (signedIn) com.onedrivesyncer.app.R.string.status_onedrive_signed_in else com.onedrivesyncer.app.R.string.status_onedrive_signed_out)
                }

                override fun onAccountChanged(priorAccount: com.microsoft.identity.client.IAccount?, currentAccount: com.microsoft.identity.client.IAccount?) {
                    val signedIn = currentAccount != null
                    binding.tvStatusOneDrive.text = getString(if (signedIn) com.onedrivesyncer.app.R.string.status_onedrive_signed_in else com.onedrivesyncer.app.R.string.status_onedrive_signed_out)
                }

                override fun onError(exception: com.microsoft.identity.client.exception.MsalException) {
                    binding.tvStatusOneDrive.text = getString(com.onedrivesyncer.app.R.string.status_onedrive_signed_out)
                }
            })
        } catch (_: Throwable) {
            binding.tvStatusOneDrive.text = getString(com.onedrivesyncer.app.R.string.status_onedrive_signed_out)
        }

        // Google signed-in via stored AuthState
        val store = TokenStore(this)
        val stateJson = store.get("google_auth_state")
        val signedInG = try { stateJson != null && AuthState.jsonDeserialize(stateJson).isAuthorized } catch (_: Throwable) { false }
        binding.tvStatusGoogle.text = getString(if (signedInG) com.onedrivesyncer.app.R.string.status_google_signed_in else com.onedrivesyncer.app.R.string.status_google_signed_out)

        // Last sync result
        val last = getSharedPreferences("sync", MODE_PRIVATE).getString("last_result", null)
        binding.tvLastSync.text = last ?: getString(com.onedrivesyncer.app.R.string.last_sync_unknown)
    }
}

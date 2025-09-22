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
        gAuth.handleAuthResponse(result.data) { /* optional UI feedback */ }
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
    }
}

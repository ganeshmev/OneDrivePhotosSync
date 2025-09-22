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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

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
    }
}

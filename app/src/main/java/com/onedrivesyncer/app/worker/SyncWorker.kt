package com.onedrivesyncer.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.onedrivesyncer.app.auth.GoogleAuth
import com.onedrivesyncer.app.auth.OneDriveAuth
import com.onedrivesyncer.app.auth.TokenStore
import com.onedrivesyncer.app.sync.GooglePhotosClient
import com.onedrivesyncer.app.sync.OneDriveClient

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        Log.d(TAG, "Sync tick: attempting auth and placeholder sync")
        val store = TokenStore(applicationContext)
        val onedriveAuth = OneDriveAuth(applicationContext, store)
        val googleAuth = GoogleAuth(applicationContext, store)
        val odToken = onedriveAuth.ensureAccessToken()
        val gToken = googleAuth.ensureAccessToken()
        val oneDrive = OneDriveClient { odToken }
        val google = GooglePhotosClient { gToken }

        // Placeholder: list then no-op
        val items = oneDrive.listNewMedia()
        Log.d(TAG, "Found ${items.size} remote items to process")
        // Upload flow would go here
        return Result.success()
    }

    companion object { private const val TAG = "SyncWorker" }
}

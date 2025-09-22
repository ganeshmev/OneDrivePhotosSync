package com.onedrivesyncer.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        Log.d(TAG, "Sync tick: placeholder work running")
        // TODO: 1) Auth to OneDrive via MSAL, list new media files
        // TODO: 2) Upload to Google Photos (via Drive or Photos Library upload tokens)
        // TODO: 3) On success, delete from OneDrive and optional local copies via SAF
        return Result.success()
    }

    companion object { private const val TAG = "SyncWorker" }
}

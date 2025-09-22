package com.onedrivesyncer.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.onedrivesyncer.app.auth.GoogleAuth
import com.onedrivesyncer.app.auth.OneDriveMsalAuth
import com.onedrivesyncer.app.auth.TokenStore
import com.onedrivesyncer.app.sync.GooglePhotosClient
import com.onedrivesyncer.app.sync.OneDriveClient
import com.onedrivesyncer.app.sync.DeviceDeleter
import okhttp3.OkHttpClient
import okhttp3.Request

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        Log.d(TAG, "Sync tick: attempting auth and placeholder sync")
        val store = TokenStore(applicationContext)
        val msal = OneDriveMsalAuth(applicationContext)
        var odToken: String? = null
        val scopes = arrayOf("Files.ReadWrite", "offline_access")
        // Try silent; if not available, skip this run (user must sign in via UI)
        val latch = java.util.concurrent.CountDownLatch(1)
        msal.getAccessTokenSilent(scopes) { token ->
            odToken = token
            latch.countDown()
        }
        latch.await()

        val googleAuth = GoogleAuth(applicationContext, store)
        val gToken = googleAuth.ensureAccessToken()
        if (odToken.isNullOrEmpty() || gToken.isNullOrEmpty()) {
            Log.w(TAG, "Auth not ready (OneDrive or Google). Skipping this cycle.")
            return Result.success()
        }

        val oneDrive = OneDriveClient { odToken }
        val google = GooglePhotosClient { gToken }

        val items = oneDrive.listNewMedia()
        Log.d(TAG, "Found ${items.size} remote items to process")
        val http = OkHttpClient()
        var successCount = 0
    val safRoot = applicationContext.getSharedPreferences("saf", Context.MODE_PRIVATE).getString("root", null)
    val deleter = DeviceDeleter(applicationContext, safRoot?.let { android.net.Uri.parse(it) })
    for (item in items) {
            try {
                val request = Request.Builder().url(item.downloadUrl).build()
                val bytes = http.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("Download failed ${resp.code}")
                    resp.body?.bytes() ?: ByteArray(0)
                }
                if (bytes.isEmpty()) continue
                val uploaded = google.uploadBytes(item.name, bytes)
                if (uploaded) {
                    val deleted = oneDrive.delete(item.id)
                    // Try local delete (best-effort). Mapping of name to path may vary; using name only.
                    deleter.deleteIfExists(item.name)
                    if (deleted) successCount++
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed processing ${item.id}", t)
            }
        }
        val msg = "Last sync: ${java.time.Instant.now()} — success=$successCount, total=${items.size}"
        Log.d(TAG, msg)
        applicationContext.getSharedPreferences("sync", Context.MODE_PRIVATE).edit().putString("last_result", msg).apply()
        return Result.success()
    }

    companion object { private const val TAG = "SyncWorker" }
}

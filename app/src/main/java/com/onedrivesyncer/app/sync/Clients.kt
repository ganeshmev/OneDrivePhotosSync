package com.onedrivesyncer.app.sync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class RemoteMedia(val id: String, val name: String, val downloadUrl: String)

class OneDriveClient(private val tokenProvider: suspend () -> String?) {
    private val http = OkHttpClient()
    suspend fun listNewMedia(): List<RemoteMedia> {
        // TODO: Use Microsoft Graph: GET /me/drive/special/cameraRoll or specific folder children
        return emptyList()
    }
    suspend fun delete(id: String): Boolean {
        // TODO: DELETE /me/drive/items/{item-id}
        return true
    }
}

class GooglePhotosClient(private val tokenProvider: suspend () -> String?) {
    private val http = OkHttpClient()
    suspend fun uploadBytes(name: String, bytes: ByteArray): Boolean {
        // Example Photos upload-token flow is multi-step; placeholder upload to Drive shown here
        val token = tokenProvider() ?: return false
        val req: Request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=media")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/octet-stream")
            .post(bytes.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp -> return resp.isSuccessful }
    }
}

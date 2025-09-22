package com.onedrivesyncer.app.sync

import com.onedrivesyncer.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject

data class RemoteMedia(val id: String, val name: String, val downloadUrl: String)

class OneDriveClient(private val tokenProvider: suspend () -> String?) {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
        .build()

    suspend fun listNewMedia(): List<RemoteMedia> {
        val token = tokenProvider() ?: return emptyList()
    var nextUrl: String? = "https://graph.microsoft.com/v1.0/me/drive/special/cameraRoll/children?%24select=id,name,@microsoft.graph.downloadUrl,photo,video,file&%24top=200"
        val list = mutableListOf<RemoteMedia>()
        var keepGoing = true
        while (keepGoing && nextUrl != null && list.size < 1000) {
            val url = nextUrl!!
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    keepGoing = false
                } else {
                    val body = resp.body?.string()
                    if (body == null) {
                        keepGoing = false
                    } else {
                        val json = JSONObject(body)
                        val arr: JSONArray = json.optJSONArray("value") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val hasMedia = item.has("photo") || item.has("video")
                            val dl = item.optString("@microsoft.graph.downloadUrl", null)
                            if (hasMedia && dl != null) {
                                list += RemoteMedia(
                                    id = item.getString("id"),
                                    name = item.optString("name", item.getString("id")),
                                    downloadUrl = dl
                                )
                            }
                        }
                        nextUrl = json.optString("@odata.nextLink", null)
                        keepGoing = nextUrl != null
                    }
                }
            }
        }
        return list
    }

    suspend fun delete(id: String): Boolean {
        val token = tokenProvider() ?: return false
        val req = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me/drive/items/$id")
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()
        http.newCall(req).execute().use { return it.isSuccessful }
    }
}

class GooglePhotosClient(private val tokenProvider: suspend () -> String?) {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
        .build()

    suspend fun uploadBytes(name: String, bytes: ByteArray): Boolean {
        val token = tokenProvider() ?: return false
        // Step 1: upload bytes to get uploadToken
        val uploadReq = Request.Builder()
            .url("https://photoslibrary.googleapis.com/v1/uploads")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-Goog-Upload-File-Name", name)
            .addHeader("X-Goog-Upload-Protocol", "raw")
            .post(bytes.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        val uploadToken: String = http.newCall(uploadReq).execute().use { resp ->
            if (!resp.isSuccessful) return false
            resp.body?.string()?.trim().orEmpty()
        }
        if (uploadToken.isEmpty()) return false

        // Step 2: create media item
        val createBody = JSONObject(mapOf(
            "newMediaItems" to listOf(
                mapOf(
                    "simpleMediaItem" to mapOf(
                        "fileName" to name,
                        "uploadToken" to uploadToken
                    )
                )
            )
        )).toString().toRequestBody("application/json".toMediaType())

        val createReq = Request.Builder()
            .url("https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(createBody)
            .build()
        return http.newCall(createReq).execute().use { it.isSuccessful }
    }
}

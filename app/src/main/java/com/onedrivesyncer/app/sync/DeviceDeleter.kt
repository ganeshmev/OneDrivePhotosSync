package com.onedrivesyncer.app.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class DeviceDeleter(private val context: Context, private val pickedRoot: Uri?) {
    fun deleteIfExists(relativePath: String): Boolean {
        if (pickedRoot == null) return false
        val root = DocumentFile.fromTreeUri(context, pickedRoot) ?: return false
        val segments = relativePath.trim('/').split('/')
        var current = root
        for (i in segments.indices) {
            val name = segments[i]
            val next = current?.findFile(name)
            if (next == null) return false
            if (i == segments.size - 1) {
                return next.delete()
            } else {
                current = next
            }
        }
        return false
    }
}

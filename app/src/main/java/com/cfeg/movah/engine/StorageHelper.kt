package com.cfeg.movah.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
object StorageHelper {

    fun hasManageStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun openManageStorageSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    fun resolvePathFromTreeUri(treeUri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri) ?: return null
        val parts = docId.split(":")
        if (parts.isEmpty()) return null

        val volume = parts[0]
        val relativePath = if (parts.size > 1) parts[1] else ""

        return if (volume.equals("primary", ignoreCase = true)) {
            val root = Environment.getExternalStorageDirectory().absolutePath
            if (relativePath.isEmpty()) root else "$root/$relativePath"
        } else {
            // Removable storage (SD Card, USB-OTG)
            val externalStoragePath = "/storage/$volume"
            if (relativePath.isEmpty()) externalStoragePath else "$externalStoragePath/$relativePath"
        }
    }
}

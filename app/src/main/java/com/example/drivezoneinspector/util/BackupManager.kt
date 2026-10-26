package com.example.drivezoneinspector.util

import android.content.Context
import android.net.Uri
import com.example.drivezoneinspector.model.BackupItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {

    private val backupFolder: File
        get() = File(context.filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }

    fun createBackup(sourceUri: Uri, originalName: String): BackupItem? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(backupFolder, "backup_${timestamp}_$safeName")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            BackupItem(
                fileName = targetFile.name,
                dateCreated = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
                absolutePath = targetFile.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBackups(): List<BackupItem> {
        val files = backupFolder.listFiles() ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }.map { file ->
            BackupItem(
                fileName = file.name,
                dateCreated = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified())),
                absolutePath = file.absolutePath
            )
        }
    }

    fun restoreBackupToUri(backupFilePath: String, destinationUri: Uri): Boolean {
        return try {
            val sourceFile = File(backupFilePath)
            if (!sourceFile.exists()) return false

            sourceFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

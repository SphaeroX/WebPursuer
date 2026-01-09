package com.murmli.webpursuer.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BackupRepository(private val context: Context) {

    fun exportDatabase(): Uri? {
        val dbName = "webpursuer_database"
        val database = AppDatabase.getDatabase(context)

        // Ensure all data is written to the main file
        val supportDb = database.openHelper.writableDatabase
        supportDb.query("PRAGMA wal_checkpoint(FULL)").close()

        val dbFile = context.getDatabasePath(dbName)
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val backupFile = File(backupDir, "webpursuer_backup.db")

        try {
            dbFile.inputStream().use { input ->
                backupFile.outputStream().use { output -> input.copyTo(output) }
            }
            return FileProvider.getUriForFile(
                    context,
                    "com.murmli.webpursuer.fileprovider",
                    backupFile
            )
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun importDatabase(sourceUri: Uri): Boolean {
        val dbName = "webpursuer_database"
        val dbFile = context.getDatabasePath(dbName)

        // Close the current database connection
        AppDatabase.closeAndReset()

        // Small delay to ensure lock release - though closeAndReset should handle it
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dbFile).use { output -> input.copyTo(output) }
            }
            // Verify if we can open the database to ensure it's valid
            // Force re-initialization
            AppDatabase.getDatabase(context).openHelper.readableDatabase
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}

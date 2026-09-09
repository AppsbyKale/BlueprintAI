package com.example.blueprintai.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    suspend fun createBackup(targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            database.close()
            val dbFile = context.getDatabasePath("blueprint_ai_db")
            
            ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
                // Add database
                addFileToZip(dbFile, "blueprint_ai_db", zos)
                addFileToZip(File(dbFile.path + "-shm"), "blueprint_ai_db-shm", zos)
                addFileToZip(File(dbFile.path + "-wal"), "blueprint_ai_db-wal", zos)
                
                // Add attachments folder (if any)
                // Assuming attachments are in context.filesDir / "attachments"
                val attachmentsDir = File(context.filesDir, "attachments")
                if (attachmentsDir.exists()) {
                    attachmentsDir.listFiles()?.forEach { file ->
                        addFileToZip(file, "attachments/${file.name}", zos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, entryName: String, zos: ZipOutputStream) {
        if (!file.exists()) return
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }

    suspend fun restoreBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            database.close()
            val dbFile = context.getDatabasePath("blueprint_ai_db")
            
            ZipInputStream(FileInputStream(backupFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val target = if (entry.name.startsWith("attachments/")) {
                        File(context.filesDir, entry.name)
                    } else {
                        context.getDatabasePath(entry.name)
                    }
                    
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { fos ->
                        zis.copyTo(fos)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

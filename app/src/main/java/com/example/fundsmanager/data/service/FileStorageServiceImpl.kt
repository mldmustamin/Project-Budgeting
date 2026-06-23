package com.example.fundsmanager.data.service

import android.content.Context
import com.example.fundsmanager.domain.service.FileStorageService
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class FileStorageServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLogger: AppLogger
) : FileStorageService {

    override suspend fun saveFile(inputStream: InputStream, fileName: String): String = withContext(Dispatchers.IO) {
        val attachmentsDir = File(context.filesDir, "attachments")
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }

        // Generate a unique name to avoid collisions
        val uniqueName = "${UUID.randomUUID()}_$fileName"
        val targetFile = File(attachmentsDir, uniqueName)

        targetFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        appLogger.info(
            category = AppLogCategory.FILE,
            screen = "FileStorage",
            action = "save_attachment_success",
            message = "Attachment file saved",
            details = "fileName=${fileName.take(120)} bytes=${targetFile.length()}"
        )
        uniqueName // Return only the name/relative path
    }

    override suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "attachments/$path")
        if (file.exists()) {
            val deleted = file.delete()
            appLogger.info(
                category = AppLogCategory.FILE,
                screen = "FileStorage",
                action = "delete_attachment_file",
                message = "Attachment file delete requested",
                details = "path=${path.take(160)} deleted=$deleted"
            )
            deleted
        } else {
            appLogger.warning(
                category = AppLogCategory.FILE,
                screen = "FileStorage",
                action = "delete_attachment_missing",
                message = "Attachment file not found",
                details = "path=${path.take(160)}"
            )
            false
        }
    }
}

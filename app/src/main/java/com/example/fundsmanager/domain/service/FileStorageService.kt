package com.example.fundsmanager.domain.service

import java.io.InputStream

interface FileStorageService {
    /**
     * Copies a file from an input stream to internal app storage.
     * Returns the relative path of the saved file.
     */
    suspend fun saveFile(inputStream: InputStream, fileName: String): String
    
    /**
     * Deletes a file from internal storage.
     */
    suspend fun deleteFile(path: String): Boolean
}

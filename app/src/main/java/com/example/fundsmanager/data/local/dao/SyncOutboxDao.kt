package com.example.fundsmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fundsmanager.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Insert
    suspend fun insert(entity: SyncOutboxEntity): Long

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncOutboxEntity>

    @Query("UPDATE sync_outbox SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE sync_outbox SET status = :status, lastError = :error, retryCount = retryCount + 1, updatedAt = :now WHERE id = :id")
    suspend fun markRejected(id: Long, error: String, status: String = "REJECTED", now: Long = System.currentTimeMillis())

    @Query("UPDATE sync_outbox SET status = 'SYNCED', updatedAt = :now WHERE id = :id")
    suspend fun markSynced(id: Long, now: Long = System.currentTimeMillis())
}
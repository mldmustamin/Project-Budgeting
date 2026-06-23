package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId AND deletedAt IS NULL")
    fun getAttachmentsByTransaction(transactionId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT DISTINCT transactionId FROM attachments WHERE transactionId IN (:transactionIds) AND deletedAt IS NULL")
    suspend fun getTransactionIdsWithAttachments(transactionIds: List<Long>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Query("UPDATE attachments SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteAttachment(id: Long, deletedAt: Long = System.currentTimeMillis())
}

package com.example.fundsmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fundsmanager.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityType = :entityType AND entityId = :entityId ORDER BY createdAt DESC")
    fun getAuditLogsForEntity(entityType: String, entityId: Long): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAuditLog(auditLog: AuditLogEntity): Long
}

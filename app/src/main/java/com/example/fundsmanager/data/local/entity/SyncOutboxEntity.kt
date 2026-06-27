package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fundsmanager.util.UuidGenerator

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["localUserId", "deviceId", "sessionId"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["status"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UuidGenerator.newUuid(),
    val localUserId: Long,
    val serverUserId: String? = null,
    val userUuid: String? = null,
    val deviceId: String? = null,
    val sessionId: String? = null,
    val entityType: String, // "transaction"
    val entityUuid: String,
    val operation: String, // CREATE, UPDATE, SOFT_DELETE
    val payloadJson: String,
    val idempotencyKey: String,
    val status: String = "PENDING", // PENDING, IN_FLIGHT, SYNCED, REJECTED, CONFLICT
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
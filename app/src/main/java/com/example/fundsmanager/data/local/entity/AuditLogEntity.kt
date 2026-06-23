package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["entityType", "entityId"]),
        Index(value = ["userId"]),
        Index(value = ["createdAt"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val entityType: String,
    val entityId: Long,
    val action: String,
    val oldValueJson: String? = null,
    val newValueJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

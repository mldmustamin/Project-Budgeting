package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fundsmanager.data.local.SyncStatus
import com.example.fundsmanager.util.UuidGenerator

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["uuid"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String? = null,
    val uuid: String = UuidGenerator.newUuid(),
    val serverId: String? = null,
    val serverUserId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fundsmanager.data.local.SyncStatus
import com.example.fundsmanager.util.UuidGenerator

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["uuid"], unique = true)
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val description: String? = null,
    val isArchived: Boolean = false,
    val startAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val uuid: String = UuidGenerator.newUuid(),
    val serverId: String? = null,
    val deviceId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

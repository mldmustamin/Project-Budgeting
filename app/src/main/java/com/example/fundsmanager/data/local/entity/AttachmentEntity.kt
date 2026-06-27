package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fundsmanager.data.local.SyncStatus
import com.example.fundsmanager.util.UuidGenerator

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["uuid"], unique = true)
    ]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val filePath: String, // internal app storage path
    val fileName: String? = null,
    val mimeType: String? = null,
    val uuid: String = UuidGenerator.newUuid(),
    val serverId: String? = null,
    val deviceId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

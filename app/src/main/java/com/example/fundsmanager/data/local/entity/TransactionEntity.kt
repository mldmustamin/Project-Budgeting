package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fundsmanager.data.local.ApprovalStatus
import com.example.fundsmanager.data.local.FinanceStatus
import com.example.fundsmanager.data.local.SyncStatus
import com.example.fundsmanager.util.UuidGenerator

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["projectId"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["legacyHash"], unique = true),
        Index(value = ["uuid"], unique = true),
        Index(value = ["syncStatus"]),
        Index(value = ["serverId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val projectId: Long,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val type: TransactionType,
    val date: String, // ISO 8601: yyyy-MM-dd
    val description: String,
    val reportedAmount: Long,
    val realAmount: Long,
    val sourceText: String? = null,
    val note: String? = null,
    val legacyHash: String? = null,
    val uuid: String = UuidGenerator.newUuid(),
    val serverId: String? = null,
    val deviceId: String? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val approvalStatus: String = ApprovalStatus.DRAFT,
    val financeStatus: String = FinanceStatus.ACTIVE,
    val lastSyncedAt: Long? = null,
    val sessionId: String? = null,
    val serverUserId: String? = null,
    val userUuid: String? = null,
    val projectUuid: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

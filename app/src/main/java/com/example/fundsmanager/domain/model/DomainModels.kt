package com.example.fundsmanager.domain.model

enum class TransactionType {
    FUND_IN,
    OFFICE_EXPENSE,
    PERSONAL_EXPENSE
}

data class User(
    val id: Long,
    val name: String,
    val email: String?,
    val uuid: String = "",
    val serverId: String? = null,
    val serverUserId: String? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long? = null
)

data class Project(
    val id: Long,
    val name: String,
    val description: String?,
    val isArchived: Boolean,
    val startAt: Long = 0L,
    val completedAt: Long? = null,
    val uuid: String = "",
    val serverId: String? = null,
    val deviceId: String? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

data class Account(
    val id: Long,
    val name: String,
    val description: String?
)

data class Category(
    val id: Long,
    val name: String,
    val description: String?
)

data class Transaction(
    val id: Long,
    val userId: Long,
    val projectId: Long,
    val accountId: Long?,
    val categoryId: Long?,
    val type: TransactionType,
    val date: String,
    val description: String,
    val reportedAmount: Long,
    val realAmount: Long,
    val sourceText: String?,
    val note: String?,
    val legacyHash: String?,
    val uuid: String = "",
    val serverId: String? = null,
    val deviceId: String? = null,
    val syncStatus: String = "PENDING",
    val approvalStatus: String = "DRAFT",
    val financeStatus: String = "ACTIVE",
    val lastSyncedAt: Long? = null,
    val sessionId: String? = null,
    val serverUserId: String? = null,
    val userUuid: String? = null,
    val projectUuid: String? = null,
    val deletedAt: Long? = null
)

data class Attachment(
    val id: Long,
    val transactionId: Long,
    val filePath: String,
    val fileName: String?,
    val mimeType: String?,
    val uuid: String = "",
    val serverId: String? = null,
    val deviceId: String? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long? = null,
    val createdAt: Long
)

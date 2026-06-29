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

// === Budget Request Domain Models ===

data class BudgetTask(
    val id: Long = 0,
    val uuid: String = "",
    val projectId: Long,
    val locationId: Long? = null,
    val taskNo: String,
    val vid: String,
    val taskName: String? = null,
    val remoteName: String? = null,
    val jobType: String,
    val stage: String = "DRAFT",
    val submittedBy: Long,
    val forwardedBy: Long? = null,
    val approvedBy: Long? = null,
    val verifiedBy: Long? = null,
    val reconciledBy: Long? = null,
    val totalEstimated: Long = 0,
    val totalRevised: Long = 0,
    val totalApproved: Long = 0,
    val totalRealization: Long = 0,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val completedAt: Long? = null,
    val deadlineAt: Long? = null,
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ExpenseItemModel(
    val id: Long = 0,
    val uuid: String = "",
    val taskExpenseId: Long = 0,
    val templateId: Long? = null,
    val tanggal: String,
    val note: String? = null,
    val estimatedAmount: Long = 0,
    val revisedAmount: Long? = null,
    val approvedAmount: Long? = null,
    val realizationAmount: Long? = null,
    val buktiPath: String? = null,
    val requiresBill: Boolean = false,
    val billVerified: Boolean = false,
    val itemStatus: String = "DRAFT",
    val rejectionReason: String? = null,
    val sortOrder: Int = 0
)

data class BudgetTemplate(
    val id: Long = 0,
    val uuid: String,
    val categoryName: String,
    val categoryGroup: String,
    val paguType: String,
    val paguAmount: Long? = null,
    val paguNote: String? = null,
    val requiresBill: Boolean = false,
    val billNote: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

data class MasterLocation(
    val id: Long = 0,
    val uuid: String,
    val projectId: Long,
    val remoteName: String,
    val address: String,
    val provinsi: String? = null,
    val kotaKab: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

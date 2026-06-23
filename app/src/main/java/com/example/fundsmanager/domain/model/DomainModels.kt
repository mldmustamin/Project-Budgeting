package com.example.fundsmanager.domain.model

enum class TransactionType {
    FUND_IN,
    OFFICE_EXPENSE,
    PERSONAL_EXPENSE
}

data class User(
    val id: Long,
    val name: String,
    val email: String?
)

data class Project(
    val id: Long,
    val name: String,
    val description: String?,
    val isArchived: Boolean
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
    val deletedAt: Long? = null
)

data class Attachment(
    val id: Long,
    val transactionId: Long,
    val filePath: String,
    val fileName: String?,
    val mimeType: String?,
    val createdAt: Long
)

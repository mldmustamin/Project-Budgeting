package com.example.fundsmanager.data.mapper

import com.example.fundsmanager.data.local.entity.*
import com.example.fundsmanager.domain.model.*

fun ProjectEntity.toDomain(): Project = Project(id, name, description, isArchived, startAt, completedAt, createdAt, updatedAt, deletedAt)
fun Project.toEntity(userId: Long): ProjectEntity = ProjectEntity(id, userId, name, description, isArchived, startAt, completedAt, createdAt, updatedAt, deletedAt)

fun AccountEntity.toDomain(): Account = Account(id, name, description)
fun Account.toEntity(): AccountEntity = AccountEntity(id, name, description)

fun CategoryEntity.toDomain(): Category = Category(id, name, description)
fun Category.toEntity(): CategoryEntity = CategoryEntity(id, name, description)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id, userId, projectId, accountId, categoryId, 
    com.example.fundsmanager.domain.model.TransactionType.valueOf(this.type.name),
    date, description, reportedAmount, realAmount, 
    sourceText, note, legacyHash, deletedAt
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id, userId, projectId, accountId, categoryId,
    com.example.fundsmanager.data.local.entity.TransactionType.valueOf(this.type.name),
    date, description, reportedAmount, realAmount,
    sourceText, note, legacyHash, deletedAt = deletedAt
)

fun AttachmentEntity.toDomain(): Attachment = Attachment(id, transactionId, filePath, fileName, mimeType, createdAt)

fun Attachment.toEntity(): AttachmentEntity = AttachmentEntity(id, transactionId, filePath, fileName, mimeType, createdAt)

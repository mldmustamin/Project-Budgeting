package com.example.fundsmanager.data.mapper

import com.example.fundsmanager.data.local.entity.*
import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.util.UuidGenerator

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    description = description,
    isArchived = isArchived,
    startAt = startAt,
    completedAt = completedAt,
    uuid = uuid,
    serverId = serverId,
    deviceId = deviceId,
    syncStatus = syncStatus,
    lastSyncedAt = lastSyncedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun Project.toEntity(userId: Long): ProjectEntity = ProjectEntity(
    id = id,
    userId = userId,
    name = name,
    description = description,
    isArchived = isArchived,
    startAt = startAt,
    completedAt = completedAt,
    uuid = uuid.ifBlank { UuidGenerator.newUuid() },
    serverId = serverId,
    deviceId = deviceId,
    syncStatus = syncStatus,
    lastSyncedAt = lastSyncedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun AccountEntity.toDomain(): Account = Account(id, name, description)
fun Account.toEntity(): AccountEntity = AccountEntity(id, name, description)

fun CategoryEntity.toDomain(): Category = Category(id, name, description)
fun Category.toEntity(): CategoryEntity = CategoryEntity(id, name, description)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    userId = userId,
    projectId = projectId,
    accountId = accountId,
    categoryId = categoryId,
    type = com.example.fundsmanager.domain.model.TransactionType.valueOf(type.name),
    date = date,
    description = description,
    reportedAmount = reportedAmount,
    realAmount = realAmount,
    sourceText = sourceText,
    note = note,
    legacyHash = legacyHash,
    uuid = uuid,
    serverId = serverId,
    deviceId = deviceId,
    syncStatus = syncStatus,
    approvalStatus = approvalStatus,
    financeStatus = financeStatus,
    lastSyncedAt = lastSyncedAt,
    sessionId = sessionId,
    serverUserId = serverUserId,
    userUuid = userUuid,
    projectUuid = projectUuid,
    deletedAt = deletedAt
)

fun Transaction.toEntity(
    resolvedProjectUuid: String? = null,
    resolvedUserUuid: String? = null
): TransactionEntity = TransactionEntity(
    id = id,
    userId = userId,
    projectId = projectId,
    accountId = accountId,
    categoryId = categoryId,
    type = com.example.fundsmanager.data.local.entity.TransactionType.valueOf(type.name),
    date = date,
    description = description,
    reportedAmount = reportedAmount,
    realAmount = realAmount,
    sourceText = sourceText,
    note = note,
    legacyHash = legacyHash,
    uuid = uuid.ifBlank { UuidGenerator.newUuid() },
    serverId = serverId,
    deviceId = deviceId,
    syncStatus = syncStatus,
    approvalStatus = approvalStatus,
    financeStatus = financeStatus,
    lastSyncedAt = lastSyncedAt,
    sessionId = sessionId,
    serverUserId = serverUserId,
    userUuid = userUuid ?: resolvedUserUuid,
    projectUuid = projectUuid ?: resolvedProjectUuid,
    deletedAt = deletedAt
)

fun AttachmentEntity.toDomain(): Attachment = Attachment(
    id = id,
    transactionId = transactionId,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    uuid = uuid,
    serverId = serverId,
    deviceId = deviceId,
    syncStatus = syncStatus,
    lastSyncedAt = lastSyncedAt,
    createdAt = createdAt
)

fun Attachment.toEntity(): AttachmentEntity = AttachmentEntity(
    id = id,
    transactionId = transactionId,
    filePath = filePath,
    fileName = fileName,
    mimeType = mimeType,
    uuid = uuid.ifBlank { UuidGenerator.newUuid() },
    serverId = serverId,
    deviceId = deviceId,
    syncStatus = syncStatus,
    lastSyncedAt = lastSyncedAt,
    createdAt = createdAt
)

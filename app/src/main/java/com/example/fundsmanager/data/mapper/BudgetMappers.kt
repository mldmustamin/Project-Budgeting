package com.example.fundsmanager.data.mapper

import com.example.fundsmanager.data.local.entity.BudgetTemplateEntity
import com.example.fundsmanager.data.local.entity.ExpenseItemEntity
import com.example.fundsmanager.data.local.entity.MasterLocationEntity
import com.example.fundsmanager.data.local.entity.TaskExpenseEntity
import com.example.fundsmanager.domain.model.BudgetTask
import com.example.fundsmanager.domain.model.BudgetTemplate
import com.example.fundsmanager.domain.model.ExpenseItemModel
import com.example.fundsmanager.domain.model.MasterLocation

fun TaskExpenseEntity.toDomain(items: List<ExpenseItemModel> = emptyList()) = BudgetTask(
    id = id, uuid = uuid, projectId = projectId, locationId = locationId,
    taskNo = taskNo, vid = vid, taskName = taskName, remoteName = remoteName,
    jobType = jobType, stage = stage, submittedBy = submittedBy,
    forwardedBy = forwardedBy, approvedBy = approvedBy, verifiedBy = verifiedBy,
    reconciledBy = reconciledBy, totalEstimated = totalEstimated,
    totalRevised = totalRevised, totalApproved = totalApproved,
    totalRealization = totalRealization, rejectionReason = rejectionReason,
    notes = notes, completedAt = completedAt, deadlineAt = deadlineAt,
    syncStatus = syncStatus, lastSyncedAt = lastSyncedAt,
    createdAt = createdAt, updatedAt = updatedAt
)

fun BudgetTask.toEntity() = TaskExpenseEntity(
    id = id, uuid = uuid, projectId = projectId, locationId = locationId,
    taskNo = taskNo, vid = vid, taskName = taskName, remoteName = remoteName,
    jobType = jobType, stage = stage, submittedBy = submittedBy,
    forwardedBy = forwardedBy, approvedBy = approvedBy, verifiedBy = verifiedBy,
    reconciledBy = reconciledBy, totalEstimated = totalEstimated,
    totalRevised = totalRevised, totalApproved = totalApproved,
    totalRealization = totalRealization, rejectionReason = rejectionReason,
    notes = notes, completedAt = completedAt, deadlineAt = deadlineAt,
    syncStatus = syncStatus, lastSyncedAt = lastSyncedAt,
    createdAt = createdAt, updatedAt = updatedAt
)

fun ExpenseItemEntity.toDomain() = ExpenseItemModel(
    id = id, uuid = uuid, taskExpenseId = taskExpenseId, templateId = templateId,
    tanggal = tanggal, note = note, estimatedAmount = estimatedAmount,
    revisedAmount = revisedAmount, approvedAmount = approvedAmount,
    realizationAmount = realizationAmount, buktiPath = buktiPath,
    requiresBill = requiresBill, billVerified = billVerified,
    itemStatus = itemStatus, rejectionReason = rejectionReason, sortOrder = sortOrder
)

fun ExpenseItemModel.toEntity(taskExpenseId: Long) = ExpenseItemEntity(
    id = id, uuid = uuid, taskExpenseId = taskExpenseId, templateId = templateId,
    tanggal = tanggal, note = note, estimatedAmount = estimatedAmount,
    revisedAmount = revisedAmount, approvedAmount = approvedAmount,
    realizationAmount = realizationAmount, buktiPath = buktiPath,
    requiresBill = requiresBill, billVerified = billVerified,
    itemStatus = itemStatus, rejectionReason = rejectionReason, sortOrder = sortOrder
)

fun BudgetTemplateEntity.toDomain() = BudgetTemplate(
    id = id, uuid = uuid, categoryName = categoryName, categoryGroup = categoryGroup,
    paguType = paguType, paguAmount = paguAmount, paguNote = paguNote,
    requiresBill = requiresBill, billNote = billNote,
    displayOrder = displayOrder, isActive = isActive
)

fun MasterLocationEntity.toDomain() = MasterLocation(
    id = id, uuid = uuid, projectId = projectId, remoteName = remoteName,
    address = address, provinsi = provinsi, kotaKab = kotaKab,
    latitude = latitude, longitude = longitude
)

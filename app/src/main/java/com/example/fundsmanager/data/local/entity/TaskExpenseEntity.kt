package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fundsmanager.util.UuidGenerator

@Entity(
    tableName = "task_expenses",
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["submittedBy"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["submittedBy"]),
        Index(value = ["stage"]),
        Index(value = ["projectId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["taskNo", "vid"], unique = true)
    ]
)
data class TaskExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UuidGenerator.newUuid(),
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
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

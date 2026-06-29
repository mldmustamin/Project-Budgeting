package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fundsmanager.util.UuidGenerator

@Entity(
    tableName = "expense_items",
    foreignKeys = [
        ForeignKey(entity = TaskExpenseEntity::class, parentColumns = ["id"], childColumns = ["taskExpenseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["taskExpenseId"])
    ]
)
data class ExpenseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UuidGenerator.newUuid(),
    val taskExpenseId: Long,
    val templateId: Long? = null,
    val tanggal: String, // ISO 8601: yyyy-MM-dd
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
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

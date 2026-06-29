package com.example.fundsmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_templates")
data class BudgetTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

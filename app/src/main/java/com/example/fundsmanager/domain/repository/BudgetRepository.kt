package com.example.fundsmanager.domain.repository

import com.example.fundsmanager.domain.model.BudgetTask
import com.example.fundsmanager.domain.model.ExpenseItemModel
import com.example.fundsmanager.domain.model.BudgetTemplate
import com.example.fundsmanager.domain.model.MasterLocation
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    // Task Expenses
    fun getMyTasks(userId: Long): Flow<List<BudgetTask>>
    fun getTasksByStage(stage: String): Flow<List<BudgetTask>>
    suspend fun getTaskByUuid(uuid: String): BudgetTask?
    suspend fun createDraft(task: BudgetTask, items: List<ExpenseItemModel>): BudgetTask
    suspend fun updateDraft(task: BudgetTask, items: List<ExpenseItemModel>): BudgetTask
    suspend fun deleteDraft(taskId: Long)
    suspend fun submitTask(taskId: Long): BudgetTask

    // Templates
    fun getTemplates(): Flow<List<BudgetTemplate>>
    suspend fun refreshTemplates()

    // Locations
    fun getLocations(projectId: Long): Flow<List<MasterLocation>>
    suspend fun refreshLocations()
}

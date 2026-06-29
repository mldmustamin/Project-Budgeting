package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.BudgetTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetTemplateDao {
    @Query("SELECT * FROM budget_templates WHERE isActive = 1 ORDER BY displayOrder ASC")
    fun getAll(): Flow<List<BudgetTemplateEntity>>

    @Query("SELECT * FROM budget_templates WHERE isActive = 1 ORDER BY displayOrder ASC")
    suspend fun getAllSync(): List<BudgetTemplateEntity>

    @Query("SELECT * FROM budget_templates WHERE paguType = :paguType AND isActive = 1")
    fun getByPaguType(paguType: String): Flow<List<BudgetTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<BudgetTemplateEntity>)

    @Query("DELETE FROM budget_templates")
    suspend fun deleteAll()
}

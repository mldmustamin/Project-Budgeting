package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.ExpenseItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseItemDao {
    @Query("SELECT * FROM expense_items WHERE taskExpenseId = :taskId ORDER BY sortOrder ASC")
    fun getByTask(taskId: Long): Flow<List<ExpenseItemEntity>>

    @Query("SELECT * FROM expense_items WHERE taskExpenseId = :taskId ORDER BY sortOrder ASC")
    suspend fun getByTaskSync(taskId: Long): List<ExpenseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExpenseItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExpenseItemEntity>)

    @Update
    suspend fun update(item: ExpenseItemEntity)

    @Query("DELETE FROM expense_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM expense_items WHERE taskExpenseId = :taskId AND id NOT IN (:keptIds)")
    suspend fun deleteNotIn(taskId: Long, keptIds: List<Long>)
}

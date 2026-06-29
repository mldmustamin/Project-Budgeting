package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.TaskExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskExpenseDao {
    @Query("SELECT * FROM task_expenses WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TaskExpenseEntity>>

    @Query("SELECT * FROM task_expenses WHERE submittedBy = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getByUser(userId: Long): Flow<List<TaskExpenseEntity>>

    @Query("SELECT * FROM task_expenses WHERE stage = :stage AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getByStage(stage: String): Flow<List<TaskExpenseEntity>>

    @Query("SELECT * FROM task_expenses WHERE submittedBy = :userId AND stage = 'DRAFT' AND deletedAt IS NULL")
    fun getDraftsByUser(userId: Long): Flow<List<TaskExpenseEntity>>

    @Query("SELECT COUNT(*) FROM task_expenses WHERE submittedBy = :userId AND stage = 'DRAFT' AND deletedAt IS NULL")
    suspend fun draftCount(userId: Long): Int

    @Query("SELECT * FROM task_expenses WHERE uuid = :uuid AND deletedAt IS NULL LIMIT 1")
    suspend fun getByUuid(uuid: String): TaskExpenseEntity?

    @Query("SELECT * FROM task_expenses WHERE syncStatus = 'PENDING' AND deletedAt IS NULL ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingSync(limit: Int = 20): List<TaskExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskExpenseEntity>)

    @Update
    suspend fun update(task: TaskExpenseEntity)

    @Query("UPDATE task_expenses SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE task_expenses SET syncStatus = :status, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis())
}

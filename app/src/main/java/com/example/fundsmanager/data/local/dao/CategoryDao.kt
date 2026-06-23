package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("UPDATE categories SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteCategory(id: Long, deletedAt: Long = System.currentTimeMillis())
}

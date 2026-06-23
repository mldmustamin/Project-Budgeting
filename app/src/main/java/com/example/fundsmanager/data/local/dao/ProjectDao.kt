package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE deletedAt IS NULL ORDER BY isArchived ASC, updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id AND deletedAt IS NULL")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE name = :name AND deletedAt IS NULL LIMIT 1")
    suspend fun getProjectByName(name: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteProject(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchiveStatus(id: Long, isArchived: Boolean)
}

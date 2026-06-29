package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.MasterLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterLocationDao {
    @Query("SELECT * FROM master_locations WHERE projectId = :projectId ORDER BY remoteName ASC")
    fun getByProject(projectId: Long): Flow<List<MasterLocationEntity>>

    @Query("SELECT * FROM master_locations ORDER BY remoteName ASC")
    suspend fun getAllSync(): List<MasterLocationEntity>

    @Query("SELECT * FROM master_locations WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): MasterLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<MasterLocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: MasterLocationEntity)

    @Query("DELETE FROM master_locations")
    suspend fun deleteAll()
}

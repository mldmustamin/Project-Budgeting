package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE deletedAt IS NULL")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id AND deletedAt IS NULL")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteUser(id: Long, deletedAt: Long = System.currentTimeMillis())
}

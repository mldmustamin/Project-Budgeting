package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE name = :name AND deletedAt IS NULL LIMIT 1")
    suspend fun getAccountByName(name: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("UPDATE accounts SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteAccount(id: Long, deletedAt: Long = System.currentTimeMillis())
}

package com.example.fundsmanager.data.local.dao

import androidx.room.*
import com.example.fundsmanager.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE projectId = :projectId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByProject(projectId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE projectId = :projectId AND deletedAt IS NULL")
    suspend fun getTransactionsByProjectSync(projectId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE legacyHash = :hash AND deletedAt IS NULL LIMIT 1")
    suspend fun getTransactionByHash(hash: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, deletedAt: Long = System.currentTimeMillis())
}

package com.example.fundsmanager.domain.repository

import com.example.fundsmanager.domain.model.*
import kotlinx.coroutines.flow.Flow

interface FundsRepository {
    // Project
    suspend fun getProjectById(id: Long): Project?
    fun getAllProjects(): Flow<List<Project>>
    suspend fun insertProject(userId: Long, project: Project): Long
    suspend fun getOrCreateProject(userId: Long, name: String): Project
    suspend fun renameProject(id: Long, name: String)
    suspend fun setProjectArchived(id: Long, isArchived: Boolean)
    suspend fun softDeleteProject(id: Long)
    
    // Transaction
    suspend fun getTransactionsByProject(projectId: Long): List<Transaction>
    suspend fun getTransactionByHash(hash: String): Transaction?
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun softDeleteTransaction(id: Long)
    
    // Account
    suspend fun getAccountByName(name: String): Account?
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun insertAccount(account: Account): Long
    suspend fun getOrCreateAccount(userId: Long, name: String): Account
    suspend fun getDefaultCashAccount(userId: Long): Account
    
    // Category
    fun getAllCategories(): Flow<List<Category>>
    
    // Attachment
    fun getAttachmentsByTransaction(transactionId: Long): Flow<List<Attachment>>
    suspend fun getTransactionIdsWithAttachments(transactionIds: List<Long>): Set<Long>
    suspend fun insertAttachment(attachment: Attachment): Long
    
    // Atomic Import
    suspend fun runInTransaction(block: suspend () -> Unit)
}

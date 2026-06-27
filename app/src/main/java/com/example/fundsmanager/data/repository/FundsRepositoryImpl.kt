package com.example.fundsmanager.data.repository

import androidx.room.withTransaction
import com.example.fundsmanager.data.local.ActiveSession
import com.example.fundsmanager.data.local.AppDatabase
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.data.local.dao.*
import com.example.fundsmanager.data.sync.SyncOutboxRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.fundsmanager.data.local.entity.AuditLogEntity
import com.example.fundsmanager.data.local.entity.ProjectEntity
import com.example.fundsmanager.data.local.entity.TransactionEntity
import com.example.fundsmanager.data.local.entity.AccountEntity
import com.example.fundsmanager.data.mapper.*
import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FundsRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val userDao: UserDao,
    private val projectDao: ProjectDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao,
    private val auditLogDao: AuditLogDao,
    private val syncOutboxRepository: SyncOutboxRepository,
    private val sessionManager: SessionManager
) : FundsRepository {

    override suspend fun getProjectById(id: Long): Project? {
        return projectDao.getProjectById(id)?.toDomain()
    }

    override fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertProject(userId: Long, project: Project): Long {
        return database.withTransaction {
            val id = projectDao.insertProject(project.toEntity(userId))
            if (id > 0) {
                insertAuditLog(
                    userId = userId,
                    entityType = ENTITY_PROJECT,
                    entityId = id,
                    action = ACTION_CREATE,
                    newValueJson = project.toEntity(userId).copy(id = id).toAuditJson()
                )
            }
            id
        }
    }

    override suspend fun getOrCreateProject(userId: Long, name: String): Project {
        val existing = projectDao.getProjectByName(name)
        if (existing != null) return existing.toDomain()
        
        val newProject = ProjectEntity(userId = userId, name = name)
        val id = projectDao.insertProject(newProject)
        if (id > 0) {
            insertAuditLog(
                userId = userId,
                entityType = ENTITY_PROJECT,
                entityId = id,
                action = ACTION_CREATE,
                newValueJson = newProject.copy(id = id).toAuditJson()
            )
        }
        return newProject.copy(id = id).toDomain()
    }

    override suspend fun renameProject(id: Long, name: String) {
        database.withTransaction {
            val old = projectDao.getProjectById(id) ?: return@withTransaction
            val renamed = old.copy(name = name.trim(), updatedAt = System.currentTimeMillis())
            projectDao.updateProject(renamed)
            insertAuditLog(
                userId = old.userId,
                entityType = ENTITY_PROJECT,
                entityId = id,
                action = ACTION_RENAME,
                oldValueJson = old.toAuditJson(),
                newValueJson = renamed.toAuditJson()
            )
        }
    }

    override suspend fun updateProjectSchedule(id: Long, startAt: Long, completedAt: Long?) {
        database.withTransaction {
            val old = projectDao.getProjectById(id) ?: return@withTransaction
            val updated = old.copy(
                startAt = startAt,
                completedAt = completedAt,
                updatedAt = System.currentTimeMillis()
            )
            projectDao.updateProject(updated)
            insertAuditLog(
                userId = old.userId,
                entityType = ENTITY_PROJECT,
                entityId = id,
                action = ACTION_UPDATE,
                oldValueJson = old.toAuditJson(),
                newValueJson = updated.toAuditJson()
            )
        }
    }

    override suspend fun setProjectArchived(id: Long, isArchived: Boolean) {
        database.withTransaction {
            val old = projectDao.getProjectById(id)
            projectDao.updateArchiveStatus(id, isArchived)
            old?.let {
                insertAuditLog(
                    userId = it.userId,
                    entityType = ENTITY_PROJECT,
                    entityId = id,
                    action = if (isArchived) ACTION_ARCHIVE else ACTION_UNARCHIVE,
                    oldValueJson = it.toAuditJson(),
                    newValueJson = it.copy(isArchived = isArchived).toAuditJson()
                )
            }
        }
    }

    override suspend fun softDeleteProject(id: Long) {
        database.withTransaction {
            val old = projectDao.getProjectById(id)
            val deletedAt = System.currentTimeMillis()
            projectDao.softDeleteProject(id, deletedAt)
            old?.let {
                insertAuditLog(
                    userId = it.userId,
                    entityType = ENTITY_PROJECT,
                    entityId = id,
                    action = ACTION_SOFT_DELETE,
                    oldValueJson = it.toAuditJson(),
                    newValueJson = it.copy(deletedAt = deletedAt).toAuditJson()
                )
            }
        }
    }

    override suspend fun getTransactionsByProject(projectId: Long): List<Transaction> {
        return transactionDao.getTransactionsByProjectSync(projectId).map { it.toDomain() }
    }

    override suspend fun getAllTransactions(): List<Transaction> {
        return transactionDao.getAllTransactionsSync().map { it.toDomain() }
    }

    override suspend fun getTransactionByHash(hash: String): Transaction? {
        return transactionDao.getTransactionByHash(hash)?.toDomain()
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        database.withTransaction {
            val entities = buildList {
                for (tx in transactions) {
                    add(resolveTransactionEntity(tx))
                }
            }
            val insertedIds = transactionDao.insertTransactions(entities)
            transactions.zip(insertedIds).forEach { (transaction, id) ->
                if (id > 0) {
                    val inserted = transaction.copy(id = id)
                    insertAuditLog(
                        userId = inserted.userId,
                        entityType = ENTITY_TRANSACTION,
                        entityId = id,
                        action = ACTION_CREATE,
                        newValueJson = inserted.toAuditJson()
                    )
                }
            }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return database.withTransaction {
            val id = transactionDao.insertTransaction(resolveTransactionEntity(transaction))
            if (id > 0) {
                insertAuditLog(
                    userId = transaction.userId,
                    entityType = ENTITY_TRANSACTION,
                    entityId = id,
                    action = ACTION_CREATE,
                    newValueJson = transaction.copy(id = id).toAuditJson()
                )
            }
            id
        }.also { newId ->
            if (newId > 0) enqueueOutbox(transaction.copy(id = newId), "CREATE")
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        database.withTransaction {
            val old = transactionDao.getTransactionById(transaction.id)
            transactionDao.updateTransaction(resolveTransactionEntity(transaction))
            insertAuditLog(
                userId = transaction.userId,
                entityType = ENTITY_TRANSACTION,
                entityId = transaction.id,
                action = ACTION_UPDATE,
                oldValueJson = old?.toDomain()?.toAuditJson(),
                newValueJson = transaction.toAuditJson()
            )
        }
        enqueueOutbox(transaction, "UPDATE")
    }

    override suspend fun softDeleteTransaction(id: Long) {
        var oldTransaction: Transaction? = null
        database.withTransaction {
            val old = transactionDao.getTransactionById(id)
            val deletedAt = System.currentTimeMillis()
            transactionDao.softDeleteTransaction(id, deletedAt)
            old?.let {
                oldTransaction = it.toDomain()
                insertAuditLog(
                    userId = it.userId,
                    entityType = ENTITY_TRANSACTION,
                    entityId = id,
                    action = ACTION_SOFT_DELETE,
                    oldValueJson = it.toDomain().toAuditJson(),
                    newValueJson = it.toDomain().copy(deletedAt = deletedAt).toAuditJson()
                )
            }
        }
        oldTransaction?.let { enqueueOutbox(it, "SOFT_DELETE") }
    }

    private fun enqueueOutbox(tx: Transaction, operation: String) {
        val json = Json { ignoreUnknownKeys = true }
        val payloadJson = json.encodeToString(tx.toAuditJson())
        val session: ActiveSession? = kotlinx.coroutines.runBlocking {
            try {
                sessionManager.activeSession.first()
            } catch (_: Exception) {
                null
            }
        }
        kotlinx.coroutines.runBlocking {
            syncOutboxRepository.enqueue(
                localUserId = tx.userId,
                userUuid = session?.userUuid ?: tx.userUuid ?: "",
                deviceId = session?.deviceUuid ?: tx.deviceId ?: "",
                entityUuid = tx.uuid.ifBlank { "" },
                operation = operation,
                payloadJson = payloadJson
            )
        }
    }

    override suspend fun getAccountByName(name: String): Account? {
        return accountDao.getAccountByName(name)?.toDomain()
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.getAccountById(account.id)?.let { existing ->
            accountDao.updateAccount(
                existing.copy(
                    name = account.name.trim(),
                    description = account.description,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun softDeleteAccount(id: Long) {
        accountDao.softDeleteAccount(id)
    }

    override suspend fun getOrCreateAccount(userId: Long, name: String): Account {
        val existing = accountDao.getAccountByName(name)
        if (existing != null) return existing.toDomain()
        
        val newAccount = AccountEntity(name = name)
        val id = accountDao.insertAccount(newAccount)
        return newAccount.copy(id = id).toDomain()
    }

    override suspend fun getDefaultCashAccount(userId: Long): Account {
        val name = "Cash in Hand"
        val existing = accountDao.getAccountByName(name)
        if (existing != null) return existing.toDomain()
        
        val newAccount = AccountEntity(name = name)
        val id = accountDao.insertAccount(newAccount)
        return newAccount.copy(id = id).toDomain()
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> 
            list.map { Category(it.id, it.name, it.description) } 
        }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.getCategoryById(category.id)?.let { existing ->
            categoryDao.updateCategory(
                existing.copy(
                    name = category.name.trim(),
                    description = category.description,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun softDeleteCategory(id: Long) {
        categoryDao.softDeleteCategory(id)
    }

    override fun getAttachmentsByTransaction(transactionId: Long): Flow<List<Attachment>> {
        return attachmentDao.getAttachmentsByTransaction(transactionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionIdsWithAttachments(transactionIds: List<Long>): Set<Long> {
        if (transactionIds.isEmpty()) return emptySet()
        return attachmentDao.getTransactionIdsWithAttachments(transactionIds).toSet()
    }

    override suspend fun insertAttachment(attachment: Attachment): Long {
        return attachmentDao.insertAttachment(attachment.toEntity())
    }

    override suspend fun runInTransaction(block: suspend () -> Unit) {
        database.withTransaction {
            block()
        }
    }

    private suspend fun insertAuditLog(
        userId: Long,
        entityType: String,
        entityId: Long,
        action: String,
        oldValueJson: String? = null,
        newValueJson: String? = null
    ) {
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                entityType = entityType,
                entityId = entityId,
                action = action,
                oldValueJson = oldValueJson,
                newValueJson = newValueJson
            )
        )
    }

    private fun ProjectEntity.toAuditJson(): String {
        return """{"id":$id,"userId":$userId,"name":"${name.escapeJson()}","isArchived":$isArchived,"startAt":$startAt,"completedAt":${completedAt ?: "null"},"deletedAt":${deletedAt ?: "null"}}"""
    }

    private fun Transaction.toAuditJson(): String {
        return """{"id":$id,"userId":$userId,"projectId":$projectId,"type":"${type.name}","date":"${date.escapeJson()}","description":"${description.escapeJson()}","reportedAmount":$reportedAmount,"realAmount":$realAmount,"deletedAt":${deletedAt ?: "null"}}"""
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private suspend fun resolveTransactionEntity(transaction: Transaction): TransactionEntity {
        return transaction.toEntity(
            resolvedProjectUuid = projectDao.getProjectById(transaction.projectId)?.uuid,
            resolvedUserUuid = userDao.getUserById(transaction.userId)?.uuid
        )
    }

    private companion object {
        const val ENTITY_PROJECT = "project"
        const val ENTITY_TRANSACTION = "transaction"
        const val ACTION_CREATE = "create"
        const val ACTION_UPDATE = "update"
        const val ACTION_RENAME = "rename"
        const val ACTION_SOFT_DELETE = "soft_delete"
        const val ACTION_ARCHIVE = "archive"
        const val ACTION_UNARCHIVE = "unarchive"
    }
}

package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.Account
import com.example.fundsmanager.domain.model.Attachment
import com.example.fundsmanager.domain.model.Category
import com.example.fundsmanager.domain.model.Project
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateTransactionUseCaseTest {

    private lateinit var repository: FakeFundsRepository
    private lateinit var useCase: ValidateTransactionUseCase

    @Before
    fun setUp() {
        repository = FakeFundsRepository()
        useCase = ValidateTransactionUseCase(repository)
    }

    @Test
    fun `invalid calendar date is rejected`() = runTest {
        val result = useCase(transaction(date = "2023-02-30"))

        assertEquals(
            TransactionValidationResult.Invalid("Format tanggal harus yyyy-MM-dd"),
            result
        )
    }

    @Test
    fun `expense without description is rejected`() = runTest {
        val result = useCase(transaction(description = ""))

        assertEquals(
            TransactionValidationResult.Invalid("Keterangan wajib diisi untuk expense"),
            result
        )
    }

    @Test
    fun `matching active transaction returns duplicate warning`() = runTest {
        repository.transactions = listOf(
            transaction(id = 99, description = "Taxi  Airport")
        )

        val result = useCase(transaction(description = "taxi airport"))

        assertTrue(result is TransactionValidationResult.DuplicateWarning)
    }

    @Test
    fun `soft-deleted matching transaction is ignored`() = runTest {
        repository.transactions = listOf(
            transaction(id = 99, deletedAt = 123L)
        )

        val result = useCase(transaction())

        assertEquals(TransactionValidationResult.Valid, result)
    }

    @Test
    fun `editing same transaction does not flag itself as duplicate`() = runTest {
        repository.transactions = listOf(
            transaction(id = 5)
        )

        val result = useCase(transaction(id = 5))

        assertEquals(TransactionValidationResult.Valid, result)
    }

    private fun transaction(
        id: Long = 0,
        date: String = "2023-01-01",
        description: String = "Taxi Airport",
        deletedAt: Long? = null
    ): Transaction {
        return Transaction(
            id = id,
            userId = 1,
            projectId = 10,
            accountId = 20,
            categoryId = 30,
            type = TransactionType.OFFICE_EXPENSE,
            date = date,
            description = description,
            reportedAmount = 100_000,
            realAmount = 90_000,
            sourceText = null,
            note = null,
            legacyHash = null,
            deletedAt = deletedAt
        )
    }

    private class FakeFundsRepository : FundsRepository {
        var transactions: List<Transaction> = emptyList()

        override suspend fun getProjectById(id: Long): Project? = null
        override fun getAllProjects(): Flow<List<Project>> = emptyFlow()
        override suspend fun insertProject(userId: Long, project: Project): Long = 0
        override suspend fun getOrCreateProject(userId: Long, name: String): Project = error("Unused")
        override suspend fun renameProject(id: Long, name: String) = Unit
        override suspend fun setProjectArchived(id: Long, isArchived: Boolean) = Unit
        override suspend fun softDeleteProject(id: Long) = Unit
        override suspend fun getTransactionsByProject(projectId: Long): List<Transaction> = transactions
        override suspend fun getTransactionByHash(hash: String): Transaction? = null
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override suspend fun insertTransactions(transactions: List<Transaction>) = Unit
        override suspend fun insertTransaction(transaction: Transaction): Long = 0
        override suspend fun updateTransaction(transaction: Transaction) = Unit
        override suspend fun softDeleteTransaction(id: Long) = Unit
        override suspend fun getAccountByName(name: String): Account? = null
        override fun getAllAccounts(): Flow<List<Account>> = emptyFlow()
        override suspend fun insertAccount(account: Account): Long = 0
        override suspend fun getOrCreateAccount(userId: Long, name: String): Account = error("Unused")
        override suspend fun getDefaultCashAccount(userId: Long): Account = error("Unused")
        override fun getAllCategories(): Flow<List<Category>> = emptyFlow()
        override fun getAttachmentsByTransaction(transactionId: Long): Flow<List<Attachment>> = emptyFlow()
        override suspend fun getTransactionIdsWithAttachments(transactionIds: List<Long>): Set<Long> = emptySet()
        override suspend fun insertAttachment(attachment: Attachment): Long = 0
        override suspend fun runInTransaction(block: suspend () -> Unit) = block()
    }
}

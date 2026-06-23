package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class CalculateProjectSummaryUseCaseTest {

    @Mock
    private lateinit var repository: FundsRepository

    private lateinit var useCase: CalculateProjectSummaryUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = CalculateProjectSummaryUseCase(repository)
    }

    private fun createTx(id: Long, type: TransactionType, reported: Long, real: Long, deletedAt: Long? = null) = Transaction(
        id = id, userId = 1, projectId = 1, accountId = 1L, categoryId = null,
        type = type, date = "2023-01-01", reportedAmount = reported, realAmount = real,
        description = "Test", sourceText = null, note = null, legacyHash = null,
        deletedAt = deletedAt
    )

    @Test
    fun `invoke with normal transactions returns correct summary`() = runTest {
        val projectId = 1L
        val project = Project(id = projectId, name = "Test Project", description = null, isArchived = false)
        
        val transactions = listOf(
            createTx(1, TransactionType.FUND_IN, 1000000, 1000000),
            createTx(2, TransactionType.OFFICE_EXPENSE, 200000, 150000),
            createTx(3, TransactionType.PERSONAL_EXPENSE, 50000, 50000)
        )

        `when`(repository.getProjectById(projectId)).thenReturn(project)
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(transactions)

        val summary = useCase(projectId)

        // totalFundIn = 1,000,000
        // totalOfficeReported = 200,000
        // totalOfficeReal = 150,000
        // totalPersonalExpense = 50,000
        
        // saving = 200,000 - 150,000 = 50,000
        // remainingReported = 1,000,000 - 200,000 = 800,000
        // remainingReal = 1,000,000 - 150,000 = 850,000
        // totalCashOut = 150,000 + 50,000 = 200,000
        // netPosition = 1,000,000 - 200,000 = 800,000

        assertEquals(1000000L, summary?.totalFundIn)
        assertEquals(200000L, summary?.totalOfficeReported)
        assertEquals(150000L, summary?.totalOfficeReal)
        assertEquals(50000L, summary?.totalPersonalExpense)
        assertEquals(50000L, summary?.saving)
        assertEquals(800000L, summary?.remainingReported)
        assertEquals(850000L, summary?.remainingReal)
        assertEquals(200000L, summary?.totalCashOut)
        assertEquals(800000L, summary?.netPosition)
    }

    @Test
    fun `invoke ignores soft-deleted transactions`() = runTest {
        val projectId = 1L
        val project = Project(id = projectId, name = "Soft Delete Test", description = null, isArchived = false)
        
        val transactions = listOf(
            createTx(1, TransactionType.FUND_IN, 1000, 1000),
            createTx(2, TransactionType.OFFICE_EXPENSE, 200, 200, deletedAt = 12345L)
        )

        `when`(repository.getProjectById(projectId)).thenReturn(project)
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(transactions)

        val summary = useCase(projectId)

        assertEquals(1000L, summary?.totalFundIn)
        assertEquals(0L, summary?.totalOfficeReported)
        assertEquals(1000L, summary?.netPosition)
    }

    @Test
    fun `invoke with negative saving returns correct values`() = runTest {
        val projectId = 1L
        `when`(repository.getProjectById(projectId)).thenReturn(Project(1, "Negative Saving", null, false))
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(listOf(
            createTx(1, TransactionType.OFFICE_EXPENSE, 100000, 120000)
        ))

        val summary = useCase(projectId)
        assertEquals(100000L, summary?.totalOfficeReported)
        assertEquals(120000L, summary?.totalOfficeReal)
        assertEquals(-20000L, summary?.saving)
        assertEquals(120000L, summary?.totalCashOut)
        assertEquals(-120000L, summary?.netPosition)
    }

    @Test
    fun `invoke with zero fund returns negative netPosition`() = runTest {
        val projectId = 1L
        `when`(repository.getProjectById(projectId)).thenReturn(Project(1, "Zero Fund", null, false))
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(listOf(
            createTx(1, TransactionType.PERSONAL_EXPENSE, 100000, 100000)
        ))

        val summary = useCase(projectId)
        assertEquals(0L, summary?.totalFundIn)
        assertEquals(-100000L, summary?.netPosition)
    }

    @Test
    fun `invoke with empty transaction list returns zero summary`() = runTest {
        val projectId = 1L
        `when`(repository.getProjectById(projectId)).thenReturn(Project(1, "Empty", null, false))
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(emptyList())

        val summary = useCase(projectId)
        assertEquals(0L, summary?.totalFundIn)
        assertEquals(0L, summary?.netPosition)
        assertEquals(0L, summary?.saving)
    }

    @Test
    fun `invoke with missing project returns null`() = runTest {
        `when`(repository.getProjectById(99L)).thenReturn(null)
        val summary = useCase(99L)
        assertNull(summary)
    }
}

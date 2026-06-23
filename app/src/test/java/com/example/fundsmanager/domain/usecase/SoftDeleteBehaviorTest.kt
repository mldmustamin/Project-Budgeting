package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class SoftDeleteBehaviorTest {

    @Mock
    private lateinit var repository: FundsRepository

    private lateinit var calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        calculateProjectSummaryUseCase = CalculateProjectSummaryUseCase(repository)
    }

    @Test
    fun `ProjectSummary excludes soft-deleted transactions`() = runTest {
        val projectId = 1L
        val transactions = listOf(
            Transaction(1, 1, 1, 1, null, TransactionType.FUND_IN, "2023-01-01", "F", 100, 100, null, null, null, null),
            Transaction(2, 1, 1, 1, null, TransactionType.OFFICE_EXPENSE, "2023-01-01", "D", 50, 50, null, null, "h1", 12345L) // deleted
        )
        
        `when`(repository.getProjectById(projectId)).thenReturn(Project(1, "P", null, false))
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(transactions)
        
        val summary = calculateProjectSummaryUseCase(projectId)
        assertEquals(100L, summary?.totalFundIn)
        assertEquals(0L, summary?.totalOfficeReported)
    }
}

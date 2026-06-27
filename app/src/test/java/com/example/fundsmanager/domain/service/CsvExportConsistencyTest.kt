package com.example.fundsmanager.domain.service

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.usecase.CalculateProjectSummaryUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class CsvExportConsistencyTest {

    @Mock
    private lateinit var repository: FundsRepository
    
    @Mock
    private lateinit var calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase

    private lateinit var exportCsvUseCase: ExportCsvUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        exportCsvUseCase = ExportCsvUseCase(repository, calculateProjectSummaryUseCase)
    }

    @Test
    fun `CSV export excludes soft-deleted transactions and matches summary`() = runTest {
        val projectId = 1L
        val summary = ProjectSummary(1, "P", 100, 0, 0, 0, 0, 100, 100, 0, 100)
        val transactions = listOf(
            Transaction(1, 1, 1, 1, null, TransactionType.FUND_IN, "2023-01-01", "Valid Tx", 100, 100, null, null, null, deletedAt = null),
            Transaction(2, 1, 1, 1, null, TransactionType.OFFICE_EXPENSE, "2023-01-01", "Deleted Tx", 50, 50, null, null, null, deletedAt = 12345L)
        )

        `when`(calculateProjectSummaryUseCase(projectId)).thenReturn(summary)
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(transactions)

        val csv = exportCsvUseCase.execute(projectId)
        
        assertTrue(csv.contains("Valid Tx"))
        assertFalse(csv.contains("Deleted Tx"))
        assertTrue(csv.contains("Total Dana Masuk,100"))
    }
}

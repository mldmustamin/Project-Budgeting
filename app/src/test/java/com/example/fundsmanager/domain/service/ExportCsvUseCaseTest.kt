package com.example.fundsmanager.domain.service

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.usecase.CalculateProjectSummaryUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ExportCsvUseCaseTest {

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
    fun `execute returns CSV string with raw numeric columns and summary footer`() = runBlocking {
        val projectId = 1L
        val summary = ProjectSummary(
            projectId = projectId, projectName = "P",
            totalFundIn = 1000, totalOfficeReported = 200, totalOfficeReal = 150,
            totalPersonalExpense = 50, saving = 50, remainingReported = 800,
            remainingReal = 850, totalCashOut = 200, netPosition = 800
        )
        
        val transactions = listOf(
            Transaction(1, 1, projectId, null, null, TransactionType.FUND_IN, "2023-01-01", "F", 1000, 1000, null, null, null)
        )

        `when`(calculateProjectSummaryUseCase(projectId)).thenReturn(summary)
        `when`(repository.getTransactionsByProject(projectId)).thenReturn(transactions)

        val csv = exportCsvUseCase.execute(projectId)

        // Check header
        assertTrue(csv.contains("Tanggal,Keterangan,Jenis,Nominal Dilaporkan,Nominal Real,Selisih,Catatan"))
        // Check raw numeric data with user-facing transaction type label.
        assertTrue(csv.contains("2023-01-01,F,Transfer Dana,1000,1000,0,"))
        // Check summary footer
        assertTrue(csv.contains("RINGKASAN"))
        assertTrue(csv.contains("Total Dana Masuk,1000"))
        assertTrue(csv.contains("Posisi Bersih,800"))
    }
}

package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.argThat

class ImportTrackerDuitUseCaseTest {

    @Mock
    private lateinit var repository: FundsRepository

    private lateinit var useCase: ImportTrackerDuitUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = ImportTrackerDuitUseCase(repository)
    }

    @Test
    fun `getPreview parses top-level projects format correctly`() = runTest {
        val json = """
            {
              "version": 3,
              "projects": [
                {
                  "name": "Project X",
                  "fundEntries": [{"date": "2023-01-01", "amount": 500}]
                }
              ]
            }
        """.trimIndent()

        val preview = useCase.getPreview(json, 1L)

        assertEquals(1, preview.projectCount)
        assertEquals(1, preview.fundInCount)
        assertEquals(1, preview.validCount)
    }

    @Test
    fun `getPreview detects invalid dates and amounts`() = runTest {
        val json = """
            {
              "projects": [
                {
                  "name": "P",
                  "fundEntries": [
                    {"date": "invalid-date", "amount": 100},
                    {"date": "2023-01-01", "amount": 0}
                  ]
                }
              ]
            }
        """.trimIndent()

        val preview = useCase.getPreview(json, 1L)

        assertEquals(2, preview.invalidCount)
        assertEquals(0, preview.validCount)
        assertEquals(ImportItemStatus.INVALID, preview.items[0].status)
    }

    @Test
    fun `getPreview detects duplicates within the same file`() = runTest {
        val json = """
            {
              "projects": [
                {
                  "name": "P",
                  "fundEntries": [
                    {"date": "2023-01-01", "amount": 100},
                    {"date": "2023-01-01", "amount": 100}
                  ]
                }
              ]
            }
        """.trimIndent()

        val preview = useCase.getPreview(json, 1L)

        assertEquals(1, preview.validCount)
        assertEquals(1, preview.duplicateCount)
        assertEquals(ImportItemStatus.DUPLICATE, preview.items[1].status)
    }

    @Test
    fun `executeImport uses real repository calls and accounts`() = runTest {
        val items = listOf(
            ImportPreviewItem(
                legacyProjectName = "P", type = TransactionType.FUND_IN,
                date = "2023-01-01", description = "F", source = "Bank",
                reportedAmount = 100, realAmount = 100, note = null,
                legacyHash = "hash1", status = ImportItemStatus.VALID
            )
        )
        val preview = ImportPreview(1, 1, 0, 0, 1, 0, 0, items)

        val mockProject = Project(10L, "P", null, false)
        val mockAccount = Account(20L, "Bank", null)

        `when`(repository.getOrCreateProject(anyLong(), anyString())).thenReturn(mockProject)
        `when`(repository.getOrCreateAccount(anyLong(), anyString())).thenReturn(mockAccount)
        
        // Mock runInTransaction to execute immediately using runBlocking instead of runTest nested
        `when`(repository.runInTransaction(any())).thenAnswer { invocation ->
            val block = invocation.arguments[0] as (suspend () -> Unit)
            runBlocking { block() }
        }

        useCase.executeImport(preview, 1L)

        verify(repository).insertTransactions(argThat { txs ->
            txs.size == 1 && txs[0].projectId == 10L && txs[0].accountId == 20L
        })
    }
}

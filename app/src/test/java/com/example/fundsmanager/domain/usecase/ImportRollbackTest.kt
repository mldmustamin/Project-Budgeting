package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class ImportRollbackTest {

    @Mock
    private lateinit var repository: FundsRepository

    private lateinit var useCase: ImportTrackerDuitUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = ImportTrackerDuitUseCase(repository)
    }

    @Test(expected = RuntimeException::class)
    fun `executeImport rolls back if repository throws exception`() = runTest {
        val items = listOf(
            ImportPreviewItem(
                legacyProjectName = "P", type = TransactionType.FUND_IN,
                date = "2023-01-01", description = "F", source = "S",
                reportedAmount = 100, realAmount = 100, note = null,
                legacyHash = "h1", status = ImportItemStatus.VALID
            )
        )
        val preview = ImportPreview(1, 1, 0, 0, 1, 0, 0, items)

        // Mock runInTransaction to execute immediately and throw if inner fails
        `when`(repository.runInTransaction(any())).thenAnswer { invocation ->
            val block = invocation.arguments[0] as (suspend () -> Unit)
            runTest { block() }
        }
        
        `when`(repository.getOrCreateProject(anyLong(), anyString())).thenThrow(RuntimeException("Atomic Failure"))

        useCase.executeImport(preview, 1L)
    }
}

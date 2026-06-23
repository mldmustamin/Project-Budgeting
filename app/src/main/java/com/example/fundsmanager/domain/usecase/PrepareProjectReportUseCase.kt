package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.ProjectReportData
import com.example.fundsmanager.domain.model.TransactionReportRow
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.toUiLabel
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PrepareProjectReportUseCase @Inject constructor(
    private val repository: FundsRepository,
    private val calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase
) {
    suspend operator fun invoke(projectId: Long): ProjectReportData? {
        val project = repository.getProjectById(projectId) ?: return null
        val summary = calculateProjectSummaryUseCase(projectId) ?: return null
        val accounts = repository.getAllAccounts().first().associateBy { it.id }
        val categories = repository.getAllCategories().first().associateBy { it.id }
        val transactions = repository.getTransactionsByProject(projectId)
            .filter { it.deletedAt == null }
            .sortedWith(compareByDescending<com.example.fundsmanager.domain.model.Transaction> { it.date }.thenByDescending { it.id })
        val attachmentIds = repository.getTransactionIdsWithAttachments(transactions.map { it.id })

        return ProjectReportData(
            project = project,
            summary = summary,
            transactions = transactions.map { tx ->
                TransactionReportRow(
                    date = tx.date,
                    typeLabel = tx.type.toUiLabel(),
                    description = tx.description,
                    accountName = tx.accountId?.let { accounts[it]?.name }.orEmpty(),
                    categoryName = tx.categoryId?.let { categories[it]?.name },
                    reportedAmount = tx.reportedAmount,
                    realAmount = tx.realAmount,
                    saving = if (tx.type == TransactionType.OFFICE_EXPENSE) tx.reportedAmount - tx.realAmount else 0L,
                    note = tx.note,
                    receiptStatus = if (tx.id in attachmentIds) "Ada bukti" else "Belum ada bukti"
                )
            },
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        )
    }
}

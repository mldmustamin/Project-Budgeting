package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import javax.inject.Inject

class CalculateProjectSummaryUseCase @Inject constructor(
    private val repository: FundsRepository
) {
    suspend operator fun invoke(projectId: Long): ProjectSummary? {
        val project = repository.getProjectById(projectId) ?: return null
        val transactions = repository.getTransactionsByProject(projectId)
            .filter { it.deletedAt == null }

        var totalFundIn = 0L
        var totalOfficeReported = 0L
        var totalOfficeReal = 0L
        var totalPersonalExpense = 0L

        transactions.forEach { tx ->
            when (tx.type) {
                TransactionType.FUND_IN -> {
                    totalFundIn += tx.reportedAmount
                }
                TransactionType.OFFICE_EXPENSE -> {
                    totalOfficeReported += tx.reportedAmount
                    totalOfficeReal += tx.realAmount
                }
                TransactionType.PERSONAL_EXPENSE -> {
                    totalPersonalExpense += tx.realAmount
                }
            }
        }

        val saving = totalOfficeReported - totalOfficeReal
        val remainingReported = totalFundIn - totalOfficeReported
        val remainingReal = totalFundIn - totalOfficeReal
        val totalCashOut = totalOfficeReal + totalPersonalExpense
        val netPosition = totalFundIn - totalCashOut

        return ProjectSummary(
            projectId = projectId,
            projectName = project.name,
            totalFundIn = totalFundIn,
            totalOfficeReported = totalOfficeReported,
            totalOfficeReal = totalOfficeReal,
            totalPersonalExpense = totalPersonalExpense,
            saving = saving,
            remainingReported = remainingReported,
            remainingReal = remainingReal,
            totalCashOut = totalCashOut,
            netPosition = netPosition
        )
    }
}

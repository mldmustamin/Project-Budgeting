package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.ProjectSummary
import javax.inject.Inject

class CalculateOverallSummaryUseCase @Inject constructor() {
    operator fun invoke(summaries: List<ProjectSummary>): ProjectSummary? {
        if (summaries.isEmpty()) return null
        return ProjectSummary(
            projectId = 0L,
            projectName = "Semua Project",
            totalFundIn = summaries.sumOf { it.totalFundIn },
            totalOfficeReported = summaries.sumOf { it.totalOfficeReported },
            totalOfficeReal = summaries.sumOf { it.totalOfficeReal },
            totalPersonalExpense = summaries.sumOf { it.totalPersonalExpense },
            saving = summaries.sumOf { it.saving },
            remainingReported = summaries.sumOf { it.remainingReported },
            remainingReal = summaries.sumOf { it.remainingReal },
            totalCashOut = summaries.sumOf { it.totalCashOut },
            netPosition = summaries.sumOf { it.netPosition }
        )
    }
}

package com.example.fundsmanager.ui.screen.dashboard

import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.service.ReportFile

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val summary: ProjectSummary,
        val recentItems: List<Transaction>,
        val isExporting: Boolean = false,
        val exportMessage: String? = null,
        val exportError: String? = null,
        val pendingShareFile: ReportFile? = null
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

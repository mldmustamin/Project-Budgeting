package com.example.fundsmanager.ui.screen.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.service.ReportFileRepository
import com.example.fundsmanager.domain.usecase.CalculateProjectSummaryUseCase
import com.example.fundsmanager.domain.usecase.GetProjectLedgerUseCase
import com.example.fundsmanager.domain.usecase.PrepareProjectReportUseCase
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase,
    private val getProjectLedgerUseCase: GetProjectLedgerUseCase,
    private val prepareProjectReportUseCase: PrepareProjectReportUseCase,
    private val reportFileRepository: ReportFileRepository,
    private val appLogger: AppLogger
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: run {
        val error = IllegalStateException("Missing projectId route argument")
        appLogger.error(
            category = AppLogCategory.NAVIGATION,
            screen = "Dashboard",
            action = "route_argument_missing",
            message = "Missing projectId route argument",
            throwable = error
        )
        -1L
    }

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var lastRefreshAt = 0L

    init {
        refreshDashboard(force = true)
    }

    fun refreshDashboard(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force) {
            if (_uiState.value is DashboardUiState.Loading) return
            if (now - lastRefreshAt < 1_500L) return
        }
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                val summary = calculateProjectSummaryUseCase(projectId)
                if (summary != null) {
                    val recentItems = getProjectLedgerUseCase(projectId)
                        .sortedByDescending { it.date }
                        .take(10)
                    lastRefreshAt = System.currentTimeMillis()
                    _uiState.value = DashboardUiState.Success(summary, recentItems)
                } else {
                    _uiState.value = DashboardUiState.Error("Project tidak tersedia")
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Gagal memuat dashboard")
            }
        }
    }

    fun exportPdf() {
        exportReport(isPdf = true, share = true)
    }

    fun exportExcel() {
        exportReport(isPdf = false, share = true)
    }

    fun savePdf() {
        exportReport(isPdf = true, share = false)
    }

    fun saveExcel() {
        exportReport(isPdf = false, share = false)
    }

    fun onShareConsumed() {
        val current = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = current.copy(pendingShareFile = null)
    }

    fun clearExportMessage() {
        val current = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = current.copy(exportMessage = null, exportError = null)
    }

    fun logShareError(throwable: Throwable, reportFile: com.example.fundsmanager.domain.service.ReportFile) {
        appLogger.error(
            category = AppLogCategory.EXPORT,
            screen = "Dashboard",
            action = "share_report_error",
            message = "Failed to open report share sheet",
            throwable = throwable,
            details = "projectId=$projectId fileName=${reportFile.file.name} mimeType=${reportFile.mimeType}"
        )
    }

    private fun exportReport(isPdf: Boolean, share: Boolean) {
        val current = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = current.copy(isExporting = true, exportMessage = null, exportError = null, pendingShareFile = null)
        viewModelScope.launch {
            try {
                val format = if (isPdf) "PDF" else "Excel"
                appLogger.info(
                    category = AppLogCategory.EXPORT,
                    screen = "Dashboard",
                    action = if (isPdf) "export_pdf_start" else "export_excel_start",
                    message = "$format export started",
                    details = "projectId=$projectId share=$share"
                )
                val reportData = prepareProjectReportUseCase(projectId)
                    ?: throw IllegalStateException("Project tidak tersedia")
                val reportFile = if (isPdf) {
                    reportFileRepository.createPdf(reportData)
                } else {
                    reportFileRepository.createExcel(reportData)
                }
                val latest = _uiState.value as? DashboardUiState.Success ?: return@launch
                appLogger.info(
                    category = AppLogCategory.EXPORT,
                    screen = "Dashboard",
                    action = if (isPdf) "export_pdf_success" else "export_excel_success",
                    message = "$format export created",
                    details = "projectId=$projectId fileName=${reportFile.file.name} bytes=${reportFile.file.length()} share=$share"
                )
                _uiState.value = latest.copy(
                    isExporting = false,
                    exportMessage = if (share) "Laporan berhasil dibuat" else "Laporan tersimpan: ${reportFile.file.name}",
                    exportError = null,
                    pendingShareFile = reportFile.takeIf { share }
                )
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.EXPORT,
                    screen = "Dashboard",
                    action = if (isPdf) "export_pdf_error" else "export_excel_error",
                    message = "Failed to create report",
                    throwable = e,
                    details = "projectId=$projectId share=$share"
                )
                val latest = _uiState.value as? DashboardUiState.Success ?: return@launch
                _uiState.value = latest.copy(
                    isExporting = false,
                    exportMessage = null,
                    exportError = "Gagal membuat laporan. Coba lagi.",
                    pendingShareFile = null
                )
            }
        }
    }
}

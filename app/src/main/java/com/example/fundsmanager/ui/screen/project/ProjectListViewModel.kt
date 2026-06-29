package com.example.fundsmanager.ui.screen.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.domain.model.Project
import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class ProjectListItem(
    val project: Project,
    val summary: ProjectSummary?,
    val transactionCount: Int
)

data class ProjectListUiState(
    val projects: List<ProjectListItem> = emptyList(),
    val showArchived: Boolean = false,
    val error: String? = null,
    val canCreateProject: Boolean = false
)

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val repository: FundsRepository,
    private val sessionManager: SessionManager,
    private val appLogger: AppLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectListUiState())
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    private var allProjects: List<Project> = emptyList()

    init {
        observeProjects()
        observeCanCreateProject()
    }

    private fun observeCanCreateProject() {
        viewModelScope.launch {
            sessionManager.activeSession.collectLatest { session ->
                val roles = session?.roles ?: emptyList()
                val canCreate = roles.any { it in listOf("OWNER", "ADMIN", "FINANCE_MANAGER", "SUPERVISOR") }
                _uiState.update { it.copy(canCreateProject = canCreate) }
            }
        }
    }

    fun addProject(name: String, startDate: String?, completedDate: String?) {
        viewModelScope.launch {
            try {
                val session = sessionManager.activeSession.first()
                val userId = session?.userId ?: 1L
                _uiState.update { it.copy(error = null) }
                val startAt = parseDateOrNull(startDate) ?: System.currentTimeMillis()
                val completedAt = parseDateOrNull(completedDate)
                if (completedAt != null && completedAt < startAt) {
                    _uiState.update { it.copy(error = "Tanggal selesai tidak boleh lebih awal dari tanggal mulai") }
                    return@launch
                }
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "create_project_clicked",
                    message = "Create project requested",
                    details = "name=${name.take(80)} startAt=$startAt completedAt=${completedAt ?: "-"}"
                )
                val projectId = repository.insertProject(
                    userId,
                    Project(
                        id = 0,
                        name = name.trim(),
                        description = null,
                        isArchived = false,
                        startAt = startAt,
                        completedAt = completedAt
                    )
                )
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "create_project_success",
                    message = "Project created",
                    details = "projectId=$projectId name=${name.take(80)} startAt=$startAt completedAt=${completedAt ?: "-"}"
                )
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "create_project_error",
                    message = "Failed to create project",
                    throwable = e,
                    details = "name=${name.take(80)}"
                )
                _uiState.update { it.copy(error = e.message ?: "Gagal membuat project") }
            }
        }
    }

    private fun parseDateOrNull(value: String?): Long? {
        val text = value?.trim().orEmpty()
        if (text.isBlank()) return null
        return try {
            LocalDate.parse(text).toEpochDay() * 86_400_000L
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun onShowArchivedChange(show: Boolean) {
        _uiState.update { it.copy(showArchived = show) }
        refreshProjectItems()
    }

    fun setProjectArchived(projectId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            try {
                repository.setProjectArchived(projectId, isArchived)
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = if (isArchived) "archive_project_success" else "restore_project_success",
                    message = if (isArchived) "Project archived" else "Project restored",
                    details = "projectId=$projectId"
                )
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = if (isArchived) "archive_project_error" else "restore_project_error",
                    message = if (isArchived) "Failed to archive project" else "Failed to restore project",
                    throwable = e,
                    details = "projectId=$projectId"
                )
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            try {
                repository.softDeleteProject(projectId)
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "delete_project_success",
                    message = "Project deleted",
                    details = "projectId=$projectId"
                )
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "delete_project_error",
                    message = "Failed to delete project",
                    throwable = e,
                    details = "projectId=$projectId"
                )
                _uiState.update { it.copy(error = e.message ?: "Gagal menghapus project") }
            }
        }
    }

    fun renameProject(projectId: Long, name: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null) }
                repository.renameProject(projectId, name)
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "rename_project_success",
                    message = "Project renamed",
                    details = "projectId=$projectId name=${name.take(80)}"
                )
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "rename_project_error",
                    message = "Failed to rename project",
                    throwable = e,
                    details = "projectId=$projectId name=${name.take(80)}"
                )
                _uiState.update { it.copy(error = e.message ?: "Gagal mengganti nama project") }
            }
        }
    }

    fun updateProjectSchedule(projectId: Long, startDate: String, completedDate: String?) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null) }
                val startAt = parseDateOrNull(startDate)
                if (startAt == null) {
                    _uiState.update { it.copy(error = "Tanggal mulai wajib diisi") }
                    return@launch
                }
                val completedAt = parseDateOrNull(completedDate)
                if (completedAt != null && completedAt < startAt) {
                    _uiState.update { it.copy(error = "Tanggal selesai tidak boleh lebih awal dari tanggal mulai") }
                    return@launch
                }
                repository.updateProjectSchedule(projectId, startAt, completedAt)
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "update_project_schedule_success",
                    message = "Project schedule updated",
                    details = "projectId=$projectId startAt=$startAt completedAt=${completedAt ?: "-"}"
                )
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "update_project_schedule_error",
                    message = "Failed to update project schedule",
                    throwable = e,
                    details = "projectId=$projectId startDate=$startDate completedDate=${completedDate ?: "-"}"
                )
                _uiState.update { it.copy(error = e.message ?: "Gagal memperbarui tanggal project") }
            }
        }
    }

    private fun observeProjects() {
        viewModelScope.launch {
            repository.getAllProjects().collectLatest { projects ->
                allProjects = projects
                refreshProjectItems()
            }
        }
    }

    private fun refreshProjectItems() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.Default) {
                val showArchived = _uiState.value.showArchived
                val visibleProjects = allProjects.filter { it.isArchived == showArchived }
                val transactionsByProject = repository.getAllTransactions().groupBy { it.projectId }
                visibleProjects.map { project ->
                    val transactions = transactionsByProject[project.id].orEmpty()
                    val summary = buildSummary(project, transactions)
                    val transactionCount = transactions.size
                    ProjectListItem(project, summary, transactionCount)
                }
            }
            _uiState.update { it.copy(projects = items) }
        }
    }

    private fun buildSummary(project: Project, transactions: List<Transaction>): ProjectSummary {
        var totalFundIn = 0L
        var totalOfficeReported = 0L
        var totalOfficeReal = 0L
        var totalPersonalExpense = 0L

        transactions.forEach { tx ->
            when (tx.type) {
                TransactionType.FUND_IN -> totalFundIn += tx.reportedAmount
                TransactionType.OFFICE_EXPENSE -> {
                    totalOfficeReported += tx.reportedAmount
                    totalOfficeReal += tx.realAmount
                }
                TransactionType.PERSONAL_EXPENSE -> totalPersonalExpense += tx.realAmount
            }
        }

        val saving = totalOfficeReported - totalOfficeReal
        val remainingReported = totalFundIn - totalOfficeReported
        val remainingReal = totalFundIn - totalOfficeReal
        val totalCashOut = totalOfficeReal + totalPersonalExpense
        val netPosition = totalFundIn - totalCashOut

        return ProjectSummary(
            projectId = project.id,
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
package com.example.fundsmanager.ui.screen.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Project
import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.usecase.CalculateProjectSummaryUseCase
import com.example.fundsmanager.domain.usecase.GetProjectLedgerUseCase
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectListItem(
    val project: Project,
    val summary: ProjectSummary?,
    val transactionCount: Int
)

data class ProjectListUiState(
    val projects: List<ProjectListItem> = emptyList(),
    val showArchived: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val repository: FundsRepository,
    private val calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase,
    private val getProjectLedgerUseCase: GetProjectLedgerUseCase,
    private val appLogger: AppLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProjectListUiState())
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    private var allProjects: List<Project> = emptyList()

    init {
        observeProjects()
    }

    fun addProject(name: String, userId: Long = 1L) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null) }
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "create_project_clicked",
                    message = "Create project requested",
                    details = "name=${name.take(80)}"
                )
                val projectId = repository.insertProject(userId, Project(0, name.trim(), null, false))
                appLogger.info(
                    category = AppLogCategory.PROJECT,
                    screen = "ProjectList",
                    action = "create_project_success",
                    message = "Project created",
                    details = "projectId=$projectId name=${name.take(80)}"
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
            val showArchived = _uiState.value.showArchived
            val visibleProjects = allProjects.filter { it.isArchived == showArchived }
            val items = visibleProjects.map { project ->
                val summary = calculateProjectSummaryUseCase(project.id)
                val transactionCount = getProjectLedgerUseCase(project.id).count()
                ProjectListItem(project, summary, transactionCount)
            }
            _uiState.update { it.copy(projects = items) }
        }
    }
}

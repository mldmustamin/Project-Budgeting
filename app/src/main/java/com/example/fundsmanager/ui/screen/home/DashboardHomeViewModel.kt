package com.example.fundsmanager.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Project
import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.isIncome
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.usecase.CalculateProjectSummaryUseCase
import com.example.fundsmanager.domain.usecase.GetProjectLedgerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DashboardProjectItem(
    val project: Project,
    val summary: ProjectSummary,
    val ageDays: Long
)

data class DashboardRecentTransactionItem(
    val transaction: Transaction,
    val projectName: String
)

data class DashboardAttentionItem(
    val title: String,
    val subtitle: String,
    val amount: Long,
    val projectId: Long
)

data class DashboardHomeUiState(
    val isLoading: Boolean = true,
    val activeProjectCount: Int = 0,
    val archivedProjectCount: Int = 0,
    val ongoingBalance: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val activeDifference: Long = 0L,
    val overallDifference: Long = 0L,
    val projectItems: List<DashboardProjectItem> = emptyList(),
    val recentTransactions: List<DashboardRecentTransactionItem> = emptyList(),
    val longestRunningProject: DashboardAttentionItem? = null,
    val biggestExpenseProject: DashboardAttentionItem? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardHomeViewModel @Inject constructor(
    private val repository: FundsRepository,
    private val calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase,
    private val getProjectLedgerUseCase: GetProjectLedgerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardHomeUiState())
    val uiState: StateFlow<DashboardHomeUiState> = _uiState.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    val projects = repository.getAllProjects().first()
                    val summaries = projects.mapNotNull { project ->
                        calculateProjectSummaryUseCase(project.id)?.let { summary -> project to summary }
                    }
                    val activeItems = summaries.filter { (project, _) -> !project.isArchived }
                    val allTransactions = projects.flatMap { project ->
                        getProjectLedgerUseCase(project.id).map { transaction -> project to transaction }
                    }.sortedWith(compareByDescending<Pair<Project, Transaction>> { it.second.date }.thenByDescending { it.second.id })

                    val projectItems = activeItems.map { (project, summary) ->
                        DashboardProjectItem(
                            project = project,
                            summary = summary,
                            ageDays = project.ageInDays()
                        )
                    }.sortedByDescending { it.summary.netPosition }

                    val longestRunning = projects
                        .filter { !it.isArchived }
                        .minByOrNull { it.startAt.takeIf { start -> start > 0L } ?: Long.MAX_VALUE }
                    val biggestExpense = allTransactions
                        .filter { it.second.type != TransactionType.FUND_IN }
                        .maxByOrNull { it.second.realAmount }

                    DashboardHomeUiState(
                        isLoading = false,
                        activeProjectCount = projects.count { !it.isArchived },
                        archivedProjectCount = projects.count { it.isArchived },
                        ongoingBalance = activeItems.sumOf { it.second.netPosition },
                        totalIncome = summaries.sumOf { it.second.totalFundIn },
                        totalExpense = summaries.sumOf { it.second.totalCashOut },
                        activeDifference = activeItems.sumOf { it.second.netPosition },
                        overallDifference = summaries.sumOf { it.second.netPosition },
                        projectItems = projectItems,
                        recentTransactions = allTransactions.take(6).map { (project, transaction) ->
                            DashboardRecentTransactionItem(transaction = transaction, projectName = project.name)
                        },
                        longestRunningProject = longestRunning?.let { project ->
                            DashboardAttentionItem(
                                title = project.name,
                                subtitle = "${project.ageInDays()} hari berjalan",
                                amount = activeItems.firstOrNull { it.first.id == project.id }?.second?.netPosition ?: 0L,
                                projectId = project.id
                            )
                        },
                        biggestExpenseProject = biggestExpense?.let { (project, transaction) ->
                            DashboardAttentionItem(
                                title = project.name,
                                subtitle = transaction.description,
                                amount = if (transaction.type.isIncome()) 0L else transaction.realAmount,
                                projectId = project.id
                            )
                        }
                    )
                }
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                _uiState.value = DashboardHomeUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat dashboard"
                )
            }
        }
    }

    private fun Project.ageInDays(): Long {
        val started = startAt.takeIf { it > 0 } ?: createdAt.takeIf { it > 0 } ?: return 0L
        val diff = System.currentTimeMillis() - started
        return (diff / (1000L * 60L * 60L * 24L)).coerceAtLeast(0L)
    }
}

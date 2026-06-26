package com.example.fundsmanager.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Project
import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.isIncome
import com.example.fundsmanager.domain.repository.FundsRepository
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
    private val repository: FundsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardHomeUiState())
    val uiState: StateFlow<DashboardHomeUiState> = _uiState.asStateFlow()
    private var lastRefreshAt = 0L

    init {
        refreshDashboard(force = true)
    }

    fun refreshDashboard(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force) {
            if (_uiState.value.isLoading) return
            if (now - lastRefreshAt < 1_500L) return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    val projects = repository.getAllProjects().first()
                    val projectMap = projects.associateBy { it.id }
                    val allTransactions = repository.getAllTransactions()
                    val transactionsByProject = allTransactions.groupBy { it.projectId }
                    val summaries = projects.mapNotNull { project ->
                        buildSummary(project, transactionsByProject[project.id].orEmpty())?.let { summary -> project to summary }
                    }
                    val activeItems = summaries.filter { (project, _) -> !project.isArchived }
                    val timeline = allTransactions.mapNotNull { transaction ->
                        projectMap[transaction.projectId]?.let { project -> project to transaction }
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
                    val biggestExpense = timeline
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
                        recentTransactions = timeline.take(6).map { (project, transaction) ->
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
                lastRefreshAt = System.currentTimeMillis()
                _uiState.value = state
            }.onFailure { error ->
                _uiState.value = DashboardHomeUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat dashboard"
                )
            }
        }
    }

    private fun buildSummary(project: Project, transactions: List<Transaction>): ProjectSummary? {
        if (project.deletedAt != null) return null
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

    private fun Project.ageInDays(): Long {
        val started = startAt.takeIf { it > 0 } ?: createdAt.takeIf { it > 0 } ?: return 0L
        val diff = System.currentTimeMillis() - started
        return (diff / (1000L * 60L * 60L * 24L)).coerceAtLeast(0L)
    }
}

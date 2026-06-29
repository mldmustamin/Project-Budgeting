package com.example.fundsmanager.ui.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.data.local.dao.ExpenseItemDao
import com.example.fundsmanager.data.local.dao.TaskExpenseDao
import com.example.fundsmanager.data.local.dao.UserDao
import com.example.fundsmanager.data.mapper.toDomain
import com.example.fundsmanager.domain.model.BudgetTask
import com.example.fundsmanager.domain.model.ExpenseItemModel
import com.example.fundsmanager.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApprovalUiState(
    val tasks: List<BudgetTask> = emptyList(),
    val userNames: Map<Long, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Dialog state
    val selectedTask: BudgetTask? = null,
    val selectedItems: List<ExpenseItemModel> = emptyList(),
    val approvedAmounts: Map<String, String> = emptyMap(), // item uuid -> editable amount string
    val isSubmitting: Boolean = false
)

@HiltViewModel
class ApprovalViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager,
    private val taskExpenseDao: TaskExpenseDao,
    private val expenseItemDao: ExpenseItemDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApprovalUiState())
    val uiState: StateFlow<ApprovalUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                budgetRepository.getTasksByStage("FORWARDED").collect { tasks ->
                    val userIds = tasks.flatMap { listOfNotNull(it.submittedBy, it.forwardedBy) }.distinct()
                    val names = mutableMapOf<Long, String>()
                    userIds.forEach { uid ->
                        userDao.getUserById(uid)?.let { user -> names[uid] = user.name }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks,
                            userNames = names
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectTask(task: BudgetTask) {
        viewModelScope.launch {
            try {
                val entity = taskExpenseDao.getByUuid(task.uuid)
                val items = if (entity != null) {
                    expenseItemDao.getByTaskSync(entity.id).map { it.toDomain() }
                } else {
                    emptyList()
                }
                val amounts = items.associate {
                    it.uuid to (it.approvedAmount?.toString() ?: it.estimatedAmount.toString())
                }
                _uiState.update {
                    it.copy(
                        selectedTask = task,
                        selectedItems = items,
                        approvedAmounts = amounts
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(selectedTask = null, selectedItems = emptyList(), approvedAmounts = emptyMap())
        }
    }

    fun updateApprovedAmount(itemUuid: String, amount: String) {
        _uiState.update {
            it.copy(approvedAmounts = it.approvedAmounts + (itemUuid to amount))
        }
    }

    fun approveTask(taskId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                // TODO: Call backend approve endpoint
                dismissDialog()
                loadTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }
}

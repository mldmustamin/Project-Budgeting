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

data class VerificationUiState(
    val tasks: List<BudgetTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Expanded state
    val expandedTaskUuid: String? = null,
    val expandedItems: List<ExpenseItemModel> = emptyList(),
    // Per-item bill_verified state: item uuid -> verified boolean
    val billVerifiedMap: Map<String, Boolean> = emptyMap(),
    val isSubmitting: Boolean = false
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager,
    private val taskExpenseDao: TaskExpenseDao,
    private val expenseItemDao: ExpenseItemDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                budgetRepository.getTasksByStage("REALISASI").collect { tasks ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleExpanded(task: BudgetTask) {
        val current = _uiState.value.expandedTaskUuid
        if (current == task.uuid) {
            // Collapse
            _uiState.update {
                it.copy(expandedTaskUuid = null, expandedItems = emptyList(), billVerifiedMap = emptyMap())
            }
        } else {
            // Expand: load items
            viewModelScope.launch {
                try {
                    val entity = taskExpenseDao.getByUuid(task.uuid)
                    val items = if (entity != null) {
                        expenseItemDao.getByTaskSync(entity.id).map { it.toDomain() }
                    } else {
                        emptyList()
                    }
                    val verifiedMap = items.associate { it.uuid to it.billVerified }
                    _uiState.update {
                        it.copy(
                            expandedTaskUuid = task.uuid,
                            expandedItems = items,
                            billVerifiedMap = verifiedMap
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun toggleBillVerified(itemUuid: String) {
        _uiState.update {
            val current = it.billVerifiedMap[itemUuid] ?: false
            it.copy(billVerifiedMap = it.billVerifiedMap + (itemUuid to !current))
        }
    }

    fun verifyTask(taskId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                // TODO: Call backend verify endpoint with billVerifiedMap
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        expandedTaskUuid = null,
                        expandedItems = emptyList(),
                        billVerifiedMap = emptyMap()
                    )
                }
                loadTasks()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }
}

package com.example.fundsmanager.ui.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.domain.model.BudgetTask
import com.example.fundsmanager.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyTasksUiState(
    val draftTasks: List<BudgetTask> = emptyList(),
    val pendingTasks: List<BudgetTask> = emptyList(),  // ESTIMASI, FORWARDED
    val activeTasks: List<BudgetTask> = emptyList(),    // APPROVED
    val completedTasks: List<BudgetTask> = emptyList(), // REALISASI, VERIFIED, RECONCILED
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MyTasksViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyTasksUiState())
    val uiState: StateFlow<MyTasksUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val session = sessionManager.activeSession.first() ?: return@launch
                budgetRepository.getMyTasks(session.userId).collect { tasks ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            draftTasks = tasks.filter { t -> t.stage == "DRAFT" },
                            pendingTasks = tasks.filter { t -> t.stage in listOf("ESTIMASI", "FORWARDED") },
                            activeTasks = tasks.filter { t -> t.stage == "APPROVED" },
                            completedTasks = tasks.filter { t ->
                                t.stage in listOf("REALISASI", "VERIFIED", "RECONCILED")
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteDraft(task: BudgetTask) {
        viewModelScope.launch {
            budgetRepository.deleteDraft(task.id)
        }
    }
}

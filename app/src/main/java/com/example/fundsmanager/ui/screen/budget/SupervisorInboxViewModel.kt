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

data class SupervisorInboxUiState(
    val pendingTasks: List<BudgetTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SupervisorInboxViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupervisorInboxUiState())
    val uiState: StateFlow<SupervisorInboxUiState> = _uiState.asStateFlow()

    init {
        loadPendingTasks()
    }

    fun loadPendingTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                combine(
                    budgetRepository.getTasksByStage("ESTIMASI"),
                    budgetRepository.getTasksByStage("FORWARDED")
                ) { estimasi, forwarded ->
                    estimasi + forwarded
                }.collect { tasks ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingTasks = tasks
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

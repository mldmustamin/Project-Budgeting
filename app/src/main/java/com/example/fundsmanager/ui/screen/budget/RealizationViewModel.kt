package com.example.fundsmanager.ui.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.domain.model.BudgetTask
import com.example.fundsmanager.domain.model.ExpenseItemModel
import com.example.fundsmanager.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RealizationUiState(
    val task: BudgetTask? = null,
    val items: List<ExpenseItemModel> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class RealizationViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RealizationUiState())
    val uiState: StateFlow<RealizationUiState> = _uiState.asStateFlow()
    private var taskUuid: String? = null

    fun loadTask(uuid: String) {
        taskUuid = uuid
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            budgetRepository.getMyTasks(0).collect { tasks ->
                val task = tasks.find { it.uuid == uuid }
                if (task != null) {
                    _uiState.update { it.copy(task = task, isLoading = false) }
                }
            }
        }
    }

    fun updateItemAmount(index: Int, amount: Long) {
        val items = _uiState.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = items[index].copy(realizationAmount = amount)
            _uiState.update { it.copy(items = items) }
        }
    }

    fun updateItemNote(index: Int, note: String) {
        val items = _uiState.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = items[index].copy(note = note.ifBlank { null })
            _uiState.update { it.copy(items = items) }
        }
    }

    fun saveRealization() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

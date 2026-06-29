package com.example.fundsmanager.ui.screen.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetEstimateUiState(
    // Task fields
    val taskNo: String = "",
    val vid: String = "",
    val taskName: String = "",
    val jobType: String = "INSTALASI",
    val projectId: Long = 1,
    val locationId: Long? = null,
    val notes: String = "",
    val deadlineAt: Long? = null,

    // Items
    val items: List<ExpenseItemDraft> = listOf(ExpenseItemDraft()),

    // Data sources
    val templates: List<BudgetTemplate> = emptyList(),
    val locations: List<MasterLocation> = emptyList(),

    // UI state
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val savedTask: BudgetTask? = null,
    val error: String? = null,
    val paguViolations: List<String> = emptyList(),
    val paguWarnings: List<String> = emptyList()
)

data class ExpenseItemDraft(
    val id: Long = 0,
    val templateId: Long? = null,
    val tanggal: String = "",
    val estimatedAmount: String = "",
    val note: String = ""
)

@HiltViewModel
class BudgetEstimateViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetEstimateUiState())
    val uiState: StateFlow<BudgetEstimateUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
        loadLocations()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            budgetRepository.getTemplates().collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    private fun loadLocations() {
        viewModelScope.launch {
            budgetRepository.getLocations(projectId = 1).collect { locations ->
                _uiState.update { it.copy(locations = locations) }
            }
        }
    }

    fun updateTaskNo(value: String) {
        _uiState.update { it.copy(taskNo = value, isDirty = true) }
    }
    fun updateVid(value: String) {
        _uiState.update { it.copy(vid = value, isDirty = true) }
    }
    fun updateJobType(value: String) {
        _uiState.update { it.copy(jobType = value, isDirty = true) }
    }
    fun updateLocation(id: Long?) {
        _uiState.update { it.copy(locationId = id, isDirty = true) }
    }
    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value, isDirty = true) }
    }

    fun updateItem(index: Int, field: String, value: String) {
        val items = _uiState.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = when (field) {
                "templateId" -> items[index].copy(templateId = value.toLongOrNull())
                "tanggal" -> items[index].copy(tanggal = value)
                "estimatedAmount" -> items[index].copy(estimatedAmount = value)
                "note" -> items[index].copy(note = value)
                else -> items[index]
            }
            _uiState.update { it.copy(items = items, isDirty = true) }
        }
    }

    fun addItem() {
        _uiState.update {
            it.copy(items = it.items + ExpenseItemDraft(), isDirty = true)
        }
    }

    fun removeItem(index: Int) {
        val items = _uiState.value.items.toMutableList()
        if (items.size > 1 && index in items.indices) {
            items.removeAt(index)
            _uiState.update { it.copy(items = items, isDirty = true) }
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val session = sessionManager.activeSession.first() ?: return@launch
                val state = _uiState.value
                val domainItems = state.items.mapNotNull { draft ->
                    if (draft.estimatedAmount.isBlank()) return@mapNotNull null
                    ExpenseItemModel(
                        id = draft.id,
                        templateId = draft.templateId,
                        tanggal = draft.tanggal.ifBlank { "2026-01-01" },
                        estimatedAmount = draft.estimatedAmount.toLongOrNull() ?: 0,
                        note = draft.note.ifBlank { null }
                    )
                }
                val task = BudgetTask(
                    projectId = state.projectId,
                    locationId = state.locationId,
                    taskNo = state.taskNo.ifBlank { "DRAFT-${System.currentTimeMillis()}" },
                    vid = state.vid.ifBlank { "VID-DRAFT" },
                    taskName = state.taskName.ifBlank { null },
                    jobType = state.jobType,
                    submittedBy = session.userId,
                    notes = state.notes.ifBlank { null }
                )

                val saved = if (_uiState.value.savedTask != null) {
                    budgetRepository.updateDraft(_uiState.value.savedTask!!.copy(
                        taskNo = task.taskNo, vid = task.vid,
                        jobType = task.jobType, locationId = task.locationId,
                        notes = task.notes
                    ), domainItems)
                } else {
                    budgetRepository.createDraft(task, domainItems)
                }

                _uiState.update {
                    it.copy(isSaving = false, savedTask = saved, isDirty = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

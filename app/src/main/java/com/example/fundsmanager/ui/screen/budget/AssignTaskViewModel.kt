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

data class AssignTaskUiState(
    val taskNo: String = "",
    val vid: String = "",
    val taskName: String = "",
    val jobType: String = "INSTALASI",
    val projectId: Long = 1,
    val locationId: Long? = null,
    val fieldEngineerId: Long? = null,
    val notes: String = "",

    val locations: List<MasterLocation> = emptyList(),

    val isSaving: Boolean = false,
    val savedTask: BudgetTask? = null,
    val error: String? = null
)

@HiltViewModel
class AssignTaskViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignTaskUiState())
    val uiState: StateFlow<AssignTaskUiState> = _uiState.asStateFlow()

    init {
        loadLocations()
    }

    private fun loadLocations() {
        viewModelScope.launch {
            budgetRepository.getLocations(projectId = 1).collect { locations ->
                _uiState.update { it.copy(locations = locations) }
            }
        }
    }

    fun updateTaskNo(value: String) {
        _uiState.update { it.copy(taskNo = value) }
    }

    fun updateVid(value: String) {
        _uiState.update { it.copy(vid = value) }
    }

    fun updateTaskName(value: String) {
        _uiState.update { it.copy(taskName = value) }
    }

    fun updateJobType(value: String) {
        _uiState.update { it.copy(jobType = value) }
    }

    fun updateLocation(id: Long?) {
        _uiState.update { it.copy(locationId = id) }
    }

    fun updateFieldEngineer(id: Long?) {
        _uiState.update { it.copy(fieldEngineerId = id) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun saveDraft() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val session = sessionManager.activeSession.first() ?: return@launch
                val state = _uiState.value

                val task = BudgetTask(
                    projectId = state.projectId,
                    locationId = state.locationId,
                    taskNo = state.taskNo.ifBlank { "DRAFT-${System.currentTimeMillis()}" },
                    vid = state.vid.ifBlank { "VID-DRAFT" },
                    taskName = state.taskName.ifBlank { null },
                    jobType = state.jobType,
                    submittedBy = session.userId,
                    forwardedBy = state.fieldEngineerId,
                    notes = state.notes.ifBlank { null }
                )

                val saved = if (state.savedTask != null) {
                    budgetRepository.updateDraft(
                        state.savedTask!!.copy(
                            taskNo = task.taskNo,
                            vid = task.vid,
                            taskName = task.taskName,
                            jobType = task.jobType,
                            locationId = task.locationId,
                            forwardedBy = task.forwardedBy,
                            notes = task.notes
                        ),
                        emptyList()
                    )
                } else {
                    budgetRepository.createDraft(task, emptyList())
                }

                _uiState.update {
                    it.copy(isSaving = false, savedTask = saved)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

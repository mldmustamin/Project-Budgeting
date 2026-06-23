package com.example.fundsmanager.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.ProjectSummary
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.usecase.CalculateOverallSummaryUseCase
import com.example.fundsmanager.domain.usecase.CalculateProjectSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportHomeUiState(
    val summaries: List<ProjectSummary> = emptyList(),
    val total: ProjectSummary? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportHomeViewModel @Inject constructor(
    private val repository: FundsRepository,
    private val calculateProjectSummaryUseCase: CalculateProjectSummaryUseCase,
    private val calculateOverallSummaryUseCase: CalculateOverallSummaryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportHomeUiState())
    val uiState: StateFlow<ReportHomeUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val summaries = repository.getAllProjects().first()
                .filter { !it.isArchived }
                .mapNotNull { calculateProjectSummaryUseCase(it.id) }
            _uiState.update {
                it.copy(
                    summaries = summaries,
                    total = calculateOverallSummaryUseCase(summaries),
                    isLoading = false
                )
            }
        }
    }
}

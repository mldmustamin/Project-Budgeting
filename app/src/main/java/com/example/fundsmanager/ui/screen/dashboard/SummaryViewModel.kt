package com.example.fundsmanager.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.TransactionType
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

data class SummaryUiState(
    val isLoading: Boolean = true,
    val totalDanaMasuk: Long = 0L,
    val totalKasKeluar: Long = 0L,
    val posisiBersih: Long = 0L,
    val pendingAmount: Long = 0L,
    val officeRealTotal: Long = 0L,
    val personalTotal: Long = 0L,
    val surplusDefisit: Long = 0L,
    val error: String? = null
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: FundsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()
    private var lastRefreshAt = 0L

    init {
        loadSummary(force = true)
    }

    fun loadSummary(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force) {
            if (_uiState.value.isLoading) return
            if (now - lastRefreshAt < 1_500L) return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    val allProjects = repository.getAllProjects().first()
                    val activeProjectIds = allProjects.filter { !it.isArchived && it.deletedAt == null }
                        .map { it.id }.toSet()
                    val allTransactions = repository.getAllTransactions()
                        .filter { it.projectId in activeProjectIds && it.deletedAt == null }

                    var totalDanaMasuk = 0L
                    var totalOfficeReal = 0L
                    var totalPersonal = 0L
                    var pendingAmount = 0L

                    allTransactions.forEach { tx ->
                        when (tx.type) {
                            TransactionType.FUND_IN -> totalDanaMasuk += tx.reportedAmount
                            TransactionType.OFFICE_EXPENSE -> totalOfficeReal += tx.realAmount
                            TransactionType.PERSONAL_EXPENSE -> totalPersonal += tx.realAmount
                        }
                        if (tx.syncStatus == "PENDING") {
                            pendingAmount += tx.reportedAmount
                        }
                    }

                    val totalKasKeluar = totalOfficeReal + totalPersonal
                    val posisiBersih = totalDanaMasuk - totalKasKeluar
                    val surplusDefisit = posisiBersih // positive = surplus, negative = defisit

                    SummaryUiState(
                        isLoading = false,
                        totalDanaMasuk = totalDanaMasuk,
                        totalKasKeluar = totalKasKeluar,
                        posisiBersih = posisiBersih,
                        pendingAmount = pendingAmount,
                        officeRealTotal = totalOfficeReal,
                        personalTotal = totalPersonal,
                        surplusDefisit = surplusDefisit
                    )
                }
            }.onSuccess { state ->
                lastRefreshAt = System.currentTimeMillis()
                _uiState.value = state
            }.onFailure { error ->
                _uiState.value = SummaryUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat ringkasan"
                )
            }
        }
    }
}

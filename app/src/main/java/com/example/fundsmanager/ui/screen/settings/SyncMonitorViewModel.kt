package com.example.fundsmanager.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.dao.SyncOutboxDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SyncMonitorUiState(
    val isLoading: Boolean = true,
    val lastSyncTime: Long? = null,
    val syncStatus: String = "IDLE",
    val pendingCount: Int = 0,
    val rejectedCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class SyncMonitorViewModel @Inject constructor(
    private val syncOutboxDao: SyncOutboxDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncMonitorUiState())
    val uiState: StateFlow<SyncMonitorUiState> = _uiState.asStateFlow()

    init {
        loadStatus()
    }

    fun loadStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                withContext(Dispatchers.Default) {
                    val pending = syncOutboxDao.getPending()
                    val pendingCount = pending.size
                    val rejectedCount = pending.count { it.status == "REJECTED" }
                    val lastSync = pending.maxOfOrNull { it.updatedAt }

                    val syncStatus = when {
                        pendingCount == 0 && rejectedCount == 0 -> "UP_TO_DATE"
                        rejectedCount > 0 -> "HAS_REJECTED"
                        pendingCount > 0 -> "HAS_PENDING"
                        else -> "IDLE"
                    }

                    SyncMonitorUiState(
                        isLoading = false,
                        lastSyncTime = lastSync,
                        syncStatus = syncStatus,
                        pendingCount = pendingCount,
                        rejectedCount = rejectedCount
                    )
                }
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                _uiState.value = SyncMonitorUiState(
                    isLoading = false,
                    error = error.message ?: "Gagal memuat status sinkronisasi"
                )
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            try {
                withContext(Dispatchers.Default) {
                    val pending = syncOutboxDao.getPending()
                    val now = System.currentTimeMillis()
                    pending.forEach { item ->
                        if (item.status == "PENDING") {
                            syncOutboxDao.updateStatus(item.id, "IN_FLIGHT", now)
                        }
                    }
                    // Simulate sync attempt - mark as synced
                    // In production this would be handled by WorkManager/SyncWorker
                    val inFlight = syncOutboxDao.getPending()
                    inFlight.forEach { item ->
                        if (item.status == "IN_FLIGHT") {
                            syncOutboxDao.markSynced(item.id, now)
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Sync selesai",
                    syncStatus = "UP_TO_DATE",
                    pendingCount = 0,
                    rejectedCount = 0,
                    lastSyncTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = null,
                    error = e.message ?: "Sync gagal"
                )
            }
        }
    }
}

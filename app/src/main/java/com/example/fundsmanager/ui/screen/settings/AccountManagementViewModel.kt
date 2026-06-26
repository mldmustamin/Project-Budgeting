package com.example.fundsmanager.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Account
import com.example.fundsmanager.domain.repository.FundsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountManagementUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val repository: FundsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountManagementUiState())
    val uiState: StateFlow<AccountManagementUiState> = _uiState.asStateFlow()

    init {
        observeAccounts()
    }

    fun addAccount(name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return@launch
            repository.insertAccount(Account(0, trimmed, null))
        }
    }

    fun renameAccount(accountId: Long, name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return@launch
            repository.updateAccount(Account(accountId, trimmed, null))
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.softDeleteAccount(accountId)
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            repository.getAllAccounts().collectLatest { accounts ->
                _uiState.update {
                    it.copy(accounts = accounts, isLoading = false)
                }
            }
        }
    }
}

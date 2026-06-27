package com.example.fundsmanager.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.data.remote.AuthRepository
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PasswordChangeUiState(
    val password: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class PasswordChangeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val appLogger: AppLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordChangeUiState())
    val uiState: StateFlow<PasswordChangeUiState> = _uiState.asStateFlow()

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onConfirmationChange(value: String) {
        _uiState.update { it.copy(passwordConfirmation = value, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "Password minimal 6 karakter") }
            return
        }
        if (state.password != state.passwordConfirmation) {
            _uiState.update { it.copy(error = "Password tidak cocok") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val token = sessionManager.getToken() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Session tidak valid") }
                return@launch
            }
            authRepository.changePassword(token, state.password, state.passwordConfirmation)
                .onSuccess {
                    appLogger.info(AppLogCategory.AUTH, "PasswordChange", "success", "Password changed")
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure {
                    appLogger.warning(AppLogCategory.AUTH, "PasswordChange", "failed", it.message ?: "")
                    _uiState.update { it.copy(isLoading = false, error = "Gagal mengganti password") }
                }
        }
    }
}

package com.example.fundsmanager.ui.screen.auth

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.data.remote.AuthRepository
import com.example.fundsmanager.data.remote.DeviceRepository
import com.example.fundsmanager.data.remote.LoginResponse
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val requirePasswordChange: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val sessionManager: SessionManager,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onLoginChange(login: String) {
        _uiState.update { it.copy(login = login, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        val login = _uiState.value.login.trim()
        val password = _uiState.value.password

        if (login.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "ID karyawan/email dan password harus diisi") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.login(login, password)
                .onSuccess { response ->
                    appLogger.info(AppLogCategory.AUTH, "Login", "login_success",
                        "Login successful", "userId=${response.user.id}")
                    saveSessionAndRegisterDevice(response)
                    val needsPasswordChange = response.user.passwordChangeRequired
                    _uiState.update { it.copy(
                        isLoading = false,
                        isLoggedIn = !needsPasswordChange,
                        requirePasswordChange = needsPasswordChange,
                    ) }
                }
                .onFailure { error ->
                    appLogger.warning(AppLogCategory.AUTH, "Login", "login_failed",
                        error.message ?: "Unknown error")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Gagal login. Periksa koneksi dan kredensial.",
                    ) }
                }
        }
    }

    fun dismissPasswordChange() {
        _uiState.update { it.copy(requirePasswordChange = false, isLoggedIn = true) }
    }

    private suspend fun saveSessionAndRegisterDevice(response: LoginResponse) {
        val localDeviceUuid = UUID.randomUUID().toString()

        // Save session with local device UUID first
        sessionManager.saveSession(
            userId = response.user.id,
            userName = response.user.name,
            userEmail = response.user.email,
            userUuid = response.user.uuid,
            roles = response.user.roles,
            accessToken = response.accessToken,
            deviceUuid = localDeviceUuid
        )

        // Register device with backend
        deviceRepository.registerDevice(
            token = response.accessToken,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            devicePlatform = "android",
            deviceUuid = localDeviceUuid
        ).onSuccess { registerResponse ->
            // Use the server-assigned UUID if different
            val serverDeviceUuid = registerResponse.device.uuid
            if (serverDeviceUuid != localDeviceUuid) {
                sessionManager.updateDeviceUuid(serverDeviceUuid)
            }
            appLogger.info(
                category = AppLogCategory.AUTH,
                screen = "Login",
                action = "device_registered",
                message = "Device registered with backend",
                details = "deviceUuid=$serverDeviceUuid"
            )
        }.onFailure { error ->
            // Non-fatal: session already saved with local UUID
            appLogger.warning(
                category = AppLogCategory.AUTH,
                screen = "Login",
                action = "device_register_failed",
                message = error.message ?: "Device registration failed"
            )
        }
    }
}
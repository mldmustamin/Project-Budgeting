package com.example.fundsmanager.ui.screen.auth

import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.data.local.dao.UserDao
import com.example.fundsmanager.data.local.entity.UserEntity
import com.example.fundsmanager.data.remote.AuthRepository
import com.example.fundsmanager.data.remote.DeviceRepository
import com.example.fundsmanager.data.remote.LoginResponse
import com.example.fundsmanager.data.sync.SyncWorker
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val appLogger: AppLogger,
    private val userDao: UserDao,
    private val application: Application,
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
                    
                    try {
                        sessionManager.saveSession(
                            userId = response.user.id,
                            userName = response.user.name,
                            userEmail = response.user.email,
                            userUuid = response.user.uuid,
                            roles = response.user.roles,
                            accessToken = response.accessToken,
                            deviceUuid = UUID.randomUUID().toString()
                        )
                        userDao.insertUser(UserEntity(
                            id = response.user.id,
                            name = response.user.name,
                            email = response.user.email,
                            uuid = response.user.uuid,
                        ))
                    } catch (e: Exception) {
                        appLogger.warning(AppLogCategory.AUTH, "Login", "session_save_failed", e.message ?: "")
                    }

                    val needsPasswordChange = response.user.passwordChangeRequired
                    _uiState.update { it.copy(
                        isLoading = false,
                        isLoggedIn = !needsPasswordChange,
                        requirePasswordChange = needsPasswordChange,
                    ) }

                    registerDeviceInBackground(response)
                }
                .onFailure { error ->
                    val msg = when {
                        error is kotlinx.coroutines.TimeoutCancellationException -> "Koneksi timeout. Server tidak merespon."
                        error.message?.contains("Unable to resolve host") == true -> "Tidak dapat terhubung ke server."
                        error.message?.contains("Connection refused") == true -> "Server sedang sibuk. Coba lagi."
                        else -> error.message ?: "Gagal login."
                    }
                    appLogger.warning(AppLogCategory.AUTH, "Login", "login_failed", msg)
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                }
        }
    }

    fun dismissPasswordChange() {
        _uiState.update { it.copy(requirePasswordChange = false, isLoggedIn = true) }
    }

    private fun registerDeviceInBackground(response: LoginResponse) {
        viewModelScope.launch {
            try {
                val session = sessionManager.activeSession.first()
                val deviceUuid = session?.deviceUuid ?: UUID.randomUUID().toString()
                
                deviceRepository.registerDevice(
                    token = response.accessToken,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                    devicePlatform = "android",
                    deviceUuid = deviceUuid
                ).onSuccess { registerResponse ->
                    val serverDeviceUuid = registerResponse.device.uuid
                    if (serverDeviceUuid != deviceUuid) {
                        sessionManager.updateDeviceUuid(serverDeviceUuid)
                    }
                    appLogger.info(AppLogCategory.AUTH, "Login", "device_registered",
                        "Device registered", "deviceUuid=$serverDeviceUuid")
                }.onFailure { error ->
                    appLogger.warning(AppLogCategory.AUTH, "Login", "device_register_failed",
                        error.message ?: "Device registration failed")
                }

                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                WorkManager.getInstance(application).enqueueUniqueWork(
                    "fundsmanager_sync_now", ExistingWorkPolicy.REPLACE, syncRequest
                )
            } catch (_: Exception) { }
        }
    }
}

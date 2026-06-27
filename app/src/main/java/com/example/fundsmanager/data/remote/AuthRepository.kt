package com.example.fundsmanager.data.remote

import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val appLogger: AppLogger
) {
    suspend fun login(login: String, password: String): Result<LoginResponse> {
        return runCatching {
            val request = LoginRequest(login = login, password = password)
            appLogger.info(
                category = AppLogCategory.AUTH,
                screen = "AuthRepository",
                action = "login_request",
                message = "Sending login request",
                details = "login=${login.take(30)}"
            )

            val response = httpClient.post("${ApiConfig.baseUrl}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                val errorBody = try { response.body<Map<String, Any>>() } catch (_: Exception) { null }
                val message = errorBody?.get("message")?.toString() ?: "Login failed: ${response.status.value}"
                appLogger.warning(
                    category = AppLogCategory.AUTH,
                    screen = "AuthRepository",
                    action = "login_failed",
                    message = message
                )
                return Result.failure(Exception(message))
            }

            val loginResponse = response.body<LoginResponse>()
            appLogger.info(
                category = AppLogCategory.AUTH,
                screen = "AuthRepository",
                action = "login_success",
                message = "Login successful",
                details = "userId=${loginResponse.user.id} roles=${loginResponse.user.roles.joinToString()}"
            )
            loginResponse
        }
    }

    suspend fun changePassword(token: String, password: String, passwordConfirmation: String): Result<Unit> {
        return runCatching {
            val request = ChangePasswordRequest(password, passwordConfirmation)
            val response = httpClient.post("${ApiConfig.baseUrl}/auth/change-password") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Password change failed"))
            }
        }
    }

    suspend fun logout(token: String): Result<Unit> {
        return runCatching {
            httpClient.post("${ApiConfig.baseUrl}/auth/logout") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            appLogger.info(
                category = AppLogCategory.AUTH,
                screen = "AuthRepository",
                action = "logout_success",
                message = "Logout successful"
            )
        }
    }
}
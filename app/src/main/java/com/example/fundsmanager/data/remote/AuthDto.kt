package com.example.fundsmanager.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("login") val login: String,
    @SerialName("password") val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName("user") val user: UserDto,
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
)

@Serializable
data class UserDto(
    @SerialName("id") val id: Long,
    @SerialName("uuid") val uuid: String,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("password_change_required") val passwordChangeRequired: Boolean = false,
    @SerialName("roles") val roles: List<String>,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("password") val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)
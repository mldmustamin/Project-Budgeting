package com.example.fundsmanager.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegisterRequest(
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_platform") val devicePlatform: String = "android",
    @SerialName("device_version") val deviceVersion: String? = null,
    @SerialName("device_uuid") val deviceUuid: String? = null
)

@Serializable
data class DeviceRegisterResponse(
    @SerialName("device") val device: DeviceDto
)

@Serializable
data class DeviceDto(
    @SerialName("uuid") val uuid: String,
    @SerialName("user_uuid") val userUuid: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("device_platform") val devicePlatform: String,
    @SerialName("is_revoked") val isRevoked: Boolean,
    @SerialName("last_active_at") val lastActiveAt: String? = null
)
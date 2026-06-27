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
class DeviceRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val appLogger: AppLogger
) {
    suspend fun registerDevice(
        token: String,
        deviceName: String,
        devicePlatform: String = "android",
        deviceUuid: String? = null
    ): Result<DeviceRegisterResponse> {
        return runCatching {
            val request = DeviceRegisterRequest(
                deviceName = deviceName,
                devicePlatform = devicePlatform,
                deviceUuid = deviceUuid
            )
            appLogger.info(
                category = AppLogCategory.AUTH,
                screen = "DeviceRepository",
                action = "device_register_request",
                message = "Registering device",
                details = "deviceName=$deviceName platform=$devicePlatform"
            )

            val response = httpClient.post("${ApiConfig.baseUrl}/devices/register") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                appLogger.warning(
                    category = AppLogCategory.AUTH,
                    screen = "DeviceRepository",
                    action = "device_register_failed",
                    message = "Device registration failed with status ${response.status.value}"
                )
                return Result.failure(Exception("Device registration failed"))
            }

            val registerResponse = response.body<DeviceRegisterResponse>()
            appLogger.info(
                category = AppLogCategory.AUTH,
                screen = "DeviceRepository",
                action = "device_register_success",
                message = "Device registered",
                details = "serverUuid=${registerResponse.device.uuid}"
            )
            registerResponse
        }
    }
}
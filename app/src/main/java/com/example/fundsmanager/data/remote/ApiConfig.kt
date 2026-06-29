package com.example.fundsmanager.data.remote

/**
 * Backend API base URL configuration.
 * During development, use 10.0.2.2 to reach the host machine's localhost from the Android emulator.
 */
object ApiConfig {
    // Production server URL — ganti sesuai alamat server
    const val DEFAULT_BASE_URL = "http://103.94.11.78/api/v1"

    var baseUrl: String = DEFAULT_BASE_URL
}
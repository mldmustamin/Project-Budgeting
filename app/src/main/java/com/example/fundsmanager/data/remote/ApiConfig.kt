package com.example.fundsmanager.data.remote

/**
 * Backend API base URL configuration.
 * During development, use 10.0.2.2 to reach the host machine's localhost from the Android emulator.
 */
object ApiConfig {
    // Default for Android emulator accessing host machine
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/api/v1"

    var baseUrl: String = DEFAULT_BASE_URL
}
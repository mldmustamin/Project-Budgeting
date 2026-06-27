package com.example.fundsmanager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

data class ActiveSession(
    val userId: Long,
    val userName: String,
    val userEmail: String,
    val userUuid: String,
    val roles: List<String>,
    val accessToken: String,
    val deviceUuid: String
) {
    val isAuthenticated: Boolean get() = accessToken.isNotBlank()
}

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_ID = longPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_UUID = stringPreferencesKey("user_uuid")
        val ROLES = stringPreferencesKey("roles") // JSON array
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val DEVICE_UUID = stringPreferencesKey("device_uuid")
    }

    val activeSession: Flow<ActiveSession?> = context.sessionStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            val token = prefs[Keys.ACCESS_TOKEN] ?: ""
            if (token.isBlank()) null
            else ActiveSession(
                userId = prefs[Keys.USER_ID] ?: 0L,
                userName = prefs[Keys.USER_NAME] ?: "",
                userEmail = prefs[Keys.USER_EMAIL] ?: "",
                userUuid = prefs[Keys.USER_UUID] ?: "",
                roles = prefs[Keys.ROLES]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                accessToken = token,
                deviceUuid = prefs[Keys.DEVICE_UUID] ?: ""
            )
        }

    suspend fun saveSession(
        userId: Long,
        userName: String,
        userEmail: String,
        userUuid: String,
        roles: List<String>,
        accessToken: String,
        deviceUuid: String
    ) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_NAME] = userName
            prefs[Keys.USER_EMAIL] = userEmail
            prefs[Keys.USER_UUID] = userUuid
            prefs[Keys.ROLES] = roles.joinToString(",")
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.DEVICE_UUID] = deviceUuid
        }
    }

    suspend fun updateDeviceUuid(deviceUuid: String) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.DEVICE_UUID] = deviceUuid
        }
    }

    suspend fun clearSession() {
        context.sessionStore.edit { it.clear() }
    }

    suspend fun getToken(): String? {
        return activeSession.first()?.accessToken
    }

    suspend fun isLoggedIn(): Boolean {
        return activeSession.first()?.isAuthenticated == true
    }
}
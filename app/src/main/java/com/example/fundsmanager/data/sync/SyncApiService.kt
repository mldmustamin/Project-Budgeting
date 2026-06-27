package com.example.fundsmanager.data.sync

import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.data.local.dao.SyncOutboxDao
import com.example.fundsmanager.data.remote.ApiConfig
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SyncPushResult(
    val idempotency_key: String,
    val entity_uuid: String,
    val status: String,
    val server_id: String? = null,
    val reason: String? = null
)

@Serializable
data class SyncPushResponse(
    val results: List<SyncPushResult>
)

@Serializable
data class SyncChangesResponse(
    val server_time: String,
    val next_cursor: String,
    val changes: SyncChangesData
)

@Serializable
data class SyncChangesData(
    val transactions: List<JsonObject>
)

@Singleton
class SyncApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionManager: SessionManager,
    private val appLogger: AppLogger
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun pushOperations(operations: List<JsonObject>): Result<SyncPushResponse> {
        return runCatching {
            val session = sessionManager.activeSession.first()
            val token = session?.accessToken ?: throw IllegalStateException("Not authenticated")
            val deviceUuid = session.deviceUuid

            appLogger.info(AppLogCategory.SYNC, "SyncApi", "push_start", "Pushing ${operations.size} operations")

            val response = httpClient.post("${ApiConfig.baseUrl}/sync/push") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(mapOf(
                    "device_uuid" to deviceUuid,
                    "operations" to operations
                ))
            }

            if (!response.status.isSuccess()) {
                appLogger.warning(AppLogCategory.SYNC, "SyncApi", "push_failed",
                    "Push failed: ${response.status.value}")
                return Result.failure(Exception("Push failed: ${response.status.value}"))
            }

            val body = response.bodyAsText()
            val result = json.decodeFromString<SyncPushResponse>(body)
            appLogger.info(AppLogCategory.SYNC, "SyncApi", "push_complete",
                "Push complete: ${result.results.size} results")
            result
        }
    }

    suspend fun pullChanges(since: String? = null): Result<SyncChangesResponse> {
        return runCatching {
            val session = sessionManager.activeSession.first()
            val token = session?.accessToken ?: throw IllegalStateException("Not authenticated")
            val deviceUuid = session.deviceUuid

            appLogger.info(AppLogCategory.SYNC, "SyncApi", "pull_start",
                "Pulling changes since $since")

            var url = "${ApiConfig.baseUrl}/sync/changes?device_uuid=$deviceUuid"
            if (since != null) url += "&since=$since"

            val response = httpClient.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (!response.status.isSuccess()) {
                appLogger.warning(AppLogCategory.SYNC, "SyncApi", "pull_failed",
                    "Pull failed: ${response.status.value}")
                return Result.failure(Exception("Pull failed: ${response.status.value}"))
            }

            val body = response.bodyAsText()
            val result = json.decodeFromString<SyncChangesResponse>(body)
            appLogger.info(AppLogCategory.SYNC, "SyncApi", "pull_complete",
                "Pull complete: ${result.changes.transactions.size} transactions")
            result
        }
    }
}
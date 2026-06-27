package com.example.fundsmanager.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fundsmanager.data.local.dao.SyncOutboxDao
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncApiService: SyncApiService,
    private val appLogger: AppLogger
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        appLogger.info(AppLogCategory.SYNC, "SyncWorker", "sync_start", "Starting sync cycle")

        // 1. Push pending operations
        val pendingOps = syncOutboxDao.getPending()
        if (pendingOps.isNotEmpty()) {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
            val operations = pendingOps.map { op ->
                buildJsonObject {
                    put("idempotency_key", op.idempotencyKey)
                    put("entity_type", op.entityType)
                    put("entity_uuid", op.entityUuid)
                    put("operation", op.operation)
                    val payload = try {
                        json.parseToJsonElement(op.payloadJson).jsonObject
                    } catch (e: Exception) {
                        appLogger.warning(AppLogCategory.SYNC, "SyncWorker", "payload_parse_error",
                            "Failed to parse payload for ${op.entityUuid}: ${e.message}")
                        kotlinx.serialization.json.JsonObject(emptyMap())
                    }
                    put("payload", payload)
                }
            }

            syncApiService.pushOperations(operations)
                .onSuccess { response ->
                    response.results.forEach { result ->
                        when (result.status) {
                            "ACCEPTED" -> {
                                val op = pendingOps.find { it.idempotencyKey == result.idempotency_key }
                                op?.let { syncOutboxDao.markSynced(it.id) }
                            }
                            "DUPLICATE" -> {
                                val op = pendingOps.find { it.idempotencyKey == result.idempotency_key }
                                op?.let { syncOutboxDao.markSynced(it.id) }
                            }
                            "REJECTED" -> {
                                val op = pendingOps.find { it.idempotencyKey == result.idempotency_key }
                                op?.let {
                                    syncOutboxDao.markRejected(it.id, result.reason ?: "Rejected")
                                }
                            }
                        }
                    }
                }
                .onFailure {
                    appLogger.warning(AppLogCategory.SYNC, "SyncWorker", "push_error",
                        "Push failed: ${it.message}")
                    return Result.retry()
                }
        }

        // 2. Pull changes
        syncApiService.pullChanges()
            .onSuccess {
                appLogger.info(AppLogCategory.SYNC, "SyncWorker", "pull_done",
                    "Pulled ${it.changes.transactions.size} changes")
            }
            .onFailure {
                appLogger.warning(AppLogCategory.SYNC, "SyncWorker", "pull_error",
                    "Pull failed: ${it.message}")
                return Result.retry()
            }

        appLogger.info(AppLogCategory.SYNC, "SyncWorker", "sync_complete", "Sync cycle complete")
        return Result.success()
    }
}
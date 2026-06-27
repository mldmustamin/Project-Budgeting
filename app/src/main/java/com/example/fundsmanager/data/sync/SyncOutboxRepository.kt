package com.example.fundsmanager.data.sync

import com.example.fundsmanager.data.local.dao.SyncOutboxDao
import com.example.fundsmanager.data.local.entity.SyncOutboxEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncOutboxRepository @Inject constructor(
    private val dao: SyncOutboxDao
) {
    suspend fun enqueue(
        localUserId: Long,
        userUuid: String,
        deviceId: String,
        entityUuid: String,
        operation: String,
        payloadJson: String
    ) {
        val idempotencyKey = "$userUuid:$deviceId:${UUID.randomUUID()}"
        val entity = SyncOutboxEntity(
            localUserId = localUserId,
            serverUserId = userUuid.ifBlank { null },
            userUuid = userUuid.ifBlank { null },
            deviceId = deviceId.ifBlank { null },
            sessionId = userUuid.ifBlank { null },
            entityType = "transaction",
            entityUuid = entityUuid,
            operation = operation,
            payloadJson = payloadJson,
            idempotencyKey = idempotencyKey
        )
        dao.insert(entity)
    }
}
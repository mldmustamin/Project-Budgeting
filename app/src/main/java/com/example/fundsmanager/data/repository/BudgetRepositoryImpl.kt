package com.example.fundsmanager.data.repository

import com.example.fundsmanager.data.local.SessionManager
import com.example.fundsmanager.data.local.dao.*
import com.example.fundsmanager.data.local.entity.BudgetTemplateEntity
import com.example.fundsmanager.data.local.entity.MasterLocationEntity
import com.example.fundsmanager.data.local.entity.TaskExpenseEntity
import com.example.fundsmanager.data.mapper.*
import com.example.fundsmanager.data.remote.ApiConfig
import com.example.fundsmanager.data.sync.SyncOutboxRepository
import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.BudgetRepository
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val taskExpenseDao: TaskExpenseDao,
    private val expenseItemDao: ExpenseItemDao,
    private val budgetTemplateDao: BudgetTemplateDao,
    private val masterLocationDao: MasterLocationDao,
    private val syncOutboxRepository: SyncOutboxRepository,
    private val sessionManager: SessionManager,
    private val httpClient: HttpClient,
    private val appLogger: AppLogger
) : BudgetRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun getMyTasks(userId: Long): Flow<List<BudgetTask>> {
        return taskExpenseDao.getByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksByStage(stage: String): Flow<List<BudgetTask>> {
        return taskExpenseDao.getByStage(stage).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTaskByUuid(uuid: String): BudgetTask? {
        val entity = taskExpenseDao.getByUuid(uuid) ?: return null
        val items = expenseItemDao.getByTaskSync(entity.id)
        return entity.toDomain(items.map { it.toDomain() })
    }

    override suspend fun createDraft(task: BudgetTask, items: List<ExpenseItemModel>): BudgetTask {
        val entity = task.toEntity()
        val taskId = taskExpenseDao.insert(entity)

        var totalEstimated = 0L
        items.forEachIndexed { index, item ->
            val itemEntity = item.toEntity(taskId).copy(sortOrder = index)
            expenseItemDao.insert(itemEntity)
            totalEstimated += item.estimatedAmount
        }

        val created = entity.copy(id = taskId, totalEstimated = totalEstimated)
        return created.toDomain(items)
    }

    override suspend fun updateDraft(task: BudgetTask, items: List<ExpenseItemModel>): BudgetTask {
        val entity = task.toEntity()
        taskExpenseDao.update(entity)

        val keptIds = items.mapNotNull { it.id.takeIf { id -> id > 0 } }
        if (keptIds.isNotEmpty()) {
            expenseItemDao.deleteNotIn(task.id, keptIds)
        }

        var totalEstimated = 0L
        items.forEachIndexed { index, item ->
            val itemEntity = item.toEntity(task.id).copy(sortOrder = index)
            if (item.id > 0) expenseItemDao.update(itemEntity)
            else expenseItemDao.insert(itemEntity)
            totalEstimated += item.estimatedAmount
        }

        return entity.copy(totalEstimated = totalEstimated).toDomain(items)
    }

    override suspend fun deleteDraft(taskId: Long) {
        taskExpenseDao.softDelete(taskId)
    }

    override suspend fun submitTask(taskId: Long): BudgetTask {
        val entity = taskExpenseDao.getByUuid(taskId.toString())
            ?: throw IllegalStateException("Task not found")
        val updated = entity.copy(stage = "ESTIMASI", syncStatus = "PENDING")
        taskExpenseDao.update(updated)
        enqueueOutbox(updated, "SUBMIT")
        return updated.toDomain()
    }

    override fun getTemplates(): Flow<List<BudgetTemplate>> {
        return budgetTemplateDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun refreshTemplates() {
        try {
            val session = sessionManager.activeSession.first() ?: return
            val response = httpClient.get("${ApiConfig.baseUrl}/budget-templates") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
            if (response.status.isSuccess()) {
                val body: TemplateListResponse = response.body()
                budgetTemplateDao.deleteAll()
                budgetTemplateDao.insertAll(body.data.map {
                    BudgetTemplateEntity(
                        uuid = it.uuid, categoryName = it.category_name,
                        categoryGroup = it.category_group, paguType = it.pagu_type,
                        paguAmount = it.pagu_amount?.toLong(), paguNote = it.pagu_note,
                        requiresBill = it.requires_bill, billNote = it.bill_note,
                        displayOrder = it.display_order, isActive = it.is_active
                    )
                })
            }
        } catch (e: Exception) {
            appLogger.warning(AppLogCategory.SYNC, "BudgetRepo", "refresh_templates_failed", e.message ?: "unknown")
        }
    }

    override fun getLocations(projectId: Long): Flow<List<MasterLocation>> {
        return masterLocationDao.getByProject(projectId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun refreshLocations() {
        try {
            val session = sessionManager.activeSession.first() ?: return
            // Fetch from first project
            val response = httpClient.get("${ApiConfig.baseUrl}/projects/1/locations") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
            if (response.status.isSuccess()) {
                val body: LocationListResponse = response.body()
                masterLocationDao.deleteAll()
                masterLocationDao.insertAll(body.data.map {
                    MasterLocationEntity(
                        uuid = it.uuid, projectId = it.project_id,
                        remoteName = it.remote_name, address = it.address,
                        provinsi = it.provinsi, kotaKab = it.kota_kab,
                        latitude = it.latitude?.toDouble(),
                        longitude = it.longitude?.toDouble()
                    )
                })
            }
        } catch (e: Exception) {
            appLogger.warning(AppLogCategory.SYNC, "BudgetRepo", "refresh_locations_failed", e.message ?: "unknown")
        }
    }

    private fun enqueueOutbox(task: TaskExpenseEntity, operation: String) {
        val payload = json.encodeToString(TaskExpenseDto.serializer(), TaskExpenseDto(
            uuid = task.uuid, task_no = task.taskNo, vid = task.vid,
            stage = task.stage, job_type = task.jobType, project_id = task.projectId,
            submitted_by = task.submittedBy
        ))
        kotlinx.coroutines.runBlocking {
            syncOutboxRepository.enqueue(
                localUserId = task.submittedBy,
                userUuid = "",
                deviceId = "",
                entityUuid = task.uuid,
                operation = operation,
                payloadJson = payload
            )
        }
    }

    @Serializable
    data class TaskExpenseDto(
        val uuid: String, val task_no: String, val vid: String,
        val stage: String, val job_type: String, val project_id: Long,
        val submitted_by: Long
    )

    @Serializable
    data class TemplateDto(
        val uuid: String, val category_name: String, val category_group: String,
        val pagu_type: String, val pagu_amount: Long? = null, val pagu_note: String? = null,
        val requires_bill: Boolean = false, val bill_note: String? = null,
        val display_order: Int = 0, val is_active: Boolean = true
    )

    @Serializable
    data class TemplateListResponse(val data: List<TemplateDto>)

    @Serializable
    data class LocationDto(
        val uuid: String, val project_id: Long, val remote_name: String,
        val address: String, val provinsi: String? = null, val kota_kab: String? = null,
        val latitude: String? = null, val longitude: String? = null
    )

    @Serializable
    data class LocationListResponse(val data: List<LocationDto>)
}

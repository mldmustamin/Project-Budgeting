package com.example.fundsmanager.domain.usecase

import com.example.fundsmanager.domain.model.*
import com.example.fundsmanager.domain.repository.FundsRepository
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject

class ImportTrackerDuitUseCase @Inject constructor(
    private val repository: FundsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getPreview(jsonContent: String, userId: Long): ImportPreview {
        return try {
            val backup = json.decodeFromString<LegacyBackup>(jsonContent)
            val legacyProjects = mutableListOf<LegacyProject>()
            
            // Support both projects[] and users[].projects[]
            backup.projects?.let { legacyProjects.addAll(it) }
            backup.users?.forEach { it.projects.let { p -> legacyProjects.addAll(p) } }
            
            if (legacyProjects.isEmpty()) {
                return ImportPreview(0, 0, 0, 0, 0, 0, 0, emptyList(), listOf("No projects found in JSON"))
            }

            processLegacyProjects(legacyProjects)
        } catch (e: Exception) {
            ImportPreview(0, 0, 0, 0, 0, 0, 0, emptyList(), listOf("Invalid JSON format: ${e.message}"))
        }
    }

    suspend fun executeImport(preview: ImportPreview, userId: Long) {
        repository.runInTransaction {
            val validItems = preview.items.filter { it.status == ImportItemStatus.VALID }
            val itemsByProject = validItems.groupBy { it.legacyProjectName }
            
            for ((projectName, items) in itemsByProject) {
                val project = repository.getOrCreateProject(userId, projectName)
                
                val transactionsToInsert = items.map { item ->
                    val account = if (item.source.isNullOrBlank()) {
                        repository.getDefaultCashAccount(userId)
                    } else {
                        repository.getOrCreateAccount(userId, item.source)
                    }
                    
                    Transaction(
                        id = 0,
                        userId = userId,
                        projectId = project.id,
                        accountId = account.id,
                        categoryId = null,
                        type = item.type,
                        date = item.date,
                        description = item.description,
                        reportedAmount = item.reportedAmount,
                        realAmount = item.realAmount,
                        sourceText = item.source,
                        note = item.note,
                        legacyHash = item.legacyHash
                    )
                }
                repository.insertTransactions(transactionsToInsert)
            }
        }
    }

    private suspend fun processLegacyProjects(
        projects: List<LegacyProject>
    ): ImportPreview {
        val items = mutableListOf<ImportPreviewItem>()
        val seenHashesInFile = mutableSetOf<String>()
        
        var fundInCount = 0
        var officeCount = 0
        var personalCount = 0
        var validCount = 0
        var invalidCount = 0
        var duplicateCount = 0
        
        projects.forEach { legacyProject ->
            val projectName = legacyProject.name
            
            // fundEntries
            legacyProject.fundEntries.forEach { entry ->
                fundInCount++
                val description = entry.note ?: if (!entry.source.isNullOrBlank()) "Fund In from ${entry.source}" else "Fund In"
                val item = createPreviewItem(projectName, TransactionType.FUND_IN, entry.date, description, entry.source, entry.amount, entry.amount, entry.note)
                val processed = validateAndCheckDuplicate(item, seenHashesInFile)
                items.add(processed)
                updateCounts(processed, { validCount++ }, { invalidCount++ }, { duplicateCount++ })
            }
            
            // officeExpenses
            legacyProject.officeExpenses.forEach { expense ->
                officeCount++
                val reported = expense.amount
                val real = expense.realAmount ?: expense.amount
                val item = createPreviewItem(projectName, TransactionType.OFFICE_EXPENSE, expense.date, expense.description, expense.source, reported, real, expense.note)
                val processed = validateAndCheckDuplicate(item, seenHashesInFile)
                items.add(processed)
                updateCounts(processed, { validCount++ }, { invalidCount++ }, { duplicateCount++ })
            }
            
            // personalExpenses
            legacyProject.personalExpenses.forEach { expense ->
                personalCount++
                val item = createPreviewItem(projectName, TransactionType.PERSONAL_EXPENSE, expense.date, expense.description, expense.source, expense.amount, expense.amount, expense.note)
                val processed = validateAndCheckDuplicate(item, seenHashesInFile)
                items.add(processed)
                updateCounts(processed, { validCount++ }, { invalidCount++ }, { duplicateCount++ })
            }
        }

        return ImportPreview(
            projectCount = projects.size,
            fundInCount = fundInCount,
            officeExpenseCount = officeCount,
            personalExpenseCount = personalCount,
            validCount = validCount,
            invalidCount = invalidCount,
            duplicateCount = duplicateCount,
            items = items
        )
    }

    private fun createPreviewItem(
        projectName: String,
        type: TransactionType,
        date: String,
        description: String,
        source: String?,
        reported: Long,
        real: Long,
        note: String?
    ): ImportPreviewItem {
        val hash = generateHash(projectName, type, date, description, reported, real, source)
        return ImportPreviewItem(
            legacyProjectName = projectName,
            type = type,
            date = date,
            description = description,
            source = source,
            reportedAmount = reported,
            realAmount = real,
            note = note,
            legacyHash = hash,
            status = ImportItemStatus.VALID
        )
    }

    private suspend fun validateAndCheckDuplicate(
        item: ImportPreviewItem,
        seenHashesInFile: MutableSet<String>
    ): ImportPreviewItem {
        // 1. Basic Validation
        if (item.legacyProjectName.isBlank()) return item.copy(status = ImportItemStatus.INVALID, error = "Project name is blank")
        if (!item.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return item.copy(status = ImportItemStatus.INVALID, error = "Invalid date format (must be yyyy-MM-dd)")
        if (item.reportedAmount <= 0) return item.copy(status = ImportItemStatus.INVALID, error = "Amount must be greater than zero")
        if (item.type != TransactionType.FUND_IN && item.description.isBlank()) return item.copy(status = ImportItemStatus.INVALID, error = "Description is required for expenses")
        
        val hash = item.legacyHash ?: return item.copy(status = ImportItemStatus.INVALID, error = "Hash generation failed")

        // 2. Duplicate Check in File
        if (seenHashesInFile.contains(hash)) {
            return item.copy(status = ImportItemStatus.DUPLICATE, error = "Duplicate record in same file")
        }
        seenHashesInFile.add(hash)

        // 3. Duplicate Check in Database
        if (repository.getTransactionByHash(hash) != null) {
            return item.copy(status = ImportItemStatus.DUPLICATE, error = "Record already exists in database")
        }

        return item
    }

    private fun updateCounts(item: ImportPreviewItem, onValid: () -> Unit, onInvalid: () -> Unit, onDuplicate: () -> Unit) {
        when (item.status) {
            ImportItemStatus.VALID -> onValid()
            ImportItemStatus.INVALID -> onInvalid()
            ImportItemStatus.DUPLICATE -> onDuplicate()
        }
    }

    private fun generateHash(
        projectName: String,
        type: TransactionType,
        date: String,
        description: String,
        reported: Long,
        real: Long,
        source: String?
    ): String {
        val input = projectName.trim().lowercase() + "|" +
                type.name + "|" +
                date + "|" +
                description.trim().lowercase() + "|" +
                reported + "|" +
                real + "|" +
                (source ?: "").trim().lowercase()
        
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

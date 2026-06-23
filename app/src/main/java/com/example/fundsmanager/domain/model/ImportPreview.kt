package com.example.fundsmanager.domain.model

data class ImportPreview(
    val projectCount: Int,
    val fundInCount: Int,
    val officeExpenseCount: Int,
    val personalExpenseCount: Int,
    val validCount: Int,
    val invalidCount: Int,
    val duplicateCount: Int,
    val items: List<ImportPreviewItem>,
    val errors: List<String> = emptyList()
)

data class ImportPreviewItem(
    val legacyProjectName: String,
    val type: TransactionType,
    val date: String,
    val description: String,
    val source: String?,
    val reportedAmount: Long,
    val realAmount: Long,
    val note: String?,
    val legacyHash: String?,
    val status: ImportItemStatus,
    val error: String? = null
)

enum class ImportItemStatus {
    VALID,
    INVALID,
    DUPLICATE
}

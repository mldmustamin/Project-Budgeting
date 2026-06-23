package com.example.fundsmanager.domain.model

data class ProjectReportData(
    val project: Project,
    val summary: ProjectSummary,
    val transactions: List<TransactionReportRow>,
    val exportedAt: String
)

data class TransactionReportRow(
    val date: String,
    val typeLabel: String,
    val description: String,
    val accountName: String,
    val categoryName: String?,
    val reportedAmount: Long,
    val realAmount: Long,
    val saving: Long,
    val note: String?,
    val receiptStatus: String
)

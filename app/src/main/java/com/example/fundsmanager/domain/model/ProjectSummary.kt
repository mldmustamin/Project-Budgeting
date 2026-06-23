package com.example.fundsmanager.domain.model

data class ProjectSummary(
    val projectId: Long,
    val projectName: String,
    val totalFundIn: Long,
    val totalOfficeReported: Long,
    val totalOfficeReal: Long,
    val totalPersonalExpense: Long,
    val saving: Long,
    val remainingReported: Long,
    val remainingReal: Long,
    val totalCashOut: Long,
    val netPosition: Long
)

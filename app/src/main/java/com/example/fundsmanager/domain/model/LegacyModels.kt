package com.example.fundsmanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LegacyBackup(
    val version: Int? = null,
    val exportedAt: String? = null,
    val users: List<LegacyUser>? = null,
    val projects: List<LegacyProject>? = null
)

@Serializable
data class LegacyUser(
    val name: String,
    val projects: List<LegacyProject> = emptyList()
)

@Serializable
data class LegacyProject(
    val name: String,
    val description: String? = null,
    val fundEntries: List<LegacyFundEntry> = emptyList(),
    val officeExpenses: List<LegacyOfficeExpense> = emptyList(),
    val personalExpenses: List<LegacyPersonalExpense> = emptyList()
)

@Serializable
data class LegacyFundEntry(
    val date: String,
    val amount: Long,
    val source: String? = null,
    val note: String? = null
)

@Serializable
data class LegacyOfficeExpense(
    val date: String,
    val description: String,
    val amount: Long,
    val realAmount: Long? = null,
    val category: String? = null,
    val source: String? = null,
    val note: String? = null
)

@Serializable
data class LegacyPersonalExpense(
    val date: String,
    val description: String,
    val amount: Long,
    val category: String? = null,
    val source: String? = null,
    val note: String? = null
)

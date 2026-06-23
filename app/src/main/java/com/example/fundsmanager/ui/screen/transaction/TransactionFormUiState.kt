package com.example.fundsmanager.ui.screen.transaction

import com.example.fundsmanager.domain.model.Account
import com.example.fundsmanager.domain.model.Category
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.defaultTransactionType

data class TransactionFormUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val type: TransactionType = defaultTransactionType(),
    val date: String = "",
    val description: String = "",
    val reportedAmount: String = "",
    val realAmount: String = "",
    val note: String = "",
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val attachments: List<com.example.fundsmanager.domain.model.Attachment> = emptyList(),
    val pendingAttachmentNames: List<String> = emptyList(),
    val isSaved: Boolean = false,
    val duplicateWarning: String? = null,
    val error: String? = null
)

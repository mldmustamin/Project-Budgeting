package com.example.fundsmanager.ui.screen.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.usecase.GetProjectLedgerUseCase
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionListItem(
    val transaction: Transaction,
    val hasReceipt: Boolean,
    val accountName: String,
    val categoryName: String?
)

data class TransactionListUiState(
    val projectId: Long = 0L,
    val items: List<TransactionListItem> = emptyList(),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val onlyMissingReceipt: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FundsRepository,
    private val getProjectLedgerUseCase: GetProjectLedgerUseCase,
    private val appLogger: AppLogger
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: run {
        val error = IllegalStateException("Missing projectId route argument")
        appLogger.error(
            category = AppLogCategory.NAVIGATION,
            screen = "DaftarTransaksi",
            action = "route_argument_missing",
            message = "Missing projectId route argument",
            throwable = error
        )
        -1L
    }
    private var allItems: List<TransactionListItem> = emptyList()

    private val _uiState = MutableStateFlow(TransactionListUiState(projectId = projectId, isLoading = true))
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        appLogger.info(
            category = AppLogCategory.TRANSACTION,
            screen = "DaftarTransaksi",
            action = "open_transaction_list",
            message = "Transaction list opened",
            details = "projectId=$projectId"
        )
        loadItems()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onTypeSelected(type: TransactionType?) {
        _uiState.update { it.copy(selectedType = type) }
        applyFilters()
    }

    fun onOnlyMissingReceiptChange(onlyMissingReceipt: Boolean) {
        _uiState.update { it.copy(onlyMissingReceipt = onlyMissingReceipt) }
        applyFilters()
    }

    fun softDeleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                repository.softDeleteTransaction(transactionId)
                appLogger.info(
                    category = AppLogCategory.TRANSACTION,
                    screen = "DaftarTransaksi",
                    action = "soft_delete_transaction_success",
                    message = "Transaction soft-deleted",
                    details = "projectId=$projectId transactionId=$transactionId"
                )
                loadItems()
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.TRANSACTION,
                    screen = "DaftarTransaksi",
                    action = "soft_delete_transaction_error",
                    message = "Failed to soft-delete transaction",
                    throwable = e,
                    details = "projectId=$projectId transactionId=$transactionId"
                )
            }
        }
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val accounts = repository.getAllAccounts().first().associateBy { it.id }
            val categories = repository.getAllCategories().first().associateBy { it.id }
            val items = getProjectLedgerUseCase(projectId)
                .sortedByDescending { it.date }
            val attachmentIds = repository.getTransactionIdsWithAttachments(items.map { it.id })

            allItems = items.map { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    hasReceipt = transaction.id in attachmentIds,
                    accountName = transaction.accountId?.let { accounts[it]?.name }.orEmpty(),
                    categoryName = transaction.categoryId?.let { categories[it]?.name }
                )
            }
            applyFilters(isLoading = false)
        }
    }

    private fun applyFilters(isLoading: Boolean = _uiState.value.isLoading) {
        val current = _uiState.value
        val query = current.searchQuery.trim().lowercase()
        val filtered = allItems.filter { item ->
            val matchesType = current.selectedType == null || item.transaction.type == current.selectedType
            val matchesReceipt = !current.onlyMissingReceipt || !item.hasReceipt
            val matchesQuery = query.isBlank() ||
                item.transaction.description.lowercase().contains(query) ||
                item.transaction.note.orEmpty().lowercase().contains(query) ||
                item.transaction.date.contains(query)
            matchesType && matchesReceipt && matchesQuery
        }
        _uiState.update { it.copy(items = filtered, isLoading = isLoading) }
    }
}

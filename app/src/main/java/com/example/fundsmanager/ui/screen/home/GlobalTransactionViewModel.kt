package com.example.fundsmanager.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Project
import com.example.fundsmanager.domain.model.Attachment
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.service.FileStorageService
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
import java.io.InputStream
import javax.inject.Inject

data class GlobalTransactionItem(
    val transaction: Transaction,
    val projectName: String,
    val accountName: String,
    val categoryName: String?,
    val hasReceipt: Boolean
)

data class GlobalTransactionUiState(
    val projects: List<Project> = emptyList(),
    val items: List<GlobalTransactionItem> = emptyList(),
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class GlobalTransactionViewModel @Inject constructor(
    private val repository: FundsRepository,
    private val fileStorageService: FileStorageService,
    private val getProjectLedgerUseCase: GetProjectLedgerUseCase,
    private val appLogger: AppLogger
) : ViewModel() {
    private var allItems: List<GlobalTransactionItem> = emptyList()

    private val _uiState = MutableStateFlow(GlobalTransactionUiState())
    val uiState: StateFlow<GlobalTransactionUiState> = _uiState.asStateFlow()

    init { load() }

    fun refreshData() {
        load()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onTypeSelected(type: TransactionType?) {
        _uiState.update { it.copy(selectedType = type) }
        applyFilters()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                repository.softDeleteTransaction(transactionId)
                appLogger.info(
                    category = AppLogCategory.TRANSACTION,
                    screen = "TransaksiGlobal",
                    action = "delete_transaction_success",
                    message = "Transaction deleted",
                    details = "transactionId=$transactionId"
                )
                load()
                _uiState.update { it.copy(message = "Transaksi berhasil dihapus") }
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.TRANSACTION,
                    screen = "TransaksiGlobal",
                    action = "delete_transaction_error",
                    message = "Failed to delete transaction",
                    throwable = e,
                    details = "transactionId=$transactionId"
                )
                _uiState.update { it.copy(message = e.message ?: "Gagal menghapus transaksi") }
            }
        }
    }

    fun saveProof(transactionId: Long, inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            try {
                val relativePath = fileStorageService.saveFile(inputStream, fileName)
                repository.insertAttachment(
                    Attachment(
                        id = 0L,
                        transactionId = transactionId,
                        filePath = relativePath,
                        fileName = fileName,
                        mimeType = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(message = "Bukti transaksi tersimpan") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message ?: "Gagal menyimpan bukti") }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val projects = repository.getAllProjects().first().filter { !it.isArchived }
            val accounts = repository.getAllAccounts().first().associateBy { it.id }
            val categories = repository.getAllCategories().first().associateBy { it.id }
            val ledgers = projects.flatMap { project ->
                getProjectLedgerUseCase(project.id).map { transaction -> project to transaction }
            }
            val attachmentIds = repository.getTransactionIdsWithAttachments(ledgers.map { it.second.id })
            allItems = ledgers
                .sortedWith(compareByDescending<Pair<Project, Transaction>> { it.second.date }.thenByDescending { it.second.id })
                .map { (project, transaction) ->
                    GlobalTransactionItem(
                        transaction = transaction,
                        projectName = project.name,
                        accountName = transaction.accountId?.let { accounts[it]?.name }.orEmpty(),
                        categoryName = transaction.categoryId?.let { categories[it]?.name },
                        hasReceipt = transaction.id in attachmentIds
                    )
                }
            _uiState.update { it.copy(projects = projects, isLoading = false) }
            applyFilters(false)
        }
    }

    private fun applyFilters(isLoading: Boolean = _uiState.value.isLoading) {
        val current = _uiState.value
        val query = current.searchQuery.trim().lowercase()
        val filtered = allItems.filter { item ->
            val matchesType = current.selectedType == null || item.transaction.type == current.selectedType
            val matchesQuery = query.isBlank() ||
                item.transaction.description.lowercase().contains(query) ||
                item.transaction.note.orEmpty().lowercase().contains(query) ||
                item.transaction.date.contains(query) ||
                item.projectName.lowercase().contains(query)
            matchesType && matchesQuery
        }
        _uiState.update { it.copy(items = filtered, isLoading = isLoading) }
    }
}

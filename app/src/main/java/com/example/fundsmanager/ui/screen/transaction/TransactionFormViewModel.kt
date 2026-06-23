package com.example.fundsmanager.ui.screen.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Attachment
import com.example.fundsmanager.domain.model.Transaction
import com.example.fundsmanager.domain.model.TransactionType
import com.example.fundsmanager.domain.model.requiresRealAmountInput
import com.example.fundsmanager.domain.repository.FundsRepository
import com.example.fundsmanager.domain.service.FileStorageService
import com.example.fundsmanager.domain.usecase.TransactionValidationResult
import com.example.fundsmanager.domain.usecase.ValidateTransactionUseCase
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FundsRepository,
    private val fileStorageService: FileStorageService,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val appLogger: AppLogger
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: run {
        val error = IllegalStateException("Missing projectId route argument")
        appLogger.error(
            category = AppLogCategory.NAVIGATION,
            screen = "FormTransaksi",
            action = "route_argument_missing",
            message = "Missing projectId route argument",
            throwable = error
        )
        -1L
    }
    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(projectId = projectId) }
        appLogger.info(
            category = AppLogCategory.TRANSACTION,
            screen = "FormTransaksi",
            action = "open_transaction_form",
            message = if (transactionId == null) "Open add transaction form" else "Open edit transaction form",
            details = "projectId=$projectId transactionId=${transactionId ?: "-"}"
        )
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = transactionId != null) }
            
            val accounts = repository.getAllAccounts().first()
            val categories = repository.getAllCategories().first()
            val defaultType = TransactionType.FUND_IN
            
            _uiState.update { 
                it.copy(
                    accounts = accounts,
                    categories = categories,
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    selectedAccountId = accounts.firstOrNull()?.id,
                    selectedCategoryId = categories.defaultCategoryIdFor(defaultType)
                )
            }

            transactionId?.let { id ->
                repository.getTransactionById(id)?.let { tx ->
                    _uiState.update { 
                        it.copy(
                            type = tx.type,
                            flowType = if (tx.type == TransactionType.FUND_IN) TransactionFlowType.INCOME else TransactionFlowType.EXPENSE,
                            expenseSubtype = if (tx.type == TransactionType.PERSONAL_EXPENSE) ExpenseSubtype.PERSONAL else ExpenseSubtype.WORK,
                            date = tx.date,
                            description = tx.description,
                            reportedAmount = tx.reportedAmount.toString(),
                            realAmount = tx.realAmount.toString(),
                            note = tx.note ?: "",
                            selectedAccountId = tx.accountId ?: accounts.firstOrNull()?.id,
                            selectedCategoryId = tx.categoryId
                        )
                    }
                }

                viewModelScope.launch {
                    repository.getAttachmentsByTransaction(id).collectLatest { attachments ->
                        _uiState.update { it.copy(attachments = attachments) }
                    }
                }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onAttachmentSelected(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            try {
                val relativePath = fileStorageService.saveFile(inputStream, fileName)
                if (transactionId != null) {
                    val attachment = Attachment(
                        id = 0,
                        transactionId = transactionId,
                        filePath = relativePath,
                        fileName = fileName,
                        mimeType = null, // Can detect if needed
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertAttachment(attachment)
                } else {
                    _pendingAttachments.add(PendingAttachment(relativePath, fileName))
                    refreshAttachmentsPreview()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Gagal menyimpan lampiran: ${e.message}") }
            }
        }
    }

    private val _pendingAttachments = mutableListOf<PendingAttachment>()

    private fun refreshAttachmentsPreview() {
        _uiState.update { state ->
            state.copy(pendingAttachmentNames = _pendingAttachments.map { it.name })
        }
    }

    private data class PendingAttachment(val path: String, val name: String)

    fun onFlowTypeChange(flowType: TransactionFlowType) {
        _uiState.update { state ->
            val type = when (flowType) {
                TransactionFlowType.INCOME -> TransactionType.FUND_IN
                TransactionFlowType.EXPENSE -> when (state.expenseSubtype) {
                    ExpenseSubtype.WORK -> TransactionType.OFFICE_EXPENSE
                    ExpenseSubtype.PERSONAL -> TransactionType.PERSONAL_EXPENSE
                }
            }
            val newState = state.copy(
                flowType = flowType,
                type = type,
                selectedCategoryId = state.categories.defaultCategoryIdFor(type),
                error = null,
                duplicateWarning = null
            )
            if (!type.requiresRealAmountInput()) newState.copy(realAmount = state.reportedAmount) else newState
        }
    }

    fun onExpenseSubtypeChange(expenseSubtype: ExpenseSubtype) {
        _uiState.update { state ->
            val type = when (expenseSubtype) {
                ExpenseSubtype.WORK -> TransactionType.OFFICE_EXPENSE
                ExpenseSubtype.PERSONAL -> TransactionType.PERSONAL_EXPENSE
            }
            val newState = state.copy(
                expenseSubtype = expenseSubtype,
                type = type,
                selectedCategoryId = state.categories.defaultCategoryIdFor(type),
                error = null,
                duplicateWarning = null
            )
            if (!type.requiresRealAmountInput()) newState.copy(realAmount = state.reportedAmount) else newState
        }
    }

    fun onDateChange(date: String) {
        _uiState.update { it.copy(date = date, error = null, duplicateWarning = null) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(description = desc, error = null, duplicateWarning = null) }
    }

    fun onReportedAmountChange(amount: String) {
        val filtered = amount.filter { it.isDigit() }
        _uiState.update { state ->
            val newState = state.copy(reportedAmount = filtered, error = null, duplicateWarning = null)
            if (!state.type.requiresRealAmountInput()) {
                newState.copy(realAmount = filtered)
            } else newState
        }
    }

    fun onRealAmountChange(amount: String) {
        val filtered = amount.filter { it.isDigit() }
        _uiState.update { it.copy(realAmount = filtered, error = null, duplicateWarning = null) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note, error = null) }
    }

    fun onAccountChange(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId, error = null) }
    }

    fun onCategoryChange(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, error = null, duplicateWarning = null) }
    }

    fun saveTransaction(userId: Long) {
        appLogger.info(
            category = AppLogCategory.TRANSACTION,
            screen = "FormTransaksi",
            action = "save_transaction_clicked",
            message = "Save transaction requested",
            details = "mode=${if (transactionId == null) "add" else "edit"} projectId=$projectId transactionId=${transactionId ?: "-"}"
        )
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val reported = state.reportedAmount.toLongOrNull() ?: 0L
                val real = state.realAmount.toLongOrNull() ?: reported
                val existing = transactionId?.let { repository.getTransactionById(it) }
                val transaction = Transaction(
                    id = transactionId ?: 0,
                    userId = existing?.userId ?: userId,
                    projectId = projectId,
                    accountId = state.selectedAccountId,
                    categoryId = state.selectedCategoryId,
                    type = state.type,
                    date = state.date,
                    description = state.description,
                    reportedAmount = reported,
                    realAmount = real,
                    sourceText = existing?.sourceText,
                    note = state.note,
                    legacyHash = existing?.legacyHash,
                    deletedAt = existing?.deletedAt
                )

                when (val validation = validateTransactionUseCase(transaction)) {
                    TransactionValidationResult.Valid -> Unit
                    is TransactionValidationResult.Invalid -> {
                        appLogger.warning(
                            category = AppLogCategory.VALIDATION,
                            screen = "FormTransaksi",
                            action = "save_transaction_validation_failed",
                            message = validation.message,
                            details = transaction.validationDetails()
                        )
                        _uiState.update { it.copy(error = validation.message, duplicateWarning = null) }
                        return@launch
                    }
                    is TransactionValidationResult.DuplicateWarning -> {
                        if (state.duplicateWarning == null) {
                            appLogger.warning(
                                category = AppLogCategory.VALIDATION,
                                screen = "FormTransaksi",
                                action = "save_transaction_duplicate_warning",
                                message = validation.message,
                                details = transaction.validationDetails()
                            )
                            _uiState.update { it.copy(error = null, duplicateWarning = validation.message) }
                            return@launch
                        }
                    }
                }

                if (transactionId == null) {
                    val newId = repository.insertTransaction(transaction)
                    _pendingAttachments.forEach { pending ->
                        repository.insertAttachment(Attachment(
                            id = 0,
                            transactionId = newId,
                            filePath = pending.path,
                            fileName = pending.name,
                            mimeType = null,
                            createdAt = System.currentTimeMillis()
                        ))
                    }
                    appLogger.info(
                        category = AppLogCategory.TRANSACTION,
                        screen = "FormTransaksi",
                        action = "add_transaction_success",
                        message = "Transaction added",
                        details = transaction.copy(id = newId).validationDetails()
                    )
                } else {
                    repository.updateTransaction(transaction)
                    appLogger.info(
                        category = AppLogCategory.TRANSACTION,
                        screen = "FormTransaksi",
                        action = "edit_transaction_success",
                        message = "Transaction updated",
                        details = transaction.validationDetails()
                    )
                }
                _uiState.update { it.copy(isSaved = true, error = null, duplicateWarning = null) }
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.TRANSACTION,
                    screen = "FormTransaksi",
                    action = if (transactionId == null) "add_transaction_error" else "edit_transaction_error",
                    message = "Failed to save transaction",
                    throwable = e,
                    details = "projectId=$projectId transactionId=${transactionId ?: "-"}"
                )
                _uiState.update { it.copy(error = e.message ?: "Gagal menyimpan transaksi") }
            }
        }
    }

    private fun Transaction.validationDetails(): String {
        return "transactionId=$id projectId=$projectId accountId=${accountId ?: "null"} categoryId=${categoryId ?: "null"} type=$type date=$date reportedAmount=$reportedAmount realAmount=$realAmount"
    }

    private fun List<com.example.fundsmanager.domain.model.Category>.defaultCategoryIdFor(type: TransactionType): Long? {
        val targetName = when (type) {
            TransactionType.FUND_IN -> "Transfer Dana"
            TransactionType.OFFICE_EXPENSE -> "Pengeluaran Pekerjaan"
            TransactionType.PERSONAL_EXPENSE -> "Pengeluaran Pribadi"
        }
        return firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.id
    }
}

package com.example.fundsmanager.ui.screen.importbackup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.ImportItemStatus
import com.example.fundsmanager.domain.model.ImportPreview
import com.example.fundsmanager.domain.usecase.ImportTrackerDuitUseCase
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportPreviewViewModel @Inject constructor(
    private val importUseCase: ImportTrackerDuitUseCase,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportPreviewUiState>(ImportPreviewUiState.Idle)
    val uiState: StateFlow<ImportPreviewUiState> = _uiState.asStateFlow()

    fun onFileSelected(jsonContent: String, userId: Long = 1L) {
        viewModelScope.launch {
            _uiState.value = ImportPreviewUiState.Loading
            appLogger.info(
                category = AppLogCategory.IMPORT,
                screen = "ImportPreview",
                action = "import_preview_start",
                message = "Import preview started",
                details = "contentLength=${jsonContent.length}"
            )
            try {
                val preview = importUseCase.getPreview(jsonContent, userId)
                appLogger.info(
                    category = AppLogCategory.IMPORT,
                    screen = "ImportPreview",
                    action = "import_preview_result",
                    message = "Import preview completed",
                    details = preview.logDetails()
                )
                _uiState.value = ImportPreviewUiState.PreviewLoaded(preview)
            } catch (e: Exception) {
                appLogger.error(
                    category = AppLogCategory.IMPORT,
                    screen = "ImportPreview",
                    action = "import_preview_error",
                    message = "Failed to create import preview",
                    throwable = e,
                    details = "contentLength=${jsonContent.length}"
                )
                _uiState.value = ImportPreviewUiState.Error(e.message ?: "Import gagal")
            }
        }
    }

    fun confirmImport(userId: Long = 1L) {
        val currentState = _uiState.value
        if (currentState is ImportPreviewUiState.PreviewLoaded) {
            viewModelScope.launch {
                _uiState.value = ImportPreviewUiState.Loading
                try {
                    appLogger.info(
                        category = AppLogCategory.IMPORT,
                        screen = "ImportPreview",
                        action = "import_confirm_start",
                        message = "Import confirm started",
                        details = currentState.preview.logDetails()
                    )
                    importUseCase.executeImport(currentState.preview, userId)
                    appLogger.info(
                        category = AppLogCategory.IMPORT,
                        screen = "ImportPreview",
                        action = "import_confirm_success",
                        message = "Import confirm completed",
                        details = currentState.preview.logDetails()
                    )
                    _uiState.value = ImportPreviewUiState.Success
                } catch (e: Exception) {
                    appLogger.error(
                        category = AppLogCategory.IMPORT,
                        screen = "ImportPreview",
                        action = "import_confirm_error",
                        message = "Import failed and transaction should rollback",
                        throwable = e,
                        details = currentState.preview.logDetails()
                    )
                    _uiState.value = ImportPreviewUiState.Error(e.message ?: "Import failed")
                }
            }
        }
    }
}

private fun ImportPreview.logDetails(): String {
    val itemValidCount = items.count { it.status == ImportItemStatus.VALID }
    val itemInvalidCount = items.count { it.status == ImportItemStatus.INVALID }
    val itemDuplicateCount = items.count { it.status == ImportItemStatus.DUPLICATE }
    return "projectCount=$projectCount validCount=$validCount invalidCount=$invalidCount duplicateCount=$duplicateCount itemValidCount=$itemValidCount itemInvalidCount=$itemInvalidCount itemDuplicateCount=$itemDuplicateCount totalItems=${items.size}"
}

sealed interface ImportPreviewUiState {
    object Idle : ImportPreviewUiState
    object Loading : ImportPreviewUiState
    data class PreviewLoaded(val preview: ImportPreview) : ImportPreviewUiState
    object Success : ImportPreviewUiState
    data class Error(val message: String) : ImportPreviewUiState
}

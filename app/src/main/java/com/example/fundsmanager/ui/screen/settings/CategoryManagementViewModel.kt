package com.example.fundsmanager.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundsmanager.domain.model.Category
import com.example.fundsmanager.domain.repository.FundsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManagementUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val repository: FundsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return@launch
            repository.insertCategory(Category(0, trimmed, null))
        }
    }

    fun renameCategory(categoryId: Long, name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return@launch
            repository.updateCategory(Category(categoryId, trimmed, null))
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.softDeleteCategory(categoryId)
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collectLatest { categories ->
                _uiState.update {
                    it.copy(categories = categories, isLoading = false)
                }
            }
        }
    }
}

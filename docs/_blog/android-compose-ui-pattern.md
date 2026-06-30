---
layout: default
created: 2026-06-30
source: developer.android.com
tags: [android, compose, pattern, viewmodel, uistate, udf]
---

# Compose UI Pattern — Official Guide

**Sources:**
- [UI Layer](https://developer.android.com/topic/architecture/ui-layer?hl=id)
- [State Hoisting](https://developer.android.com/develop/ui/compose/state-hoisting?hl=id)
- [Data Layer](https://developer.android.com/topic/architecture/data-layer?hl=id)
- [Domain Layer](https://developer.android.com/topic/architecture/domain-layer?hl=id)

## The Standard Pattern (for EVERY screen)

```kotlin
// 1. UiState — immutable data class
data class MyUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// 2. ViewModel — state holder, business logic
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    fun onEvent(event: MyEvent) {
        when (event) {
            is MyEvent.Load -> loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.data.collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }
}

// 3. Screen — stateless composable
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold { ... }
}
```

## Key Rules

| Rule | Why |
|------|-----|
| **UiState is immutable data class** | Single snapshot, consistent, testable |
| **ViewModel exposes `StateFlow<UiState>`** | Lifecycle-aware, survives config changes |
| **Composable uses `collectAsStateWithLifecycle()`** | Auto-pause collection when off-screen |
| **UDF: state DOWN, events UP** | `onEvent(event)` pattern |
| **One UiState per screen** | Consistency, avoid scattered state |
| **`hiltViewModel()` for navigation destinations** | Scoped to nav graph back stack |

## Anti-Patterns to Avoid

| Anti-Pattern | Fix |
|--------------|-----|
| Multiple mutableStateOf in ViewModel | Use single UiState data class |
| Direct API calls in Composable | Delegate to ViewModel → Repository |
| `collectAsState()` without lifecycle | Use `collectAsStateWithLifecycle()` |
| Hardcoding values in Composables | Hoist to ViewModel UiState |
| Using Fragment for screens | Single Activity + Compose Navigation |

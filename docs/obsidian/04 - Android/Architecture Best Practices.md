---
created: 2026-06-30
source: developer.android.com
tags: [best-practice, architecture, android, compose, offline-first]
---

# Android Architecture Best Practices

**Source:** [Google Android Developers — Guide to App Architecture](https://developer.android.com/topic/architecture)

## 5 Core Principles

### 1. Separation of Concerns
> "Separate your app into methods, classes, files, packages, modules and layers with clearly defined responsibilities."
- Jangan tulis semua code di Activity/Fragment
- Activity hanya container UI
- OS bisa destroy/recreate component kapan saja → **jangan simpan state di component**

### 2. Drive UI from Data Models
> "Drive your UI from data models, preferably persistent models."
- Data models independent dari UI lifecycle
- Persistent models survive OS process kill + work offline
- Base architecture pada model classes → robustness + testability
- **This is what Room DB provides**

### 3. Single Source of Truth (SSOT)
> "Assign one owner for each data type. Only the SSOT can mutate it."
- Room DB = canonical SSOT (untuk offline-first)
- Network hanya update SSOT; UI hanya baca dari SSOT
- **Higher layers NEVER talk directly to network**

### 4. Unidirectional Data Flow (UDF)
> "State flows in only ONE direction. Events flow in the OPPOSITE direction."
```
Data sources → Repository → ViewModel → UI (state DOWN)
User action → UI events → ViewModel → Repository → Data sources (events UP)
```
- State goes DOWN melalui StateFlow
- Events go UP melalui function calls

### 5. Layered Architecture
```
┌─────────────────────────────────────────┐
│  UI Layer (Compose + ViewModel)         │ ← display + user interaction
├─────────────────────────────────────────┤
│  Domain Layer (UseCases) — OPTIONAL     │ ← complex/reusable business logic
├─────────────────────────────────────────┤
│  Data Layer (Repository + Room + API)   │ ← SSOT + business logic
└─────────────────────────────────────────┘
```

## Compose Best Practices (from official docs)

### State Hoisting Pattern
Stateless composable = reusable + testable:
```kotlin
// State hoisting: state goes DOWN, events go UP
@Composable
fun HelloContent(name: String, onNameChange: (String) -> Unit) {
    OutlinedTextField(value = name, onValueChange = onNameChange)
}
```

### ViewModel + StateFlow Pattern
```kotlin
class MyViewModel(repo: Repository) : ViewModel() {
    // Room Flow → StateFlow (lifecycle-aware)
    val items: StateFlow<List<Item>> = repo.getItemsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun MyScreen(vm: MyViewModel = hiltViewModel()) {
    // Collect with lifecycle-awareness
    val items by vm.items.collectAsStateWithLifecycle()
}
```

## Offline-First Pattern (from official docs)

### Repository = 2 data sources minimum
```kotlin
class MyRepository(
    private val dao: MyDao,         // Local (Room) — SSOT
    private val api: MyApiService    // Remote (Retrofit/Ktor) — updates SSOT
) {
    // UI reads from Room (SSOT) — ALWAYS available offline
    val data: Flow<List<Item>> = dao.getAll()

    // Background sync — updates SSOT, UI auto-updates via Flow
    suspend fun refresh() {
        val remote = api.fetch()
        remote.forEach { dao.upsert(it) }
    }
}
```

### Write Strategies (official)
| Strategy | Use Case | Pattern |
|----------|----------|---------|
| **Online-only** | Real-time transactions (bank) | Try network first, local on success |
| **Queued writes** | Offline-tolerant (survey, notes) | Save to queue → drain with WorkManager |
| **FundManager uses** | Queued writes | Outbox pattern (SyncOutboxRepository) |

### WorkManager + Periodic Sync
```kotlin
// FundManager already implements this correctly ✓
val sync = PeriodicWorkRequestBuilder<SyncWorker>(15, MINUTES)
    .setConstraints(Constraints(NetworkType.CONNECTED))
    .build()
WorkManager.enqueueUniquePeriodicWork("sync", KEEP, sync)
```

## FundManager V2 Architecture Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Separation of Concerns | ✅ | UI → ViewModel → Repository → DAO |
| Drive UI from Models | ✅ | Room Flow → StateFlow → Compose |
| Single Source of Truth | ✅ | Room DB is canonical |
| Unidirectional Data Flow | ✅ | StateFlow down, events up |
| Layered Architecture | ✅ | UI + Domain (UseCases) + Data |
| State Hoisting | ✅ | Stateless composables + ViewModel state |
| Offline-First | ✅ | Room + WorkManager + Outbox |
| Single Activity | ✅ | FundsManagerApp + NavHost |

## What We're Adding (Phase 6)

Following the SAME patterns for budget request workflow:
1. **Room entities**: TaskExpenseEntity, ExpenseItemEntity — SSOT offline
2. **DAO**: Flow<List<TaskExpense>> — reactive reads
3. **Repository**: cache-first, sync-aware — same pattern
4. **ViewModel**: StateFlow + UiState — same pattern
5. **Compose**: Stateless screens + state hoisting — same pattern
6. **Sync**: Extend SyncWorker — same pattern

**No new architecture. Just extending existing patterns.**

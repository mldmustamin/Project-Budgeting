---
layout: default
created: 2026-06-30
updated: 2026-07-01
status: production
tags: [android, compose, room, sync, architecture]
---

# Android — Jetpack Compose + Room DB

## Overview

Single APK (v2.0.0-b20, 21 MB) with role-gated UI. Kotlin 2.0 + Jetpack Compose + Room DB + Hilt + WorkManager. The app works fully offline: engineers in remote areas (mountains, offshore) fill forms, save to Room, and sync when back online.

## Architecture Pattern

```
┌────────────────────────────────────────────┐
│  UI Layer (Compose)                         │
│  Screen → ViewModel → UiState (StateFlow)   │
│  collectAsStateWithLifecycle()              │
├────────────────────────────────────────────┤
│  Domain Layer                               │
│  Repository → UseCase → Domain Models       │
│  CalculateProjectSummaryUseCase (SSOT)      │
├────────────────────────────────────────────┤
│  Data Layer                                 │
│  Room DAO → Entity → Outbox                 │
│  Ktor HttpClient → SyncService              │
├────────────────────────────────────────────┤
│  Device Layer                               │
│  WorkManager (periodic 15min)               │
│  CrashReporter (uncaught exceptions)        │
└────────────────────────────────────────────┘
```

### UDF (Unidirectional Data Flow)

ViewModel exposes `StateFlow<UiState>`. Screen collects via `collectAsStateWithLifecycle()`. User events flow up: `onClick → ViewModel.method() → repository → update StateFlow → UI recomposes`. No two-way binding, no shared mutable state.

### SSOT: CalculateProjectSummaryUseCase

All project financial summaries — both on Android Dashboard and backend API — MUST converge to the same formula. `CalculateProjectSummaryUseCase` is the Android SSOT for summary computation. Backend has its equivalent. No duplication of money logic.

## 13 Screens

| # | Screen | Composable | Roles | Path |
|---|--------|-----------|-------|------|
| 1 | Login | `LoginScreen` | All | `auth/` |
| 2 | Password Change | `PasswordChangeScreen` | All | `auth/` |
| 3 | Dashboard Summary | `SummaryScreen` | All | `dashboard/` |
| 4 | My Tasks | `MyTasksScreen` | FE | `budget/` |
| 5 | Estimate Form | `BudgetEstimateFormScreen` | FE | `budget/` |
| 6 | Realization Form | `RealizationFormScreen` | FE | `budget/` |
| 7 | Laporan Pekerjaan | `LaporanPekerjaanScreen` | FE | `budget/` |
| 8 | Supervisor Inbox | `SupervisorInboxScreen` | SUP | `budget/` |
| 9 | Assign Task | `AssignTaskScreen` | SUP | `budget/` |
| 10 | Approval | `ApprovalScreen` | OWNER | `budget/` |
| 11 | Verification | `VerificationScreen` | ADMIN/FM | `budget/` |
| 12 | Crash Logs | `CrashLogScreen` | All | `settings/` |
| 13 | Sync Monitor | `SyncMonitorScreen` | ADMIN | `settings/` |

## Room Database

### Entities

| Entity | Table | Purpose |
|--------|-------|---------|
| `TaskExpenseEntity` | `task_expenses` | Budget request header (7 stages) |
| `ExpenseItemEntity` | `expense_items` | Per-item budget lines (4-layer nominal) |
| `BudgetTemplateEntity` | `budget_item_templates` | Cached 35 categories |
| `MasterLocationEntity` | `master_locations` | Cached location list |
| `SyncOutboxEntity` | `sync_outboxes` | Pending operations queue |

### DAOs

Each entity has a DAO with standard CRUD + filtered queries. Sync DAO queries by `userId`, `deviceId`, `syncStatus`. Sum queries use `Long` exclusively — Room supports `Long` return types from aggregate functions.

### Migration Strategy

Additive only. New columns/tables added via migration, never removed. Version bumps are required for each schema change (same version = no re-trigger). Column names in raw SQL MUST match Entity property names (camelCase), not snake_case — mismatch = silent migration failure that causes runtime crashes.

## Multi-User Per Device

One APK, multiple field engineers. Every local operation records:

```kotlin
data class SessionContext(
    val localUserId: Long,
    val serverUserId: Long,
    val userUuid: String,
    val deviceId: String,
    val sessionId: String
)
```

- Login: creates new session, loads user's pending outbox
- Switch user: flushes current session, starts new
- Logout: persists pending sync (does NOT delete outbox)
- Outbox queries scoped by: `WHERE userId = ? AND deviceId = ? AND sessionId = ?`

User A's outbox never accidentally sent as User B.

## Sync Engine

```
WorkManager
  ├── PeriodicSyncWorker (every 15 min)
  │     → push outbox → pull changes → refresh Room
  └── OneShotSyncWorker (triggered after login, after submit)
        → push immediately → pull immediately
```

### Idempotency

Each outbox entry has `operationId = "{serverUserId}:{deviceId}:{count}"`. Server deduplicates on receipt. Retry after network failure does NOT create duplicate server records.

### Offline Flow

1. FE fills Estimate Form → Room INSERT (DRAFT)
2. Outbox entry created: `{operationId, type: "CREATE_TASK_EXPENSE", payload: {...}}`
3. Click SUBMIT → stage changes to ESTIMASI → new outbox entry for submit
4. If offline: toast "Menunggu jaringan..." — outbox queue persists
5. PeriodicSyncWorker picks up when network returns
6. Push succeeded → pull server changes → Room upserts by UUID
7. Toast: "3 form terkirim, 0 gagal" (success), "2 terkirim, 1 gagal: server busy" (partial)

### Pull by UUID

Server returns changes keyed by UUID. Android upserts by UUID, never by local ID. Local `Long id` is internal only, never shared with server.

## Architecture Compliance

| Rule | Status |
|------|--------|
| SSOT (Room DB) | ✅ |
| UDF (StateFlow down, events up) | ✅ |
| ViewModel + `collectAsStateWithLifecycle()` | ✅ |
| Repository pattern | ✅ |
| Single Activity + Compose Navigation | ✅ |
| Hilt DI | ✅ |
| Immutable UiState data classes | ✅ |
| CrashReporter (uncaught exception handler) | ✅ |
| Ktor HttpClient (not Retrofit/OkHttp) | ✅ |
| Money = Long exclusively | ✅ |

## Known Fixes Applied

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| Login freeze | Device registration HTTP call on UI thread | Background coroutine |
| Dashboard freeze | Room migration column name mismatch | SQL matches Entity camelCase |
| Sync retry loop | SyncManager starts before auth token | Guard behind authenticated session |
| Crash on splash | CrashReporter init before login | Defer until authenticated |
| APK version drift | Manual versionCode updates | `scripts/bump-version.sh` auto-increment |

## Build & Deploy

- Build: `./gradlew assembleRelease` → `app/build/outputs/apk/release/`
- Version: `scripts/bump-version.sh` auto-increments `versionCode` and `versionName`
- Deploy: wireless ADB via Termux (`adb connect 192.168.x.x:5555` → `adb install`)
- Current: v2.0.0-b20, 21 MB

## References

- [[android-architecture-best-practices]] — Official Android architecture guide
- [[android-compose-ui-pattern]] — ViewModel + UiState + Screen pattern
- [[architecture]] — Full system architecture
- [[backend]] — API contracts this client consumes

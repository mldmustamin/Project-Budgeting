# FundManager V2 — Repository Audit (Phase 0)

**Audit date:** 2026-06-27  
**Auditor role:** Principal Architect / Phase 0 baseline  
**Scope:** Existing Android app only — no feature implementation

---

## Project Root Detected

| Check | Result |
|-------|--------|
| Root path | `/Users/fiyyalisanna/AndroidStudioProjects/FundsManager` |
| `README.md` | YES |
| `settings.gradle.kts` | YES — `rootProject.name = "Funds Manager"`, `include(":app")` |
| `app/build.gradle.kts` | YES |
| `app/src/main/` | YES |
| Package `com.example.fundsmanager` | YES |
| Android Gradle project | YES — single-module app |

**Conclusion:** Root project is correct. No subfolder search required.

---

## Existing Architecture

```
Android App (offline-first, local-only)
├── UI Layer — Jetpack Compose Material 3 + Navigation Compose
├── ViewModel Layer — per-screen ViewModels (Hilt-injected)
├── Domain Layer — use cases, domain models, repository interfaces, services
├── Data Layer — Room DB (v7), DAOs, mappers, repository impl, file services
└── DI — Hilt (DatabaseModule, RepositoryModule, LoggingModule)
```

**Stack (Android):** Kotlin 2.1, AGP 8.10, compileSdk/targetSdk 36, minSdk 26, JDK 17, Room 2.6.1, Hilt 2.51.1, Compose BOM 2024.12.01, DataStore, Coil, kotlinx.serialization.
**Stack (Backend — locked):** Laravel 11, PostgreSQL, Redis, Sanctum, Horizon, Telescope, Pest/PHPUnit.
**Stack (Web — locked):** Laravel Blade + Livewire, Tailwind CSS, Alpine.js.

**Backend:** Scaffolded in `backend/` — Laravel 11 with Sanctum, PostgreSQL, Redis, Horizon, Telescope. Models and migrations exist for users, projects, transactions, attachments, audit events, devices, sync outboxes, accounts, categories.
**Web dashboard:** Runs in same Laravel app via Blade + Livewire, not a separate `web/` module. Blade template in `backend/resources/views/`.

**Current user model:** Single implicit local user (`id=1`, name `"Local User"`) seeded at DB create/migration. `UserPreferencesRepository` stores `active_user_id` in DataStore, now wired into `ProjectListViewModel` and `TransactionFormViewModel`.

---

## Active Routes

Defined in `Screen.kt`, wired in `FundsManagerNavHost.kt`:

| Route | Screen Composable | Params |
|-------|-------------------|--------|
| `dashboard` | `DashboardHomeScreen` | — (start destination) |
| `project_list` | `ProjectListScreen` | — |
| `transaction_home` | `GlobalTransactionScreen` | — |
| `settings` | `SettingsScreen` | — |
| `account_manager` | `AccountManagementScreen` | — |
| `category_manager` | `CategoryManagementScreen` | — |
| `project_dashboard/{projectId}` | `DashboardScreen` | `projectId: Long` |
| `transaction_list/{projectId}` | `TransactionListScreen` | `projectId: Long` |
| `transaction_form/{projectId}?transactionId=` | `TransactionFormScreen` | `projectId: Long`, `transactionId: Long?` |

Navigation uses bottom-bar pattern across main tabs (Dashboard, Project, Transaction, Settings) with `popUpTo(dashboard)` + `launchSingleTop`.

---

## Active Composables (Screens + Key Components)

**Screens:**
- `DashboardHomeScreen` + `DashboardHomeViewModel`
- `DashboardScreen` + `DashboardViewModel` + `DashboardUiState`
- `ProjectListScreen` + `ProjectListViewModel`
- `GlobalTransactionScreen` + `GlobalTransactionViewModel`
- `TransactionListScreen` + `TransactionListViewModel`
- `TransactionFormScreen` + `TransactionFormViewModel`
- `SettingsScreen`
- `AccountManagementScreen` + `AccountManagementViewModel`
- `CategoryManagementScreen` + `CategoryManagementViewModel`

**Shared UI components:** `AppButtons`, `EmptyState`, `MoneyInputField`, `ProofSourceDialog`, `AppDropdownField`, `Badges`, `StatusLabels`, `UiFormatters`

**Theme:** `FundsManagerTheme`, `Type.kt`

---

## Existing Entities (Room v7)

| Entity | Table | PK | Notable Fields |
|--------|-------|----|----------------|
| `UserEntity` | `users` | `Long id` | name, email, uuid, syncStatus, deletedAt |
| `ProjectEntity` | `projects` | `Long id` | userId FK, name, isArchived, startAt, completedAt, uuid, syncStatus, deletedAt |
| `AccountEntity` | `accounts` | `Long id` | name (no userId FK) |
| `CategoryEntity` | `categories` | `Long id` | name (no userId FK) |
| `TransactionEntity` | `transactions` | `Long id` | userId, projectId, accountId, categoryId, type, date, reportedAmount, realAmount, sourceText, note, legacyHash, uuid, serverId, deviceId, syncStatus, approvalStatus, financeStatus, lastSyncedAt, sessionId, serverUserId, userUuid, projectUuid, deletedAt |
| `AttachmentEntity` | `attachments` | `Long id` | transactionId FK, filePath, fileName, mimeType, uuid, syncStatus, deletedAt |
| `AuditLogEntity` | `audit_logs` | `Long id` | userId, entityType, entityId, action, old/new JSON |

**Transaction types:** `FUND_IN`, `OFFICE_EXPENSE`, `PERSONAL_EXPENSE`

**FMv2 sync/identity fields:** Added in v7 migration — uuid, serverId, deviceId, syncStatus, approvalStatus, financeStatus, lastSyncedAt, sessionId, projectUuid, serverUserId, userUuid present on all entities.

---

## Existing DAOs

| DAO | Key Operations |
|-----|----------------|
| `UserDao` | CRUD users |
| `ProjectDao` | Flow all projects, get by id/name, insert, update, archive, soft delete |
| `AccountDao` | Flow all, get by id/name, insert, update, soft delete |
| `CategoryDao` | Flow all, get by id, insert, update, soft delete |
| `TransactionDao` | Flow/sync by project, all sync, get by id/hash, insert (IGNORE conflict), update, soft delete |
| `AttachmentDao` | Flow by transaction, batch ids with attachments, insert |
| `AuditLogDao` | insert audit log |

---

## Existing Repository Contracts

**`FundsRepository`** — single interface covering:
- Project CRUD + archive + soft delete
- Transaction CRUD + soft delete + bulk insert + hash lookup
- Account CRUD + getOrCreate + default cash account
- Category CRUD
- Attachment insert + flow query
- `runInTransaction` atomic block

**Services:**
- `FileStorageService` — save/delete internal files
- `ReportFileRepository` — create PDF and XLSX from `ProjectReportData`

**Preferences:**
- `UserPreferencesRepository` — DataStore `active_user_id`, wired into ProjectListViewModel and TransactionFormViewModel

---

## Existing Use Cases

| Use Case | Purpose |
|----------|---------|
| `CalculateProjectSummaryUseCase` | **SSOT** project financial summary |
| `CalculateOverallSummaryUseCase` | Aggregate multiple `ProjectSummary` |
| `GetProjectLedgerUseCase` | Active transactions for project |
| `ValidateTransactionUseCase` | Field validation + duplicate warning |
| `PrepareProjectReportUseCase` | Build `ProjectReportData` for export |
| `ExportCsvUseCase` | CSV string export with summary footer |

---

## Existing Export Flow

1. `PrepareProjectReportUseCase` loads project, summary (via `CalculateProjectSummaryUseCase`), accounts, categories, transactions, attachment status.
2. **PDF:** `ReportFileRepositoryImpl.createPdf()` — Android `PdfDocument`, cache dir `reports/`.
3. **Excel:** `ReportFileRepositoryImpl.createExcel()` — manual XLSX via ZIP/XML.
4. **CSV:** `ExportCsvUseCase.execute(projectId)` — string builder with summary footer.
5. Share via Android intent (FileProvider configured in manifest).

Settings UI shows "Backup & ekspor" as **Coming soon** — export per-project from dashboard is active.

---

## Existing Financial Formula

Implemented in `CalculateProjectSummaryUseCase` (filters `deletedAt == null`):

```
totalFundIn          = sum(FUND_IN.reportedAmount)
totalOfficeReported  = sum(OFFICE_EXPENSE.reportedAmount)
totalOfficeReal      = sum(OFFICE_EXPENSE.realAmount)
totalPersonalExpense = sum(PERSONAL_EXPENSE.realAmount)
saving               = totalOfficeReported - totalOfficeReal
remainingReported    = totalFundIn - totalOfficeReported
remainingReal        = totalFundIn - totalOfficeReal
totalCashOut         = totalOfficeReal + totalPersonalExpense
netPosition          = totalFundIn - totalCashOut
```

All amounts are `Long`. Unit tests exist in `CalculateProjectSummaryUseCaseTest`, `CsvExportConsistencyTest`.

---

## Existing Test / Build Commands

```bash
./gradlew testDebugUnitTest    # unit tests (6 test files)
./gradlew assembleDebug        # debug APK → app/build/outputs/apk/debug/app-debug.apk
```

**Unit tests:**
- `CalculateProjectSummaryUseCaseTest`
- `ValidateTransactionUseCaseTest`
- `SoftDeleteBehaviorTest`
- `ExportCsvUseCaseTest`
- `CsvExportConsistencyTest`
- `ExampleUnitTest`

**Instrumented tests:**
- `ExampleInstrumentedTest`
- `DatabaseSeedTest`
- `Migration6To7Test`

---

## Mismatch Risks (Updated)

| Risk | Severity | Detail |
|------|----------|--------|
| Hardcoded `userId = 1L` in ViewModels | ~~HIGH~~ **RESOLVED** | Removed from `ProjectListViewModel` and `TransactionFormViewModel`; `UserPreferencesRepository` wired as source of truth |
| Accounts/Categories not scoped to user | MEDIUM | Shared globally; FMv2 may need per-org or per-user scoping |
| uuid/serverId on entities | ~~HIGH~~ **RESOLVED** | Present on all entities via v7 migration |
| Migrations 3→4 and 4→5 are no-ops | LOW | Reserved for experimental sync tables; safe to extend in v8+ |
| `UserPreferencesRepository` wired in 2 ViewModels | MEDIUM | Still unused in other flows (dashboard, global transaction, etc.) |
| Edit transaction on approved records (future) | HIGH | FMv2 must enforce immutability after approval |
| No network layer exists | LOW (expected) | Clean slate for sync in Phase 3+ |
| Legacy import models exist but no import use case wired | LOW | `LegacyModels.kt` present; import path TBD |
| README says backup "Coming soon" but PDF/Excel/CSV work per project | LOW | Documentation drift only |
| **Docs stale** (5 mismatches identified) | **HIGH** | Updated in this session — see task report |

---

## Files Safe to Edit (Phase 2+)

- `docs/**` — documentation
- `.cursor/rules/**` — Cursor rules
- New packages under `data/sync/`, `domain/session/`, `ui/screen/auth/` (additive)
- New Room entities/DAOs via additive migrations
- New use cases and repository methods (additive to interfaces)
- New Hilt modules
- Unit tests for new behavior

---

## Files Not to Touch (Without Explicit Approval)

- `CalculateProjectSummaryUseCase.kt` — formula SSOT; only extend with modes, do not change LOCAL_VIEW defaults
- `FundsManagerNavHost.kt` / `Screen.kt` — inspect before any route changes
- Existing migration chain `MIGRATION_1_2` … `MIGRATION_6_7` — never rewrite; add `MIGRATION_7_8`+
- `TransactionEntity` core fields — additive columns only
- `TransactionType` enum values — do not rename/remove
- Export implementations unless fixing bugs with tests

---

## Recommended Implementation Order

```
Phase 0 — Audit existing Android                    ✓ done
Phase 1 — Android compatibility foundation          ✓ done (v7 migration, uuid columns, indexes)
Phase 2 — Android multi-user session foundation     ← CURRENT (UserPreferencesRepository wired, userId=1L removed)
Phase 3 — Android sync outbox and attachment queue
Phase 4 — Backend foundation (schema scaffolded in `backend/`)
Phase 5 — Sync API
Phase 6 — Web dashboard foundation (Blade + Livewire scaffolded, welcome.blade.php exists)
Phase 7 — Advanced finance operations
Phase 8 — Reports, backup, restore
Phase 9 — Hardening and tests
```

**Next recommended task:** Phase 2 continued — add login/switch user UI, wire remaining ViewModels to `UserPreferencesRepository`, scope project queries by active user.
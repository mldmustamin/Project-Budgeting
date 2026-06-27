# Existing Android Compatibility Guide

This document defines what must be preserved during FundManager V2 evolution and what can be safely added.

---

## What Must Be Preserved

### Architecture
- Offline-first local operation — no API required to save transactions
- Room as sole local database (`funds_manager_db`, currently v7)
- Clean architecture layers: UI → ViewModel → UseCase → Repository → DAO
- Hilt dependency injection
- Jetpack Compose + Navigation Compose

### Identity & Keys
- Local primary key remains `Long id` (auto-generated) on all entities
- Package root `com.example.fundsmanager` unless explicitly migrated

### Transaction Model
- Types: `FUND_IN`, `OFFICE_EXPENSE`, `PERSONAL_EXPENSE`
- Fields: `reportedAmount`, `realAmount`, `legacyHash`, `sourceText`, `deletedAt`
- Soft delete only — never hard-delete financial records
- Money stored as `Long` (integer rupiah), never `Double`/`Float`

### Financial Logic
- `CalculateProjectSummaryUseCase` is the Android summary single source of truth
- Existing formula (see `docs/00_REPO_AUDIT.md`) must produce identical results for `LOCAL_VIEW` mode
- UI must not compute balances inline — delegate to use cases

### Export
- `PrepareProjectReportUseCase` → PDF / Excel / CSV pipeline must keep working
- `ReportFileRepositoryImpl` PDF and XLSX generation
- `ExportCsvUseCase` CSV with summary footer
- FileProvider share flow

### Navigation
- All 9 active routes in `Screen.kt` must remain functional
- Start destination: `dashboard`
- Do not add screens without inspecting `FundsManagerNavHost.kt`

### Attachments
- Internal file storage via `FileStorageService`
- `AttachmentEntity.filePath` relative to app storage
- Proof upload from camera/file in transaction form

### Audit
- Local `AuditLogEntity` writes on project/transaction create/update/soft-delete/archive

---

## What Can Be Added

### Schema (additive migrations only — v7 already applied)
The following columns were added in `MIGRATION_6_7`:
- `uuid` — global sync identity (generated on insert)
- `serverId` — server-assigned ID after sync
- `deviceId` — registering device
- `syncStatus` — PENDING | SYNCED | REJECTED | CONFLICT
- `approvalStatus` — DRAFT | PENDING | APPROVED | REJECTED | VOID
- `financeStatus` — ACTIVE | CORRECTED | VOIDED
- `lastSyncedAt`, `sessionId`, `serverUserId`, `userUuid`, `projectUuid`

### New Tables
- `sync_outbox` — per user/device/session
- `attachment_upload_queue`
- `device_registration`
- `session_state`
- `project_assignment_cache`
- `permission_snapshot`

### New Use Cases
- Calculation modes: `LOCAL_VIEW`, `FINAL_APPROVED`, `PROJECTED`
- Session management, sync push/pull, device registration
- Correction and void transaction flows (new records, not in-place edit of approved)

### New UI (additive)
- Login / switch user screen
- Sync status indicator
- Approval status badges on transactions
- Pending sync warning on logout

### New Modules (repo root)
- `backend/` — API server
- `web/` — finance control center dashboard

---

## Room Migration Principles

1. **Never** drop or rename existing columns with financial data
2. **Never** change `Long` PK to UUID
3. Add new columns with sensible defaults (`uuid` generated in app code on insert; sync columns default to `PENDING`/`DRAFT`)
4. Register new migration as `MIGRATION_X_Y`, increment `AppDatabase` version
5. Export schema JSON to `app/schemas/`
6. Test migration from previous version on instrumented test with seeded data
7. Migrations 3→4 and 4→5 are intentionally empty — do not repurpose without audit

---

## ID Compatibility Rule

| Field | Purpose |
|-------|---------|
| `id` | Local Room auto-increment PK — never sent as sole server identity |
| `uuid` | Global sync identity — generated client-side on create |
| `serverId` | Authoritative server ID after successful push |

**Pull sync rule:** Match and update rows by `uuid`, never by local `id`.

**Push sync rule:** Include `uuid` + idempotency key `{serverUserId}:{deviceId}:{operationId}`.

---

## Transaction Type Compatibility

| Type | UI Label (Indonesian) | reportedAmount | realAmount |
|------|----------------------|----------------|------------|
| `FUND_IN` | Transfer Dana / Pemasukan | fund amount | same as reported |
| `OFFICE_EXPENSE` | Pengeluaran Pekerjaan | reported to office | actual spent |
| `PERSONAL_EXPENSE` | Pengeluaran Pribadi | typically same | actual spent |

Do not add new types without updating summary use case, export, and tests.

---

## Export Compatibility

Export must continue to reflect `LOCAL_VIEW` summary until user selects another mode in UI (future).

- PDF summary cards must match `CalculateProjectSummaryUseCase` output
- CSV footer totals must match `ExportCsvUseCase` / `CsvExportConsistencyTest`
- Excel sheet1 summary rows must match `PrepareProjectReportUseCase`

When `FINAL_APPROVED` mode is added, export should allow mode selection but default to current behavior.

---

## Summary Calculation Compatibility

Before and after any schema change, run:

```bash
./gradlew testDebugUnitTest --tests "*.CalculateProjectSummaryUseCaseTest"
./gradlew testDebugUnitTest --tests "*.CsvExportConsistencyTest"
```

Any change to default calculation must be gated behind explicit mode parameter, not silent behavior change.

---

## Attachment Compatibility

- Existing `filePath` values must remain valid after migration
- Upload queue is additive — do not move files or change path scheme without migration script
- Attachments linked by `transactionId` (local Long) + future `attachmentUuid` for sync

---

## Legacy Import Compatibility

**Found:** `domain/model/LegacyModels.kt` with `LegacyBackup`, `LegacyUser`, `LegacyProject`, fund/office/personal expense structures.

**Status:** Models defined; no active import use case wired in UI.

**Compatibility rule:**
- Import must populate `legacyHash` for deduplication (unique index exists on `transactions.legacyHash`)
- Import must map legacy types to `FUND_IN` / `OFFICE_EXPENSE` / `PERSONAL_EXPENSE`
- Import must assign `userId` from active session, not hardcoded `1L`
- Preserve `source\` prefix hash strategy if already used in production data

---

## Verification Checklist (Run Before Each Phase Merge)

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew assembleDebug` succeeds
- [ ] All 9 navigation routes reachable
- [ ] Create/edit/delete transaction offline
- [ ] PDF/Excel/CSV export for sample project
- [ ] Summary totals unchanged for existing test fixtures
- [ ] No hard delete of transactions in codebase changes

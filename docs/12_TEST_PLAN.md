# Test Plan

## Phase 0 Baseline (Current)

### Unit Tests (Existing)

| Test Class | Coverage |
|------------|----------|
| `CalculateProjectSummaryUseCaseTest` | Summary formula correctness |
| `ValidateTransactionUseCaseTest` | Field validation, duplicates |
| `SoftDeleteBehaviorTest` | Deleted tx excluded from calculations |
| `ExportCsvUseCaseTest` | CSV structure |
| `CsvExportConsistencyTest` | CSV totals match summary SSOT |
| `ExampleUnitTest` | Placeholder |

### Instrumented Tests (Existing)

| Test Class | Coverage |
|------------|----------|
| `DatabaseSeedTest` | Default accounts/categories seeded |
| `ExampleInstrumentedTest` | Placeholder |

### Commands

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # requires device/emulator
./gradlew assembleDebug
```

---

## Phase 1 Tests (Planned)

- Room migration v6→v7 test with seeded data
- uuid backfill on existing rows
- Summary formula unchanged after migration
- Schema export JSON matches entity definitions

---

## Phase 2 Tests (Planned)

- Session switch does not leak project list
- Outbox scoped to active user (mock)
- Logout warning when pending sync > 0
- ActiveSessionProvider wired to ViewModels

---

## Phase 3 Tests (Planned)

- Outbox enqueue on transaction save
- Idempotency key generation
- Pull merge by uuid (unit test with fake DAO)
- Attachment queue ordering

---

## Phase 4–5 Tests (Backend — Laravel + PHPUnit)

- ✅ Auth token lifecycle: 4 tests in `tests/Feature/Api/AuthTest.php` (login success, invalid credentials, me, logout revocation)
- ✅ Project CRUD + assignment scoping: 5 tests in `tests/Feature/Api/ProjectTest.php` (create, update, role enforcement, assignment-scoped list)
- ✅ Transaction CRUD + finance rule validation: 15 tests in `tests/Feature/Api/TransactionTest.php` (list, create, UUID detail, description required/optional, decimal rejection, 4 filter types, role guard)
- ✅ Device registration: 3 tests in `tests/Feature/Api/DeviceTest.php` (register, unauthenticated rejected, UUID idempotent update)
- ✅ Transaction summary service: 4 tests in `tests/Unit/TransactionSummaryServiceTest.php` (mixed types, soft-delete ignored, empty collection, all integer outputs)
- ✅ Sync push v2: 25 tests in `tests/Feature/Api/SyncPushTest.php` (CREATE, UPDATE, SOFT_DELETE, duplicate idempotency, missing payload rejection, device ownership, revoked device, batch limit, unsupported entity/operation, mixed batch, response shape, unauthenticated, assigned/unassigned users)
- ✅ Sync pull v1: 8 tests in `tests/Feature/Api/SyncChangesTest.php` (unauthenticated, missing/revoked device, assignment scoping, OWNER sees all, since cursor, response shape)
- ✅ Sync status v1: 6 tests in `tests/Feature/Api/SyncStatusTest.php` (unauthenticated, missing/revoked device, outbox counts, null last_synced_at)
- **Total: 72 tests, 231 assertions** — all passing
- All tests use `RefreshDatabase` + seeded Spatie roles
- Pending: refresh token rotation, period closed rejection, attachment sync, Horizon jobs, Artisan commands

---

## Phase 6 Tests (Web Dashboard — PHPUnit Feature)

### DashboardTest (6 tests)
- `test_guest_is_redirected_to_login` — guest → redirect /login
- `test_authenticated_user_can_view_dashboard` — OWNER can view dashboard
- `test_dashboard_shows_pending_approval_count` — PENDING tx count visible
- `test_user_can_view_projects` — VIEWER can see project list
- `test_user_can_view_transactions_with_filters` — filter by type works
- `test_user_can_view_transaction_detail` — detail page renders

### ApprovalQueueTest (7 tests)
- `test_finance_manager_can_view_approval_queue` — FINANCE_MANAGER access
- `test_field_engineer_cannot_access_approval_queue` — 403 for FIELD_ENGINEER
- `test_finance_manager_can_approve_from_web` — approve + audit event
- `test_finance_manager_can_reject_with_reason_from_web` — reject + reason
- `test_reject_without_reason_fails_validation` — validation error
- `test_field_engineer_cannot_approve_from_web` — 403 role enforcement
- `test_admin_can_view_sync_monitor` / `test_viewer_cannot_access_sync_monitor` — role-gated

**Total web tests: 13 tests**

## Phase 7 Tests (Planned — Web + Backend)

- E2E: login → approve transaction → reflected on mobile pull (Laravel Dusk)
- Period closing enforcement tests
- Correction creates linked record
- Void excludes from FINAL_APPROVED total

---

## Regression Suite (Run Every Phase)

**Must pass before merge:**

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

**Manual smoke (Android):**
1. Launch app → dashboard loads
2. Create project → appears in list
3. Add FUND_IN + OFFICE_EXPENSE + PERSONAL_EXPENSE
4. Dashboard summary matches expected manual calculation
5. Export PDF and CSV — open and verify totals
6. Soft delete transaction — excluded from summary
7. Navigate all 9 routes without crash
8. Attach photo to transaction — displays in form

---

## Financial Correctness Tests

Golden fixtures for `CalculateProjectSummaryUseCase`:
- Empty project → all zeros
- Mixed transaction types
- Soft-deleted excluded
- Large Long amounts (overflow guard — stay within Long range)

When calculation modes added:
- LOCAL_VIEW matches current golden files exactly
- FINAL_APPROVED excludes PENDING/REJECTED/VOIDED
- PROJECTED includes PENDING

---

## Performance Targets (Phase 9)

- Summary calculation < 100ms for 1000 transactions (device)
- Sync push batch 50 ops < 5s (network permitting)
- App cold start < 2s on mid-range device

---

## CI Recommendation (Phase 4+)

### Android CI
```yaml
# .github/workflows/android.yml (to be created)
- JDK 17
- ./gradlew testDebugUnitTest assembleDebug
- Upload test reports
```

### Backend CI (Laravel)
`backend/phpunit.xml` and `backend/tests/` scaffold exist. CI workflow template:
```yaml
# .github/workflows/backend.yml (to be created)
- PHP 8.3
- composer install
- cp .env.ci .env
- php artisan key:generate
- php vendor/bin/pest
- php artisan telescope:clear
```

CI workflows not yet configured in `.github/`.

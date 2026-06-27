# FundManager V2 Production Roadmap

This file is the execution roadmap for Cline/Codex. It must stay factual, test-backed, and aligned with the current repository state.

Last verified baseline:
- Android: offline-first app exists with Room v7 sync/identity columns and initial active-user wiring.
- Backend: Laravel 11 scaffold exists in `backend/`.
- Backend auth/RBAC/device/project/transaction/sync APIs are implemented.
- Sync push supports transaction `CREATE`, `UPDATE`, and `SOFT_DELETE`.
- Sync pull and sync status endpoints are implemented.
- Backend full suite: `72 tests, 231 assertions` passing.
- Current key deferred areas: Android E2E sync test, Android password change screen, production PostgreSQL/Redis deployment, browser E2E tests.

## How Cline Must Use This File

Before every task:
1. Read `.clinerules`.
2. Read this `ROADMAP.md`.
3. Read the docs related to the task.
4. Inspect current code before editing.
5. List the exact files to change and the main risk.
6. Make the smallest complete change that satisfies the task.
7. Run the required self-check commands.
8. Update docs only after tests pass.
9. Return a factual report with test output counts.

Do not claim progress from intent. Only claim progress from code, route list, migrations, tests, and docs.

## Global Guardrails

- Do not modify Android production code when doing backend-only tasks unless explicitly required.
- Do not modify backend code when doing docs-only tasks unless explicitly required.
- Do not edit generated or dependency folders:
  - `backend/vendor/`
  - `backend/node_modules/`
  - `.gradle/`
  - `build/`
  - `app/build/`
- Do not commit secrets:
  - `.env`
  - tokens
  - private keys
  - local database files
- Do not mark an endpoint implemented unless:
  - route exists in `backend/routes/api.php`
  - controller method exists
  - feature tests exist
  - `php artisan test` passes
  - docs are updated
- Do not mark an Android flow implemented unless:
  - ViewModel/state path exists
  - UI can trigger it
  - persistence/network behavior is covered
  - Android tests or a justified manual check exist
- Use integer money only. Never introduce float/double for money.
- Preserve offline-first behavior.
- Preserve audit trail behavior for server-side financial mutations.
- Preserve idempotency for sync operations.

## Standard Self-Check Commands

Run the commands relevant to the task.

Backend full suite:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
```

Backend API route check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan route:list --path=api/v1
```

Backend sync route check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan route:list --path=api/v1/sync
```

Android unit tests:
```bash
./gradlew-local testDebugUnitTest
```

Hardcoded local user check:
```bash
rg -n "userId\s*=\s*1L|saveTransaction\(1L\)|\(1L\)" app/src/main/java
```

Docs stale-number check:
```bash
rg -n "59 tests|201 assertions|70 tests|225 assertions|UPDATE/SOFT_DELETE sync operations|Current v6|planned v7|not implemented in repo" docs
```

Git status check:
```bash
git status --short
```

## Required Report Format After Each Task

Every task report must include:
- Files changed
- Behavior added or changed
- Test command run
- Exact test result count
- Docs updated
- Deferred items preserved
- Contradictions found
- Remaining risks
- Suggested next task

Use this template:
```text
Task Complete: <name>

Files changed:
- ...

Behavior:
- ...

Verification:
- <command>
- Result: <tests>, <assertions>

Docs:
- ...

Deferred:
- ...

Contradictions/Risks:
- ...

Next recommended task:
- ...
```

## Current Production Progress Scale

| Area | Current Score | Why |
|------|--------------:|-----|
| Android offline-first app | 85/100 | Employee ID login, session, device registration, sync worker, outbox. Sync payload fixed. Password change pending. |
| Backend Laravel foundation | 92/100 | 28 API endpoints, CRUD, auth, sync, RBAC, 124 tests, 381 assertions. |
| Transaction API and sync server | 95/100 | Full lifecycle: submit, approve, reject, dispute, resolve, void, correction. Period enforcement. Sync v2. |
| Multi-user/RBAC | 88/100 | 8 roles, employee_id login, auto-password, force change, user CRUD, role assignment. |
| Web dashboard | 90/100 | 12 halaman, role-based access, universal search, dispute, period, audit, user CRUD. |
| Testing | 85/100 | 124 backend tests, CI/CD workflows. Android unit tests exist. Browser/E2E tests pending. |
| Production readiness | 60/100 | Staging seeder, deploy docs, local dev ready. PostgreSQL/Redis provisioning pending. |
| Overall project | 85/100 | Backend + Web complete. Android auth wired. Ready for staging deploy. |

## Phase 0 - Lock Current Backend Sync Progress

Goal: preserve the current backend sync foundation before starting new feature work.

Status: In progress until committed and pushed.

Tasks:
- Verify current backend test suite passes.
- Verify sync routes show push, changes, and status.
- Verify docs mention sync push v2 and `72 tests, 231 assertions`.
- Commit current backend sync v2, sync pull/status, and docs.
- Push branch to remote.

Files to inspect:
- `backend/app/Http/Controllers/Api/SyncPushController.php`
- `backend/app/Http/Controllers/Api/SyncChangesController.php`
- `backend/app/Http/Controllers/Api/SyncStatusController.php`
- `backend/app/Http/Controllers/Api/ValidatesSyncDevice.php`
- `backend/tests/Feature/Api/SyncPushTest.php`
- `backend/tests/Feature/Api/SyncChangesTest.php`
- `backend/tests/Feature/Api/SyncStatusTest.php`
- `docs/05_SYNC_ENGINE.md`
- `docs/06_BACKEND_API_CONTRACT.md`
- `docs/12_TEST_PLAN.md`
- `docs/14_RELEASE_CHECKLIST.md`

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan route:list --path=api/v1/sync
```

Definition of Done:
- Full backend suite passes.
- Git status contains only intended changes before commit.
- Branch is pushed.
- No stale docs remain.

## Phase 1 - Android Login and Session Foundation

Goal: make Android use real authenticated users instead of fallback single-user behavior.

Priority: Critical.

Tasks:
1. Add auth API models:
   - login request
   - login response
   - user DTO
   - role DTO/list
2. Add secure token/session persistence:
   - token
   - active user id
   - active user uuid
   - active roles
3. Add Android auth repository:
   - login
   - logout
   - get current session
   - restore session on app start
4. Add login screen:
   - email
   - password
   - loading state
   - error state
5. Add logout path.
6. Wire active user into existing flows:
   - project create
   - transaction create
   - dashboard queries where safe
7. Remove or isolate fallback `1L` behavior:
   - allowed only for explicit local demo mode
   - not allowed in authenticated mode
8. Update docs:
   - `docs/04_ANDROID_LOCAL_FIRST_MULTI_USER.md`
   - `docs/06_BACKEND_API_CONTRACT.md`
   - `docs/12_TEST_PLAN.md`

Likely files:
- `app/src/main/java/.../data/remote/`
- `app/src/main/java/.../data/local/UserPreferencesRepository.kt`
- `app/src/main/java/.../ui/screen/auth/`
- `app/src/main/java/.../navigation/`
- Hilt modules for API client/session dependencies

Self-check:
```bash
./gradlew-local testDebugUnitTest
rg -n "userId\s*=\s*1L|saveTransaction\(1L\)|\(1L\)" app/src/main/java
```

Definition of Done:
- User can log in from Android.
- Token is stored.
- Active user is stored.
- App can restore session.
- Logout clears token/session.
- Existing unit tests pass.
- No accidental hardcoded user id in authenticated flow.

## Phase 2 - Android API Client and Device Registration

Goal: Android can talk to the Laravel backend and register the local device.

Priority: Critical.

Tasks:
1. Add backend base URL configuration.
2. Add HTTP client:
   - auth header injection
   - JSON serialization
   - timeout handling
   - error mapping
3. Add device identity storage:
   - local generated device uuid
   - device name/platform/version
4. Call `POST /api/v1/devices/register` after login.
5. Store returned device uuid and revoked state.
6. Handle device revoked response:
   - block sync
   - show user-facing message
7. Add tests for repository serialization/error mapping if project test setup supports it.
8. Update docs.

Self-check:
```bash
./gradlew-local testDebugUnitTest
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test --filter=DeviceTest
```

Definition of Done:
- Android stores a stable device uuid.
- Device registration works after login.
- Sync calls can include `device_uuid`.
- Revoked device state is handled.

## Phase 3 - Android Outbox Enqueue

Goal: every local transaction mutation creates a sync operation that can be pushed later.

Priority: Critical.

Tasks:
1. Verify existing Room sync/outbox fields.
2. Add or complete `SyncOutboxEntity` if missing.
3. Add DAO for outbox:
   - insert
   - pending list
   - mark in flight
   - mark synced
   - mark rejected
   - increment retry
4. Enqueue operation after local transaction create.
5. Enqueue operation after local transaction update.
6. Enqueue operation after local transaction soft delete.
7. Generate idempotency key:
   - user uuid/id
   - device uuid
   - operation uuid
8. Store payload JSON matching backend sync contract.
9. Keep transaction local-first: local save must not require network.
10. Add unit tests for enqueue behavior.
11. Update docs.

Self-check:
```bash
./gradlew-local testDebugUnitTest
rg -n "SyncOutbox|idempotency|SOFT_DELETE|UPDATE|CREATE" app/src/main/java
```

Definition of Done:
- Local transaction create/update/delete produces outbox rows.
- Outbox row uses correct operation and payload.
- Existing offline app behavior remains intact.

## Phase 4 - Android Sync Worker

Goal: Android can push local outbox operations and pull server changes.

Priority: Critical.

Tasks:
1. Add WorkManager sync worker.
2. Push pending outbox to `POST /api/v1/sync/push`.
3. Apply per-operation result:
   - ACCEPTED
   - DUPLICATE
   - REJECTED
4. Pull changes from `GET /api/v1/sync/changes`.
5. Apply server changes to Room by uuid.
6. Handle soft-deleted server rows.
7. Store sync cursor.
8. Add retry/backoff.
9. Add manual "sync now" action.
10. Add sync status display using `GET /api/v1/sync/status`.
11. Add tests around mapping and state transitions.
12. Update docs.

Self-check:
```bash
./gradlew-local testDebugUnitTest
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test --filter=Sync
```

Definition of Done:
- Offline create syncs to server when online.
- Offline update syncs to server when online.
- Offline soft delete syncs to server when online.
- Pull changes appear locally.
- Retry does not duplicate server rows.

## Phase 5 - Backend Approval and Rejection Flow

Goal: make transaction approval status operational.

Priority: Critical.

Endpoints to add:
- `POST /api/v1/transactions/{transaction}/submit`
- `POST /api/v1/transactions/{transaction}/approve`
- `POST /api/v1/transactions/{transaction}/reject`

Rules:
- Draft transactions can be submitted.
- Submitted transactions become `PENDING`.
- Only authorized roles can approve/reject.
- Approved transactions become immutable for normal update.
- Rejected transactions require a reason.
- Every action writes `audit_events`.
- Sync pull must return updated approval status.

Suggested role rules:
- `FIELD_ENGINEER`: create/update draft and submit own assigned project transactions.
- `PIC` or `SUPERVISOR`: can review assigned project transactions if business rules require.
- `FINANCE_MANAGER`, `OWNER`, `ADMIN`: can approve/reject.
- `AUDITOR`, `VIEWER`: read-only.

Tests:
- submit draft success
- submit non-draft rejected
- approve pending success
- approve draft rejected
- reject pending with reason success
- reject without reason rejected
- unauthorized role denied
- approved transaction update rejected through normal API
- approved transaction update rejected through sync push
- audit event written for submit/approve/reject

Docs to update:
- `docs/06_BACKEND_API_CONTRACT.md`
- `docs/08_FINANCE_RULES.md`
- `docs/09_RBAC_SECURITY.md`
- `docs/12_TEST_PLAN.md`
- `docs/14_RELEASE_CHECKLIST.md`

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan route:list --path=api/v1/transactions
```

Definition of Done:
- Approval flow works by API.
- Role enforcement is tested.
- Approved transaction immutability is enforced.
- Docs match routes/tests.

## Phase 6 - Web Dashboard MVP

Goal: finance/admin users can operate from the browser.

Priority: Critical.

Pages:
- Login
- Dashboard summary
- Project list
- Project detail
- Transaction list
- Transaction detail
- Approval queue
- Basic sync monitor

Stack:
- Laravel Blade
- Livewire
- Tailwind CSS
- Alpine.js
- Sanctum/session auth as appropriate

Tasks:
1. Create shared web layout.
2. Add web auth/login if default auth is not present.
3. Add dashboard cards:
   - total fund in
   - office reported
   - office real
   - personal expense
   - pending approvals
4. Add transaction list with filters:
   - project
   - type
   - approval status
   - finance status
   - date range
5. Add approval queue.
6. Wire approve/reject actions.
7. Add policy checks.
8. Add Dusk or feature tests for critical pages.
9. Update docs.

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
php artisan route:list
```

Definition of Done:
- Finance user can log in.
- Finance user can view and approve/reject pending transactions.
- Unauthorized users cannot access approval actions.

## Phase 7 - Period Closing

Goal: prevent changes to closed accounting periods.

Priority: High.

Tasks:
1. Create `accounting_periods` migration/model.
2. Define period fields:
   - uuid
   - period_start
   - period_end
   - status
   - closed_by
   - closed_at
   - reopened_by
   - reopened_at
   - reason
3. Add endpoints:
   - list periods
   - close period
   - reopen period
4. Enforce period lock in:
   - transaction create
   - transaction update
   - transaction soft delete
   - sync push CREATE
   - sync push UPDATE
   - sync push SOFT_DELETE
5. Add audit events.
6. Add tests.
7. Update docs.

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
```

Definition of Done:
- Closed period rejects mutations.
- Reopen is audited.
- Sync push cannot bypass period lock.

## Phase 8 - Attachment Sync

Goal: transaction receipts/photos can be uploaded and synced.

Priority: High.

Tasks:
1. Confirm `attachments` schema.
2. Add upload endpoint:
   - `POST /api/v1/transactions/{transaction}/attachments`
3. Add download endpoint:
   - `GET /api/v1/attachments/{attachment}`
4. Add file validation:
   - size
   - mime type
   - extension
5. Store files in S3-compatible storage.
6. Store metadata in DB.
7. Add authorization:
   - assigned project or admin role
8. Add Android attachment enqueue.
9. Add attachment upload worker.
10. Add tests.
11. Update docs.

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
```

Definition of Done:
- Attachment upload works.
- Attachment download is authorized.
- Metadata syncs.
- Large/invalid files are rejected.

## Phase 9 - Correction and Void Flow

Goal: approved financial records are corrected through audited workflows, not direct edits.

Priority: High.

Tasks:
1. Decide correction strategy:
   - linked correction transactions, or
   - `transaction_corrections` table
2. Document the decision in `docs/08_FINANCE_RULES.md`.
3. Add schema/model.
4. Add endpoints:
   - request correction
   - approve correction
   - void transaction
5. Enforce no direct edit on approved records.
6. Add report logic for corrected/voided records.
7. Add tests.
8. Update docs.

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
```

Definition of Done:
- Approved records are immutable.
- Corrections and voids are audited.
- Summary/report logic handles corrected/voided records.

## Phase 10 - Reporting and Export Parity

Goal: Android, backend, and web show the same financial numbers.

Priority: Medium.

Tasks:
1. Add backend project summary endpoint.
2. Use `TransactionSummaryService` for API and web.
3. Add web reports.
4. Add backend CSV/Excel/PDF export if required.
5. Add golden test cases shared with Android formula.
6. Compare:
   - Android local summary
   - backend summary
   - web dashboard summary
7. Update docs.

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
./gradlew-local testDebugUnitTest
```

Definition of Done:
- Same dataset gives same totals across Android/backend/web.
- Export totals match visible summary.

## Phase 11 - CI/CD

Goal: every pull request is tested automatically.

Priority: Critical before beta.

Tasks:
1. Add GitHub Actions backend workflow:
   - composer install
   - migrations
   - PHPUnit
   - Pint
2. Add Android workflow:
   - Gradle test
   - lint if stable
3. Add cache config.
4. Add branch protection recommendation.
5. Add docs for local and CI test commands.

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
./gradlew-local testDebugUnitTest
```

Definition of Done:
- CI runs on PR.
- Backend tests pass in CI.
- Android unit tests pass in CI.

## Phase 12 - Staging Deployment

Goal: deploy a real staging environment.

Priority: Critical before beta.

Tasks:
1. Provision PostgreSQL.
2. Provision Redis.
3. Configure Laravel env.
4. Run migrations.
5. Seed roles.
6. Configure queue worker/Horizon.
7. Configure scheduler.
8. Configure object storage.
9. Configure logs.
10. Run smoke tests.
11. Document deploy and rollback.

Self-check:
```bash
cd backend
php artisan migrate --force
php artisan route:list
php artisan queue:failed
```

Definition of Done:
- Staging URL works.
- Login works.
- API smoke tests pass.
- Horizon is reachable for admins only.
- Backup job exists.

## Phase 13 - Security Hardening

Goal: make the system safe for financial data.

Priority: Critical before production.

Tasks:
1. Add Laravel policies for all sensitive models.
2. Add rate limiting:
   - login
   - sync push
   - attachment upload
3. Add device revoke endpoint.
4. Add token revoke-all endpoint for admins if required.
5. Confirm Telescope disabled in production.
6. Confirm no secrets in logs.
7. Add audit log review page or endpoint.
8. Add security tests for unauthorized access.
9. Update docs.

Self-check:
```bash
cd backend
DB_CONNECTION=sqlite DB_DATABASE=:memory: php artisan test
rg -n "TELESCOPE_ENABLED|APP_DEBUG|LOG_LEVEL" backend/config backend/.env.example
```

Definition of Done:
- Unauthorized access is denied and tested.
- Revoked token/device cannot sync.
- Production debug tooling is off.

## Phase 14 - Beta Pilot

Goal: run a controlled real-world trial.

Priority: High.

Pilot scenario:
1. Admin creates project.
2. Field user logs in on Android.
3. Field user creates transaction offline.
4. Android syncs when online.
5. Finance user approves transaction on web.
6. Android pulls approval status.
7. Finance exports summary/report.

Tasks:
- Prepare staging seed users.
- Prepare test devices.
- Run pilot scenario.
- Record bugs.
- Fix critical bugs.
- Re-run regression tests.

Definition of Done:
- End-to-end flow works with real devices.
- No duplicate transactions after retry.
- Approval status returns to Android.
- Export/report totals match.

## Phase 15 - Production Launch

Goal: ship the final production system.

Priority: Critical.

Tasks:
1. Production infra ready:
   - PostgreSQL
   - Redis
   - object storage
   - queue workers
   - scheduler
2. Domain and SSL.
3. Production `.env`.
4. Migrations.
5. Role seed.
6. First admin account.
7. Backups enabled.
8. Restore drill completed.
9. Monitoring enabled.
10. Error reporting enabled.
11. Release tag created.
12. Rollback plan documented.

Definition of Done:
- Production smoke test passes.
- Backup restore is verified.
- Monitoring alerts work.
- Release tag exists.

## Phase 16 - Post-Launch Operations

Goal: keep the product healthy.

Tasks:
- Weekly backup restore sample.
- Monthly dependency update.
- Slow query review.
- Error log review.
- Security patch review.
- User feedback triage.
- Roadmap refresh after real usage.

Definition of Done:
- There is a repeatable maintenance rhythm.
- Bugs can be tracked and prioritized.
- Data safety is continuously verified.

## Next 10 Recommended Executable Tasks

Use this order unless the user explicitly changes priority.

1. Commit and push current sync v2 progress.
2. Android auth/session foundation.
3. Android API client and device registration.
4. Android outbox enqueue for create/update/soft-delete.
5. Android WorkManager push/pull sync.
6. Backend approval/rejection endpoints.
7. Web dashboard MVP approval queue.
8. Period closing enforcement.
9. Attachment upload/sync.
10. CI/CD workflows.

## Cline Prompt Template

Use this template for each task.

```text
Read .clinerules first.
Then read ROADMAP.md.

Task:
<specific task name from ROADMAP.md>

Requirements:
- Follow the phase/task definition exactly.
- Inspect files before editing.
- List exact files to change and main risk before editing.
- Implement the smallest complete change.
- Add or update tests.
- Run required self-check commands.
- Update docs only after tests pass.
- Do not claim implementation unless routes/code/tests/docs agree.

Return:
- files changed
- behavior added
- exact test command and result
- docs updated
- deferred items preserved
- contradictions found
- next recommended task
```

## Compact Context Rule For Cline

Ask Cline to compact context when any of these happen:
- After a large phase completes.
- After 3 or more backend feature batches.
- After test count/docs totals change significantly.
- Before switching from backend to Android.
- Before switching from Android to web.
- When Cline starts repeating stale facts.

Before compacting, require this summary:
```text
Current repo facts:
- branch:
- latest commit:
- uncommitted files:
- implemented endpoints:
- current test totals:
- current deferred items:
- next task:
- commands last run:
```

## Final Production Definition

FundManager V2 is production-ready only when all are true:
- Android login works.
- Android offline create/update/soft-delete syncs to backend.
- Backend push/pull/status sync is stable.
- Finance approval/rejection works.
- Closed periods reject mutations.
- Web dashboard supports finance approval workflow.
- Attachments are stored and authorized.
- Reports match Android/backend formulas.
- CI runs backend and Android tests.
- Staging deployment is verified.
- Production backup and restore are verified.
- Security hardening is complete.
- Production smoke test passes.

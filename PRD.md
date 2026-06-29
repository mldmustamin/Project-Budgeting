# Product Requirements Document — FundManager V2

| Metadata | Value |
|----------|-------|
| **Product** | FundManager V2 (Project Budgeting) |
| **Status** | Draft |
| **Version** | 2.0 |
| **Last Updated** | June 2026 |

---

## 1. Executive Summary

FundManager V2 is a multi-user, offline-first field finance management platform. It replaces paper-based and spreadsheet-bound project expense tracking with a server-authoritative system comprising an Android mobile client for field engineers and a web dashboard for finance controllers. The platform supports bidirectional sync, role-based access control, transaction approval workflows, audit trails, and period accounting.

---

## 2. Problem Statement

Field project teams currently manage budgets and expenses manually:

- **No offline capture** — field engineers cannot log expenses when outside network coverage
- **No centralized audit** — finance teams lack visibility into field spending until reports are compiled days or weeks later
- **No approval workflow** — transactions are captured but never formally approved, creating reconciliation gaps
- **No access control** — shared devices expose all data to all users; no separation of duties
- **No period enforcement** — transactions can be backdated into closed accounting periods
- **No sync** — data lives on individual devices with no server-side authority

---

## 3. Target Users & Personas

| Persona | Role | Primary Interface | Key Goals |
|---------|------|-------------------|-----------|
| **Rudi** — Field Engineer | FIELD_ENGINEER | Android mobile | Capture expenses offline, attach photo receipts, sync when online, view own projects only |
| **Sari** — PIC / Supervisor | PIC / SUPERVISOR | Android + Web | Review and approve/reject team transactions, monitor project budgets |
| **Budi** — Finance Manager | FINANCE_MANAGER | Web dashboard | Approve/reject transactions, close periods, reconcile, audit |
| **Dewi** — Admin | ADMIN | Web dashboard | Manage users, roles, devices, project assignments |
| **Ani** — Auditor | AUDITOR | Web dashboard (read-only) | Review audit trail, export reports |
| **Tono** — Owner | OWNER | Web dashboard | Full org control, final approvals, org-wide KPIs |

---

## 4. Product Vision

> Any field engineer can log a project expense on a shared Android device with no internet. Any finance manager can review, approve, and reconcile that expense from a web dashboard seconds after it syncs. The server is the authoritative source for identity, roles, approvals, periods, and audit. The system enforces separation of duties and financial controls without blocking field operations.

---

## 5. Core Capabilities (Epics)

### Epic 1 — Multi-User Android (Offline-First)

Field engineers log in with Employee ID / Email on shared devices. Each session is isolated — outbox, cached projects, and UI state are scoped to the active user.

- Employee ID + email + password authentication
- Token-based session persistence (`EncryptedSharedPreferences`)
- Device registration against server
- Session switching without data cross-contamination
- Logout clears tokens from memory, preserves local DB

### Epic 2 — Offline Transaction Capture

Full CRUD of transactions while offline, stored in local Room database.

- **Transaction types:** FUND_IN, OFFICE_EXPENSE, PERSONAL_EXPENSE
- **Fields:** date, description, reportedAmount, realAmount, account, category, project
- **Attachments:** photo from camera/gallery, stored in app-private storage
- **Validation:** date format, positive amounts, required description for expenses
- **Soft delete** — no destructive deletes

### Epic 3 — Bidirectional Sync Engine

Background sync via WorkManager. Push local changes; pull server changes.

- **Push:** POST `/api/v1/sync/push` with idempotency key (`userUuid:deviceUuid:operationUuid`)
- **Pull:** GET `/api/v1/sync/changes` since cursor
- **Status:** GET `/api/v1/sync/status` for device health
- **Sync outbox:** per-user, per-device queuing with retry and backoff
- **Attachment upload queue:** separate queue, uploads after transaction sync confirms serverId
- **Operations:** CREATE, UPDATE, SOFT_DELETE
- **Conflict resolution:** server wins for approval/finance status; last-write-wins for content fields

### Epic 4 — Transaction Lifecycle & Approval

Staged workflow with immutability at approval boundary.

```
DRAFT → PENDING → APPROVED
              ↘ REJECTED
              ↘ NEED_REVISION → (edit) → PENDING
APPROVED → VOID (soft, excluded from FINAL_APPROVED)
APPROVED → CORRECTION (new linked transaction)
```

- **Submit:** DRAFT → PENDING
- **Approve:** PENDING → APPROVED (immutable)
- **Reject:** PENDING → REJECTED (with reason)
- **Need Revision:** PENDING → NEED_REVISION
- **Void:** APPROVED → VOID (finance only, audited)
- **Correction:** APPROVED → linked CORRECTION transaction
- **Approved transaction immutability:** no in-place edit of amounts, date, type, or description

### Epic 5 — Web Dashboard

Finance control center built with Laravel Blade + Livewire + Tailwind CSS.

- **Dashboard:** org-wide KPIs (total fund in, office reported/real, personal expense, pending approvals)
- **Projects:** CRUD, archive, assignment
- **Transactions:** search, filter (project, type, approval status, finance status, date range), drill-down
- **Approval Center:** approval queue with approve/reject/need-revision actions
- **Audit Trail:** multi-criteria filter, immutable action log
- **Period Management:** close/reopen accounting periods
- **User Management:** CRUD, auto-password, force change, reset
- **Device Management:** register/revoke devices
- **Sync Monitor:** device sync health, conflict queue
- **Universal search:** transaksi, project, user

### Epic 6 — Role-Based Access Control (RBAC)

6 roles enforced server-side via Spatie Laravel Permission.

| Role | Permissions |
|------|------------|
| OWNER | Manager — full org control, budget approval, final decision |
| ADMIN | User/device/project admin, rekonsiliasi data realisasi |
| FINANCE_MANAGER | Pencocokan data realisasi ke kordinator, closing, reconciliation |
| SUPERVISOR | Kordinator — budget request ke Manager, approve/reject transaksi tim |
| FIELD_ENGINEER | Mobile capture transaksi/estimasi, assigned projects only |
| AUDITOR | Read-only audit and reports |

### Wewenang Kunci
| Action | Actor |
|--------|-------|
| Budget request | SUPERVISOR |
| **Approve budget + nominal** | **OWNER only** |
| Transaksi harian | FIELD_ENGINEER, SUPERVISOR |
| Approve transaksi | FINANCE_MANAGER, SUPERVISOR |
| Rekonsiliasi realisasi | FINANCE_MANAGER, ADMIN |
| Void / correction | FINANCE_MANAGER |

Effective permission = role AND project assignment AND device valid.

### Epic 7 — Period Accounting

Closed periods prevent transactions from being created or synced within that date range.

- Server rejects push for transactions dated in closed period
- Android shows rejection reason locally
- Web admin can reopen period (audited, rare)
- Periods defined by start/end date, status, reason

### Epic 8 — Finance Rules & Reporting

- **Money:** stored as Long (Android) / BIGINT (PostgreSQL) — never floating point
- **Summary formula:** totalFundIn, totalOfficeReported, totalOfficeReal, totalPersonalExpense, saving, remainingReported, remainingReal, totalCashOut, netPosition
- **Calculation modes:** LOCAL_VIEW (all non-deleted), FINAL_APPROVED (approved only), PROJECTED (approved + pending)
- **Export:** PDF, Excel, CSV from Android
- **Reports:** advanced reporting from web dashboard

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| **Offline-first** | All critical mobile operations must work without network. Network required only for sync. |
| **Sync** | Background sync with retry and backoff. Push interval ≤ 15 minutes when online. |
| **Idempotency** | Every sync operation must be idempotent — duplicate push must not create duplicate server rows. |
| **Audit** | Every financial mutation must be auditable (who, what, when, old value, new value). |
| **Immutability** | Approved transactions cannot be edited in place. Changes via correction or void only. |
| **Performance** | Web dashboard pages load in < 2s p95. Sync payloads under 1MB. |
| **Security** | TLS everywhere. Encrypted token storage on Android. bcrypt/argon2 passwords. Presigned attachment URLs expire ≤ 15 minutes. |
| **Concurrency** | Multiple devices per user; multiple users per device — all supported. |
| **Upgrade path** | Existing Android users upgrade without data loss. |

---

## 7. System Architecture

```
┌─────────────────┐     ┌──────────────────────┐     ┌──────────────────────┐
│  Android App     │◄───►│  REST API            │◄───►│  Web Dashboard       │
│  (multi-user     │     │  Laravel 11          │     │  Blade + Livewire    │
│   offline-first) │     │  Sanctum Auth        │     │  Tailwind CSS        │
│  Kotlin/Compose  │     │  28 endpoints        │     │  12 pages            │
│  Room + Hilt     │     │  Spatie RBAC         │     │  Alpine.js           │
└─────────────────┘     └───────┬──────────────┘     └──────────────────────┘
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
            ┌──────────┐ ┌──────────┐ ┌──────────┐
            │PostgreSQL│ │  Redis   │ │  Object  │
            │ (BIGINT  │ │ Queue/   │ │ Storage  │
            │  money)  │ │ Cache    │ │ (S3-comp)│
            └──────────┘ └──────────┘ └──────────┘
```

**Android stack:** Kotlin, Jetpack Compose Material 3, Room, Hilt, Navigation Compose, DataStore, Ktor HTTP Client, WorkManager, Coil

**Backend stack:** Laravel 11, Sanctum, Spatie Permission, PostgreSQL, Redis, Horizon, Telescope

**Web stack:** Blade, Livewire, Alpine.js, Tailwind CSS

---

## 8. Data Model Overview

### Android Room (Local — v7)

| Entity | Key Columns |
|--------|------------|
| `users` | id, name, email, uuid, serverId, deviceId, syncStatus |
| `projects` | id, userId, name, uuid, isArchived, syncStatus |
| `transactions` | id, userId, projectId, type, date, reportedAmount (Long), realAmount (Long), uuid, approvalStatus, financeStatus, syncStatus |
| `attachments` | id, transactionId, filePath, uuid |
| `sync_outbox` | id, operation, payload, idempotencyKey, status |
| `audit_logs` | id, userId, entityType, action, oldValueJson, newValueJson |

### Server PostgreSQL

Laravel migrations provide matching tables for: users, projects, transactions, attachments, audit_events, devices, sync_outboxes, accounts, categories, project_assignments plus Spatie RBAC tables.

**Money columns:** Always `BIGINT` (whole rupiah, no decimals).

---

## 9. API Overview (28 Endpoints)

| Category | Endpoints | Auth |
|----------|-----------|------|
| Auth | login, logout, me | Sanctum tokens |
| Transaction | CRUD, submit, approve, reject, void, correction, dispute, resolve | RBAC |
| Sync | push v2, pull changes, status | Token + device |
| Project | CRUD, summary, export | RBAC |
| Period | list, close, reopen | FINANCE_MANAGER/OWNER |
| Attachment | upload (multipart), download (authorized) | Token + project permission |

---

## 10. Security & Compliance

- **Separation of duties:** field engineers cannot approve own transactions
- **Immutable approved records:** financial audit compliance (non-repudiation)
- **Period closing:** month-end controls enforced server-side
- **Device binding:** optional; device revocation blocks sync
- **Audit trail:** immutable server-side event log with old/new value snapshots
- **Password policy:** minimum 8 characters, force change on first login, admin reset
- **Rate limiting:** Laravel built-in `RateLimiter` for failed logins

---

## 11. Sync Strategy

| Direction | Mechanism | Trigger |
|-----------|-----------|---------|
| Push (local → server) | POST /api/v1/sync/push | WorkManager periodic + manual "sync now" |
| Pull (server → local) | GET /api/v1/sync/changes | After push completes |
| Status | GET /api/v1/sync/status | On demand |
| Idempotency | `userUuid:deviceUuid:operationUuid` key | Server dedup |
| Retry | Exponential backoff, max N retries | On failure |
| Attachment | Separate queue, post-transaction-sync | After tx sync |

**Key principle:** Server is authoritative for identity, roles, approvals, periods, audit. Android is authoritative for immediate local capture, draft state, and local files.

---

## 12. Transaction Lifecycle States

```
DRAFT       — Created locally, not yet submitted for approval. Editable.
PENDING     — Submitted for approval. Editable by submitter if NEED_REVISION.
APPROVED    — Approved by finance/supervisor. Immutable in-place.
REJECTED    — Rejected with reason. Not included in FINAL_APPROVED summaries.
NEED_REVISION — Returned for edits. Re-submittable.
VOID        — Soft exclude from FINAL_APPROVED. Visible in LOCAL_VIEW with badge.
CORRECTED   — Original transaction flag; correction row exists linked by group.
```

---

## 13. Out of Scope (V2)

- Full reconciliation engine (bank match, multi-period reconciliation)
- Settlement PIC workflow
- Inter-project / inter-account transfers
- Budget vs actual reporting
- Third-party integrations (bank APIs, accounting software)
- Push notifications
- iOS mobile client
- Real-time collaboration (multi-user edit of same draft)

---

## 14. Success Metrics

| Metric | Target |
|--------|--------|
| Backend API test coverage | ≥ 120 tests, ≥ 350 assertions |
| Android test coverage | Existing unit tests pass |
| Web dashboard pages | 12 pages, role-gated |
| Sync reliability | Idempotent, no duplicates under retry |
| Approval flow completeness | Full lifecycle: DRAFT → APPROVED → VOID/CORRECTION |
| Upgrade path | Existing Android 1.x users upgrade without data loss |
| Offline capability | All capture operations work without network |

---

## 15. Current Progress (June 2026)

| Area | Score | Status |
|------|-------|--------|
| Backend API | 92/100 | 28 endpoints, 124 tests, 381 assertions |
| Web Dashboard | 90/100 | 12 pages, 26 routes, role-based |
| Android | 85/100 | Auth wired, sync fixed, APK ready |
| CI/CD | 85/100 | GitHub Actions backend + Android |
| Production Readiness | 60/100 | Staging seeder, deploy docs; PostgreSQL/Redis provisioning pending |
| **Overall** | **85/100** | Ready for staging deploy |

---

## 16. Related Documents

| Document | Content |
|----------|---------|
| `ROADMAP.md` | Full execution roadmap with phases and guardrails |
| `docs/02_PRODUCT_SCOPE.md` | Detailed product scope per platform |
| `docs/03_ARCHITECTURE.md` | System architecture diagrams |
| `docs/04_ANDROID_LOCAL_FIRST_MULTI_USER.md` | Android multi-user architecture |
| `docs/05_SYNC_ENGINE.md` | Sync engine design |
| `docs/06_BACKEND_API_CONTRACT.md` | API endpoint reference |
| `docs/07_WEB_DASHBOARD_SPEC.md` | Web dashboard specifications |
| `docs/08_FINANCE_RULES.md` | Finance calculation rules |
| `docs/09_RBAC_SECURITY.md` | RBAC and security model |
| `docs/10_DATABASE_SCHEMA.md` | Database schema |
| `docs/11_REPORT_EXPORT.md` | Report and export specifications |
| `docs/12_TEST_PLAN.md` | Test plan and coverage targets |
| `docs/13_DEPLOYMENT_BACKUP_RESTORE.md` | Deployment and operational docs |
| `docs/14_RELEASE_CHECKLIST.md` | Release checklist |
| `.clinerules` | Development conventions and constraints |

---
layout: default
created: 2026-06-30
updated: 2026-07-01T02:00
status: production
version: v2.0.0-b20
commits: 32
tests: "139 passed / 437 assertions"
tables: 31
api_routes: 22
web_routes: 30+
web_pages: 13
android_screens: 12
repo: https://github.com/mldmustamin/Project-Budgeting
server: "103.94.11.78"
demo: https://mldmustamin.github.io/Project-Budgeting/
---

<a href="/Project-Budgeting/" class="back-link">← Back to index</a>

# 🏗️ FundManager V2 — Technical Dashboard

<div class="toc">
<strong>Contents</strong>
<a href="#-stack">🧱 Stack</a>
<a href="#-budget-workflow--7-stages">🔄 Budget Workflow</a>
<a href="#-6-roles--rbac-matrix">👥 RBAC Matrix</a>
<a href="#-live-metrics">📊 Live Metrics</a>
<a href="#-web-pages">🌐 Web Pages</a>
<a href="#-android-screens">📱 Android Screens</a>
<a href="#-recent-patches">🔧 Recent Patches</a>
</div>

<div class="callout summary">
**Budget management platform for field engineering teams.** Single APK (Kotlin/Compose) + Laravel 11 API + PostgreSQL. 6 roles, 3 pagu channels, 7-stage budget workflow. Local-first Android ↔ server-authoritative backend. Production @ VPS <code>103.94.11.78</code>.
</div>

---

## 🧱 Stack

<div class="card">

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Android** | Kotlin 2.0 + Jetpack Compose + Room DB + Hilt + WorkManager | Offline-first field capture, role-gated UI |
| **Backend** | Laravel 11 + PHP 8.2 + PostgreSQL 14 + Redis | Server-authoritative API, RBAC, sync |
| **Web** | Laravel Blade + Tailwind CSS + Alpine.js + Select2 | Advanced finance control center |
| **Auth** | Laravel Sanctum (API) + Session (Web) + Spatie RBAC | Token + session dual auth |
| **Sync** | Outbox pattern + idempotency key: `{serverUserId}:{deviceId}:{operationId}` | Multi-device, multi-user, offline-first |
| **Cash** | `BigInt` (Long) everywhere — never `Double`/`Float` | Finance ledger integrity |

</div>

---

## 🔄 Budget Workflow — 7 Stages

<div class="card">

```
FE Draft (max 5)  →  ESTIMASI  →  SUP Forward  →  FORWARDED
    ↓                                                    ↓
REALISASI  ←  APPROVED  ←  OWNER Approve (± nominal)
    ↓
VERIFIED (ADMIN + FM reconcile)  →  RECONCILED
```

- **35 categories** — 10 FIXED_PAGU, 12 TICKET (wajib bukti), 13 MANAGER_APPROVAL (OWNER decides)
- **3 pagu channels:** FIXED_PAGU (auto), TICKET (verified), MANAGER_APPROVAL (OWNER-set)
- **config/budget.php** — all hardcoded params single source: `BUDGET_MAX_DRAFTS=5`, `BUDGET_PAGINATION=20`, voucher/buruh/ballast/fee amounts

</div>

---

## 👥 6 Roles — RBAC Matrix

<div class="card">

| Role | App | Budget | Approval | Realization | Reports |
|------|-----|--------|----------|-------------|---------|
| <span class="role-badge role-owner">OWNER</span> | Full | Full | **Only approver** | Full | Full |
| <span class="role-badge role-admin">ADMIN</span> | Full | Full | Verify only | Verify | Full |
| <span class="role-badge role-fm">FM</span> | Full | Full | Reconcile only | Reconcile | Full |
| <span class="role-badge role-sup">SUPERVISOR</span> | Team | Submit + Forward | — | View team | Project |
| <span class="role-badge role-fe">FIELD ENG</span> | Assigned | Estimate + Realize | — | Submit | Self |
| <span class="role-badge role-auditor">AUDITOR</span> | Read | Read | — | — | Read |

</div>

<div class="callout important">
OWNER satu-satunya role yang bisa approve budget request + set nominal. ADMIN dan FM hanya mencocokkan data realisasi.
</div>

---

## 📊 Live Metrics

<div class="card">

| Metric | |
|--------|---|
| `commits` | `9dfc810` → `c3207db` (32) |
| `db_tables` | 31 (10 task_expense tables + 12 core + RBAC + sync + audit) |
| `api_endpoints` | 22 under `api/v1/` (CRUD + 7 stage transitions) |
| `web_pages` | 13 (dashboard, transactions, budget flow, locations, equipment, mytasks) |
| `android_screens` | 12 (summary, estimate, realize, inbox, approval, verification, crash, sync) |
| `tests` | 139 passing, 437 assertions |
| `apk` | v2.0.0-b20 — 21 MB |

</div>

---

## 🌐 Web Pages

<div class="card">

| Route | Roles | |
|-------|-------|---|
| `/` | All | Ringkasan keuangan + statistik |
| `/transactions` | All | List + filter + search |
| `/transactions/create` | All | Form input (IDR 40.000 format) |
| `/transactions/{id}/edit` | All | Edit + soft-delete |
| `/budget/inbox` | SUP | Review estimate → forward/reject |
| `/budget/approval` | OWNER | Per-item approve (± nominal) |
| `/budget/verification` | ADMIN, FM | Bill check + reconcile |
| `/budget/create` | FE | Estimate draft (hotel auto-calc) |
| `/budget/{id}/realize` | FE | Realization submission |
| `/locations` | SUP, ADMIN | Master locations CRUD |
| `/equipment` | SUP, ADMIN | Equipment options CRUD |
| `/laporan` | FE | Laporan pekerjaan form |
| `/mytasks` | FE | Task assignments from supervisor |

</div>

---

## 📱 Android Screens

<div class="card">

| Screen | Composable | Roles |
|--------|-----------|-------|
| Login + Password | `LoginScreen` | All |
| Dashboard Summary | `SummaryScreen` | All — synced with `CalculateProjectSummaryUseCase` |
| My Tasks | `MyTasksScreen` | FE |
| Estimate Form | `BudgetEstimateScreen` | FE |
| Realization | `RealizationScreen` | FE |
| Laporan | `LaporanPekerjaanScreen` | FE |
| Supervisor Inbox | `SupervisorInboxScreen` | SUP |
| Assign Task | `AssignTaskScreen` | SUP |
| Approval | `ApprovalScreen` | OWNER |
| Verification | `VerificationScreen` | ADMIN, FM |
| Crash Logs | `CrashLogScreen` | All |
| Sync Monitor | `SyncMonitorScreen` | ADMIN |

</div>

---

## 🔧 Recent Patches

<div class="card">

| # | Issue | Root Cause | Fix | Platform |
|---|-------|-----------|-----|----------|
| 32 | Login freeze | Device registration HTTP blocking UI thread | Background coroutine | Android |
| 33 | APK version drift | Manual versionCode | `scripts/bump-version.sh` auto-increment | Build |
| 34 | Dashboard freeze | Room migration v10 column name mismatch | SQL columns match Entity camelCase | Android |
| 35 | No task view | Missing FE task page | `/mytasks` + 50-user load test | Web |
| 36 | Logout 405 | POST-only `/logout` | Added GET route | Web |
| 36 | IDR raw number | No input formatter | `40,000` on blur | Web |
| 36 | Hotel calc broken | Static tarif | `jumlah_hari × tarif_per_hari` auto | Web |
| 37 | Alpine x-data quotes | Nested quotes in attribute | Move to `<script>` tag | Web |

</div>

<div class="callout danger">
<strong>Hard Constraints</strong><br>
• Money = <code>Long</code>/<code>BigInt</code>. Never <code>Double</code>/<code>Float</code>.<br>
• Approved transactions are <strong>immutable</strong> — correction/void creates new row.<br>
• Sync outbox scoped per user, device, session. Retry = no duplicate.<br>
• Local <code>Long id</code> primary key preserved. <code>uuid</code>, <code>serverId</code>, <code>syncStatus</code> additive only.
</div>

---

## 🔗 Graph

- [Product](blog/product) — PRD, personas, scope
- [Architecture](blog/architecture) — system design, stack, key decisions
- [Backend](blog/backend) — API routes, controllers, migrations
- [Android](blog/android) — Compose screens, Room DB, sync engine
- [Database](blog/database) — schema, 31 tables, FK chain
- [Workflows](blog/workflows) — 7-stage budget flow, sync pattern
- [Sessions](blog/sessions) — session logs, ACTION_LOG
- [Open Q&A](blog/open-qna) — 50 stakeholder questions
- [SOUL](blog/soul) — Hermes identity & operating system
- [GitHub](https://github.com/mldmustamin/Project-Budgeting)
- [Live Site](https://mldmustamin.github.io/Project-Budgeting/)

---

*Last deployed: `c3207db` — `2026-07-01`*

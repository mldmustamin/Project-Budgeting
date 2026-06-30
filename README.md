# FundManager V2

Budget management platform for field engineering teams. Single APK (Kotlin/Compose) + Laravel 11 API + PostgreSQL. 6 roles, 7-stage budget workflow, offline-first Android. Production @ `103.94.11.78`.

**Blog:** `https://mldmustamin.github.io/Project-Budgeting/` | **Repo:** `https://github.com/mldmustamin/Project-Budgeting`

## Status — July 2026

| Component | Detail |
|-----------|--------|
| Backend | 22 API budget routes + existing endpoints, 139 tests / 437 assertions |
| Database | 31 tables (10 budget + 12 core + RBAC + sync + audit) |
| Web | 13 pages (12 original + MyTasks), search + pagination, dark theme |
| Android | 13 screens, APK v2.0.0-b20 (21 MB), multi-user per device |
| Docs | Obsidian vault (11 folders), GitHub Pages blog, 50 Q&A |
| **Overall** | **PRODUCTION** |

## Budget Request Workflow — 7 Stages

```
DRAFT → ESTIMASI → FORWARDED → APPROVED → REALISASI → VERIFIED → RECONCILED
```

- 35 categories: 10 FIXED_PAGU, 12 TICKET, 13 MANAGER_APPROVAL
- 3 pagu channels with server-side enforcement (`config/budget.php`)
- 4-layer nominal tracking: estimated → revised → approved → realized
- Rejection resets to DRAFT. Approved = immutable. Correction/void only.

## Stack

| Layer | Technology |
|-------|-----------|
| Android | Kotlin 2.0 + Jetpack Compose + Room + Hilt + WorkManager + Ktor |
| Backend | Laravel 11 + PHP 8.2 + PostgreSQL 14 + Redis |
| Web | Blade + Tailwind CSS + Alpine.js + Select2 |
| Auth | Sanctum (API) + Session (Web) + Spatie RBAC |
| Blog | Jekyll + GitHub Pages, Obsidian dark theme |

## 6 Roles

| Role | Scope |
|------|-------|
| **OWNER** | Budget approval + set final nominal, full access |
| **ADMIN** | Data reconciliation, master data CRUD |
| **FINANCE_MANAGER** | Verification, reconciliation, period closing |
| **SUPERVISOR** | Forward budget, approve team transactions |
| **FIELD_ENGINEER** | Estimate + realize, offline-capable |
| **AUDITOR** | Read-only across all data |

## Run Local

```bash
# Backend
cd backend
cp .env.example .env
composer install && php artisan key:generate
php artisan migrate --force
php artisan db:seed --class=StagingSeeder
php artisan serve --port=8080

# Web dashboard
npm install && npm run build
# http://localhost:8080 — login: 10001 / admin

# Android
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/
```

## Test

```bash
cd backend
php artisan test --parallel   # 139 tests, 437 assertions
```

## Documentation

| Doc | Location |
|-----|----------|
| PRD | `PRD.md` |
| Open Q&A (50 questions) | `OPEN_QNA.md` |
| Action Log | `ACTION_LOG.md` |
| SOUL (Hermes identity) | `SOUL.md` |
| Obsidian Vault | `docs/obsidian/` (11 folders) |
| Blog | `https://mldmustamin.github.io/Project-Budgeting/` |
| Tech specs | `docs/00-14_*.md` |

## Key Principles

- Money = `Long`/`BigInt`. Never `Double`/`Float`.
- Android offline-first. Room DB preserved.
- Multi-user per device. Sync outbox per user/device/session.
- Approved transactions immutable. Correction/void only.
- Idempotency: `{serverUserId}:{deviceId}:{operationId}`.
- Additive migrations only. Soft-delete for financial records.

## Changelog

**v2 — July 2026** (main)
- 7-stage budget request workflow: 22 API endpoints, 10 new tables, 13 Android screens
- 35 budget categories, 3 pagu channels, pagu_job_type_amounts pivot
- Laporan Pekerjaan: equipment, signal params, 19-point photo checklist
- Multi-user per device with isolated outbox
- APK v2.0.0-b20 with CrashReporter, SyncMonitor, version auto-increment
- GitHub Pages blog with search, pagination, Obsidian dark theme
- Hermes SOUL.md — AI agent operating system

**v1 — June 2026** (codex/fmv2-foundation)
- 28 API endpoints, 124 tests, transaction lifecycle, sync v2
- 12 web pages, universal search, dispute, period management
- Employee ID login, auto-password, GitHub Actions CI/CD

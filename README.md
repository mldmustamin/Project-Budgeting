# Funds Manager V2

Aplikasi Android offline-first + Web Dashboard + Backend API untuk mengelola dana project lapangan. Multi-user, server-authoritative, sync bidirectional. Dibangun dengan prinsip **ponytail** — lazy senior dev, minimal code.

## Status — June 2026

| Component | Score | Detail |
|---|---|---|
| Backend | 92/100 | 28 API endpoints, 124 tests, 381 assertions |
| Web | 90/100 | 12 halaman, 26 routes, role-based |
| Android | 85/100 | Auth wired, sync fixed, APK ready |
| CI/CD | 85/100 | GitHub Actions backend + Android |
| **Overall** | **85/100** | Ready staging deploy |

## Fitur Utama

### Mobile (Android)
- Login dengan ID Karyawan / Email + auto device registration
- Sync bidirectional (push/pull) via WorkManager
- Offline-first: Room DB, outbox enqueue, sync saat online
- Multi-user session dengan DataStore
- Export laporan PDF, Excel, CSV
- Lampiran bukti dari kamera/galeri

### Web Dashboard (Blade + Livewire)
- Dashboard ringkasan keuangan per project
- CRUD Project, Transaksi, User
- Approval Queue — approve, reject, dispute, resolve
- Audit Trail dengan filter multi-kriteria
- Period Management — close/reopen periode akuntansi
- User Management — auto-password, force change, reset
- Universal search (transaksi, project, user)
- Brand color #238b45 (Inter font)

### Backend API (28 endpoints)
- Auth: login/logout/me (employee_id + email + password change)
- Transaction: CRUD, submit, approve, reject, void, correction, dispute, resolve
- Sync: push v2 (CREATE/UPDATE/SOFT_DELETE), pull, status
- Project: CRUD, summary, export
- Period: list, close, reopen
- Attachment: upload (multipart), download (authorized)
- RBAC: 8 roles, Spatie Laravel Permission

## Stack

### Android
- Kotlin + Jetpack Compose Material 3
- Room + Hilt + Navigation Compose + DataStore
- Ktor HTTP Client + WorkManager + Coil

### Backend & Web
- Laravel 11 + Sanctum + Spatie Permission
- Blade + Livewire + Alpine.js + Tailwind CSS
- SQLite (dev) / PostgreSQL (production)
- Redis, Horizon, Telescope

## Menjalankan Lokal

```bash
# Backend
cd backend
cp .env.example .env
composer install
php artisan key:generate
php artisan migrate --force
php artisan db:seed --class=StagingSeeder
php artisan serve --port=8080

# Web dashboard
npm install && npm run build
# Buka http://localhost:8080
# Login: 10001 / admin

# Android
./gradlew-local assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Test

```bash
# Backend (119 tests)
cd backend
php vendor/bin/phpunit

# Android
./gradlew-local testDebugUnitTest
```

## Staging Credentials

| User | ID | Password | Role |
|---|---|---|---|
| Super Admin | 10001 | admin | OWNER |
| Finance | 10002 | password123 | FINANCE_MANAGER |
| Engineer | 10003 | password123 | FIELD_ENGINEER |

## Dokumentasi

Seluruh dokumen di [docs/](docs/):
- `ROADMAP.md` — full execution roadmap
- `docs/00-14_*.md` — spesifikasi teknis per modul

---

## Transaction Lifecycle

```
DRAFT → PENDING → APPROVED ──→ DISPUTED (sanggahan admin)
        ↘ REJECTED              ↓
                                resolve:
                                ├─ accept → CORRECTED + correction
                                └─ reject → kembali APPROVED

APPROVED → VOID (finance, audited)
APPROVED → CORRECTION (finance/supervisor)
```

## Web Dashboard — 12 Halaman

| Halaman | Route | Role |
|---|---|---|
| Login | `/login` | Guest |
| Dashboard | `/` | Semua |
| Projects | `/projects` + `/{uuid}` | Semua (CRUD: OWNER/ADMIN) |
| Transactions | `/transactions` + detail | Semua |
| Approval | `/approval` (Pending + Disputed) | OWNER/ADMIN/FINANCE |
| Audit Trail | `/audit` | OWNER/ADMIN/AUDITOR/FINANCE |
| Periods | `/periods` | OWNER/FINANCE |
| Users | `/users` | OWNER/ADMIN |
| Sync Monitor | `/sync` | OWNER/ADMIN |

## Test Breakdown — 124 Tests

| Class | Tests | Coverage |
|---|---|---|
| TransactionApproval | 8 | Submit, approve, reject |
| Transaction | 15 | CRUD, filters, validation |
| SyncPush | 25 | v2 with idempotency |
| SyncChanges | 8 | Cursor, scoping |
| SyncStatus | 6 | Device health |
| Period | 8 | Close, reopen, enforcement |
| Attachment | 7 | Upload, validation, auth |
| CorrectionVoid | 6 | Void, correction, immutability |
| Dispute | 5 | Dispute, resolve |
| Dashboard/Approval (Web) | 13 | Auth redirect, role access |
| Others | 23 | Auth, device, project, summary |

## Design System

| Element | Value |
|---|---|
| Primary | `#238b45` (brand-600) |
| Palette | brand-50 → brand-900 |
| Font | Inter (400–800) |
| Cards | Rounded-2xl + shadow-sm |
| Icons | SVG inline, zero deps |

## Change Log

**v2 — June 2026** (codex/fmv2-foundation)
- 28 API endpoints, 124 tests, full transaction lifecycle, sync v2
- 12 web halaman, universal search, dispute, period, user CRUD
- Employee ID login, auto-password, force change, admin reset
- GitHub Actions CI/CD, staging seeder, deploy docs
- Brand #238b45, Inter font, ponytail rules

**v1** — Android offline-first MVP

# Funds Manager V2

Aplikasi Android offline-first + Web Dashboard + Backend API untuk mengelola dana project lapangan. Multi-user, server-authoritative, dengan sync engine bidirectional.

## Status Build

- **Backend:** Laravel 11 + Blade + Livewire — 124 tests, 381 assertions
- **Android:** Kotlin + Jetpack Compose + Room + Hilt — build ready, APK tersedia
- **Web:** 12 halaman, 24 routes, role-based access
- **CI/CD:** GitHub Actions — backend PHPUnit + Android Gradle

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

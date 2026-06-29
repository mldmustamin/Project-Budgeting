# FundManager V2 — Backend & Web Dashboard

Backend API + Web Dashboard untuk aplikasi manajemen dana project lapangan.

## Production Build — Status

| Komponen | Status |
|----------|--------|
| **Database** | PostgreSQL 14 (localhost:5432) |
| **Cache/Queue** | Redis (localhost:6379) |
| **Queue Worker** | Laravel Horizon (running) |
| **API Endpoints** | 28 endpoints (cached) |
| **Web Dashboard** | Blade + Livewire (cached) |
| **Tests** | 124 passed, 381 assertions |
| **Server** | PHP 8.2, Laravel 11 |
| **Port** | 8080 |

### Staging Credentials

| User | Login | Password | Role |
|------|-------|----------|------|
| Super Admin | `admin@fundsmanager.test` or `10001` | `admin` | OWNER |
| Finance | `finance@fundsmanager.test` or `10002` | `password123` | FINANCE_MANAGER |
| Engineer | `engineer@fundsmanager.test` or `10003` | `password123` | FIELD_ENGINEER |

### API Endpoints (28)

```
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
POST   /api/v1/devices/register
GET    /api/v1/transactions
POST   /api/v1/transactions
GET    /api/v1/transactions/{uuid}
POST   /api/v1/transactions/{uuid}/submit
POST   /api/v1/transactions/{uuid}/approve
POST   /api/v1/transactions/{uuid}/reject
POST   /api/v1/transactions/{uuid}/void
POST   /api/v1/transactions/{uuid}/correction
POST   /api/v1/transactions/{uuid}/dispute
POST   /api/v1/transactions/{uuid}/resolve-dispute
POST   /api/v1/transactions/{uuid}/attachments
GET    /api/v1/attachments/{uuid}
GET    /api/v1/projects
POST   /api/v1/projects
PATCH  /api/v1/projects/{uuid}
GET    /api/v1/projects/{uuid}/summary
GET    /api/v1/projects/{uuid}/export
POST   /api/v1/projects/{uuid}/assignments
GET    /api/v1/periods
POST   /api/v1/periods/{id}/close
POST   /api/v1/periods/{id}/reopen
POST   /api/v1/sync/push
GET    /api/v1/sync/changes
GET    /api/v1/sync/status
```

### Web Dashboard Routes

```
/login       — Login page
/            — Dashboard
/projects    — Project list & CRUD
/transactions — Transaction list & detail
/approval    — Approval queue
/audit       — Audit trail
/periods     — Period management
/users       — User management
/sync        — Sync monitor
```

## Running Locally

```bash
# Start server
php artisan serve --port=8080

# Start Horizon (queue worker)
php artisan horizon

# Run tests
php artisan test
```

## Stack

- **Backend:** Laravel 11 + Sanctum + Spatie Permission
- **Database:** PostgreSQL (prod) / SQLite (dev/test)
- **Cache/Queue:** Redis
- **Web:** Blade + Livewire + Alpine.js + Tailwind CSS
- **Queue Monitor:** Laravel Horizon
- **Infra:** PHP 8.2, Nginx (reverse proxy)

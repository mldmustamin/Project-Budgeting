# RBAC and Security

## Authorization Model

Three layers combined:

1. **Role permission** — global capabilities (e.g., approve any project)
2. **Project membership** — user assigned to specific projects
3. **Device authorization** — device registered and not revoked

Effective permission = role AND project assignment AND device valid.

---

## Roles

| Role | Description |
|------|-------------|
| OWNER | Manager — full org control, budget approval, final decision |
| ADMIN | User/device/project admin, rekonsiliasi data realisasi |
| FINANCE_MANAGER | Pencocokan data realisasi penggunaan dana ke kordinator, closing, reconciliation |
| SUPERVISOR | Kordinator lapangan — budget request ke Manager, approve/reject transaksi tim |
| FIELD_ENGINEER | Mobile capture transaksi/estimasi, assigned projects only |
| AUDITOR | Read-only audit dan reports |

---

## Pembagian Wewenang Kunci

### Budget Request → Budget Approval
| Tahap | Actor | Wewenang |
|-------|-------|----------|
| Submit budget request | SUPERVISOR (Kordinator) | Estimasi kebutuhan dana per lokasi/project |
| Approve budget + set nominal | **OWNER (Manager)** | Keputusan final nominal budget |
| Lihat budget historis lokasi | OWNER (auto system query) | Data kunjungan terakhir sebagai pertimbangan |

### Realisasi → Rekonsiliasi
| Tahap | Actor | Wewenang |
|-------|-------|----------|
| Input transaksi realisasi | FIELD_ENGINEER, SUPERVISOR | Capture pengeluaran aktual di lapangan |
| Cocokkan data realisasi | FINANCE_MANAGER, ADMIN | Mencocokkan data realisasi penggunaan dana ke kordinator |
| Reconciliation final | FINANCE_MANAGER | Finalisasi pencocokan |

### Transaksi
| Tahap | Actor | Wewenang |
|-------|-------|----------|
| Submit transaksi | FIELD_ENGINEER, SUPERVISOR | Ajukan transaksi untuk approval |
| Approve transaksi | FINANCE_MANAGER, SUPERVISOR | Approval transaksi harian tim |
| Void / correction | FINANCE_MANAGER | Pembatalan / koreksi transaksi approved |

---

## Permission Matrix (Sensitive Actions)

| Action | Required Permission |
|--------|---------------------|
| Create transaction (mobile) | FIELD_ENGINEER or SUPERVISOR + project assignment |
| Submit budget request | SUPERVISOR + project assignment |
| **Approve budget request** | **OWNER only** |
| Approve transaction | FINANCE_MANAGER or SUPERVISOR + project |
| Reject / need revision | Same as approve |
| Void approved tx | FINANCE_MANAGER |
| Correction | FINANCE_MANAGER or SUPERVISOR |
| Close period | FINANCE_MANAGER or OWNER |
| Reconciliation finalize | FINANCE_MANAGER or ADMIN |
| Rekonsiliasi data realisasi | FINANCE_MANAGER, ADMIN |
| User CRUD | ADMIN or OWNER |
| Device revoke | ADMIN or OWNER |
| Project assignment | ADMIN |
| Create project | OWNER, ADMIN, FINANCE_MANAGER, SUPERVISOR |
| View audit trail | AUDITOR, FINANCE_MANAGER, ADMIN, OWNER |

---

## Field Engineer Constraints

- Can only read/write projects in assignment cache
- Cannot approve own transactions (separation of duties — server enforced)
- Cannot create budget request
- Cannot access web admin modules
- Sync push validated against assignments server-side

---

## Auditor Constraints

- Read-only by default
- No transaction mutations
- Can export audit and reconciliation reports
- Explicit grant required for any write

---

## Attachment Security

- Not public by default
- Download requires auth + project permission
- Presigned URLs expire ≤ 15 minutes
- Mobile stores files in app-private storage
- Upload over HTTPS only

---

## Authentication (Laravel Sanctum)

- **Web auth:** Laravel session-based (Blade + Livewire)
- **API auth:** Sanctum token-based (mobile clients)
- Password policy: min 8 chars (Laravel validation)
- Access token TTL: configurable via `ExpiresAt` (default 60 min)
- Refresh token rotation (Sanctum)
- Device binding optional for high-security deployments
- Failed login rate limiting (Laravel built-in `RateLimiter`)
- **RBAC implementation:** Spatie Laravel Permission (published, migrated, seeded with 6 FMv2 roles). Role checks via `$user->hasRole('OWNER')` and `$user->can('approve')`. Gates/Policies for fine-grained action permissions.

---

## Data Protection

- TLS everywhere
- Android: EncryptedSharedPreferences for tokens (Phase 2)
- Server: bcrypt/argon2 password hashes
- PII minimization in logs
- No financial amounts in crash logs

---

## Android Multi-User Security

- Session switch must not expose previous user's cached UI state
- Outbox scoped — prevent cross-user sync bleed
- Logout clears tokens from memory, not local DB

---

## Compliance Notes

- Soft delete + audit trail supports financial audit requirements
- Immutable approved records support non-repudiation
- Period closing supports month-end controls

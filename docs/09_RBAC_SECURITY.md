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
| OWNER | Full org control |
| ADMIN | User/device/project admin, no destructive org actions |
| FINANCE_MANAGER | Approval, closing, reconciliation, settlement |
| SUPERVISOR | Approve/reject for assigned projects |
| PIC | Person in charge; settlement participant |
| FIELD_ENGINEER | Mobile capture, assigned projects only |
| AUDITOR | Read-only audit and reports |
| VIEWER | Read-only dashboard |

---

## Permission Matrix (Sensitive Actions)

| Action | Required Permission |
|--------|---------------------|
| Create transaction (mobile) | FIELD_ENGINEER + project assignment |
| Approve transaction | FINANCE_MANAGER or SUPERVISOR + project |
| Reject / need revision | Same as approve |
| Void approved tx | FINANCE_MANAGER |
| Correction | FINANCE_MANAGER or SUPERVISOR |
| Close period | FINANCE_MANAGER or OWNER |
| Reconciliation finalize | FINANCE_MANAGER |
| User CRUD | ADMIN or OWNER |
| Device revoke | ADMIN or OWNER |
| Project assignment | ADMIN |
| View audit trail | AUDITOR, FINANCE_MANAGER, ADMIN, OWNER |

---

## Sensitive Action Requirements

All sensitive web actions must include:
1. Permission check (server-side)
2. Reason (text, min length)
3. Confirmation step (UI modal)
4. Audit log entry (server immutable)

Android local actions log to `audit_logs` table; synced audit merges to server trail.

---

## Field Engineer Constraints

- Can only read/write projects in assignment cache
- Cannot approve own transactions (separation of duties — server enforced)
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
- **RBAC implementation:** Spatie Laravel Permission (published, migrated, seeded with 8 FMv2 roles). Role checks via `$user->hasRole('OWNER')` and `$user->can('approve')`. Gates/Policies for fine-grained action permissions.

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

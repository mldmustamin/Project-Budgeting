# Database Schema

## Android Room (Current v7)

Database name: `funds_manager_db`  
Schema exports: `app/schemas/com.example.fundsmanager.data.local.AppDatabase/`

### Entity Relationship (Current)

```
users (1) ──< projects
users (1) ──< transactions
projects (1) ──< transactions
accounts (1) ──< transactions (nullable FK)
categories (1) ──< transactions (nullable FK)
transactions (1) ──< attachments
users (1) ──< audit_logs
```

### Table Summaries

#### users
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK AI | Local Long |
| name | TEXT | |
| email | TEXT? | |
| createdAt, updatedAt | INTEGER | |
| deletedAt | INTEGER? | soft delete |

#### projects
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK AI | |
| userId | INTEGER FK → users | CASCADE |
| name | TEXT | |
| description | TEXT? | |
| isArchived | INTEGER | boolean |
| startAt | INTEGER | epoch ms |
| completedAt | INTEGER? | |
| createdAt, updatedAt, deletedAt | | |

#### transactions
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK AI | |
| userId | INTEGER FK | |
| projectId | INTEGER FK | |
| accountId | INTEGER FK? | SET NULL |
| categoryId | INTEGER FK? | SET NULL |
| type | TEXT | enum |
| date | TEXT | yyyy-MM-dd |
| description | TEXT | |
| reportedAmount | INTEGER | Long money |
| realAmount | INTEGER | Long money |
| sourceText | TEXT? | preserved |
| note | TEXT? | |
| legacyHash | TEXT? | unique index |
| createdAt, updatedAt, deletedAt | | |

#### attachments
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK AI | |
| transactionId | INTEGER FK | CASCADE |
| filePath | TEXT | internal storage |
| fileName, mimeType | TEXT? | |
| createdAt | INTEGER | |
| deletedAt | INTEGER? | |

#### accounts / categories
Shared lookup tables (no userId FK currently).

#### audit_logs
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK AI | |
| userId | INTEGER | |
| entityType | TEXT | project, transaction |
| entityId | INTEGER | local id |
| action | TEXT | create, update, soft_delete, ... |
| oldValueJson, newValueJson | TEXT? | |
| createdAt | INTEGER | |

---

## Additive Columns Added (v7 — Phase 1)

Applied in `MIGRATION_6_7` to `transactions`, `projects`, `attachments`, `users`:

```
uuid TEXT NOT NULL
serverId TEXT
deviceId TEXT
syncStatus TEXT DEFAULT 'PENDING'
approvalStatus TEXT DEFAULT 'DRAFT'
financeStatus TEXT DEFAULT 'ACTIVE'
lastSyncedAt INTEGER
sessionId TEXT
serverUserId TEXT
userUuid TEXT
projectUuid TEXT  -- on transactions
```

Indexes: unique on `uuid`; index on `syncStatus`, `serverId`.

---

## Planned New Tables (Phase 2–3)

### sync_outbox
See `docs/05_SYNC_ENGINE.md`

### attachment_upload_queue
| Column | Notes |
|--------|-------|
| attachmentUuid | |
| transactionUuid | |
| localFilePath | |
| status | PENDING, UPLOADING, DONE, FAILED |

### session_state
Active session metadata (may use DataStore instead for some fields).

### project_assignment_cache
| Column | Notes |
|--------|-------|
| projectUuid | |
| serverUserId | |
| roleOnProject | PIC, MEMBER |
| cachedAt | |

### permission_snapshot
JSON blob of effective permissions at login.

---

## Server PostgreSQL (Scaffolded in `backend/`)

Laravel migration files exist for:
- `users` — with uuid, sync_status, soft deletes
- `projects` — with user FK, uuid, sync fields
- `transactions` — with BIGINT money, approval/finance/sync statuses, all FKs
- `attachments` — with transaction FK, sync fields
- `audit_events` — JSON old/new values, device/session tagging
- `devices` — with user FK, revocation support
- `sync_outboxes` — idempotency key, per-user/device/session scoping
- `accounts`, `categories` — with uuid, optional user_id FK
- **RBAC:** Spatie Laravel Permission tables (roles, permissions, model_has_roles, model_has_permissions, role_has_permissions) — migrated and seeded with 8 FMv2 roles
- **`project_assignments`** — with uuid, project FK, user FK, role_on_project, active_from, active_until — migrated. Scoped project list by assignment for non-OWNER/ADMIN roles.

**Money columns:** `BIGINT` via Laravel `$table->bigInteger()`.

**Deferred (schema not yet defined):**
- `transaction_corrections` — linking strategy TBD
- `accounting_periods`, `reconciliations`, `settlements` — Phase 7–8

---

## Migration History (Android)

| Version | Change |
|---------|--------|
| 1 | Initial schema |
| 2 | audit_logs table |
| 3 | Seed Local User id=1 |
| 4 | Reserved (no-op) |
| 5 | Reserved (no-op) |
| 6 | projects.startAt, projects.completedAt |
| 7 | Additive sync/identity columns + uuid backfill + indexes |

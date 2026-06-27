# Sync Engine Specification

## Overview

Bidirectional sync connects Android local-first storage to server authority. Sync is identity-aware, idempotent, and scoped per user/device/session.

---

## Outbox Design (Android — Phase 3)

### Table: `sync_outbox`

| Column | Type | Notes |
|--------|------|-------|
| id | Long PK | local |
| uuid | String | operation uuid |
| localUserId | Long | |
| serverUserId | String | |
| userUuid | String | |
| deviceId | String | |
| sessionId | String | |
| entityType | String | transaction, project, attachment_meta |
| entityUuid | String | target row uuid |
| operation | String | CREATE, UPDATE, SOFT_DELETE |
| payloadJson | String | serialized delta |
| idempotencyKey | String | `{serverUserId}:{deviceId}:{operationId}` |
| status | String | PENDING, IN_FLIGHT, SYNCED, REJECTED, CONFLICT |
| retryCount | Int | |
| lastError | String? | server rejection reason |
| createdAt | Long | |
| updatedAt | Long | |

### Enqueue Triggers
- After successful local save (transaction, project update)
- After soft delete
- Not on read operations

### Processing Rules
1. Worker runs on connectivity + manual "Sync now"
2. Process only rows matching active session scope
3. Max retry with exponential backoff
4. IN_FLIGHT timeout → revert to PENDING

---

## Idempotency

**Key format:** `{serverUserId}:{deviceId}:{operationId}`

Server stores processed keys with TTL ≥ 30 days. Duplicate POST with same key returns original response without duplicate insert.

---

## Pull Sync

**Endpoint (planned):** `GET /sync/changes?since={cursor}&projectUuids=...`

**Merge rule:**
```
FOR each server_change:
  FIND local row BY uuid
  IF not found → INSERT with new local Long id
  IF found → UPDATE fields except local id
  NEVER match by local Long id alone
```

**Conflict policy (initial):** Server wins on approvalStatus/financeStatus; client wins on draft fields until submitted.

---

## Attachment Upload Queue

Separate from entity outbox:

| Step | Action |
|------|--------|
| 1 | Transaction synced → has serverId |
| 2 | Queue attachment with presigned URL request |
| 3 | Upload binary to object storage |
| 4 | Confirm upload to API |
| 5 | Update local attachment syncStatus |

Failed uploads retain local file; retry independently.

---

## Server Validation (Phase 5)

Before accepting push:
- Valid JWT + device registered
- User assigned to project
- Permission allows operation
- Period not closed for transaction date
- Amount fields are integer
- Approved transaction rejects direct UPDATE (require correction endpoint)

---

## Rejection Handling

Rejected operations remain visible locally:
- `syncStatus = REJECTED`
- `lastError` populated with server message
- UI badge on transaction
- User can edit draft and re-submit (new outbox entry + new operationId)

---

## Sync Monitor (Web — Phase 6)

Display:
- Devices last seen
- Pending outbox count per device/user
- Rejected/conflict queue
- Manual retry trigger (admin)

---

## Before Sync Push MVP Checklist

| Item | Status |
|------|--------|
| Device registration endpoint | ✅ `POST /api/v1/devices/register` implemented |
| Project assignment API | ✅ `POST /api/v1/projects/{project}/assignments` implemented |
| Transaction create API | ✅ `POST /api/v1/transactions` implemented with BIGINT validation |
| Server-side `sync_outboxes` table | ✅ Migrated with idempotency key, user/device/session columns |
| Server-side `projects`, `users`, `devices` tables | ✅ All migrated with UUID columns |
| Sanctum auth (login/logout/me) | ✅ 4 tests passing |
| RBAC (Spatie roles seeded) | ✅ 8 roles: OWNER, ADMIN, FINANCE_MANAGER, SUPERVISOR, PIC, FIELD_ENGINEER, AUDITOR, VIEWER |
| Idempotency handling on server | ✅ `POST /api/v1/sync/push` implemented with idempotency via `legacy_hash`, device validation, project assignment enforcement |
| Android outbox enqueue | ⏳ Phase 3 — Room entity + worker not yet implemented |
| Pull sync endpoint (`GET /sync/changes`) | ⏳ Phase 5 — not yet implemented |
| Attachment upload queue (separate) | ⏳ Phase 5 — not yet implemented |
| Sync monitor UI | ⏳ Phase 6 — web dashboard |

## Phase Dependencies

| Phase | Deliverable |
|-------|-------------|
| 1 | uuid columns on entities |
| 2 | session scope for outbox filtering |
| 3 | outbox table + Android worker |
| 4 | server DB + auth |
| 5 | push/pull API |
| 6 | sync monitor UI |

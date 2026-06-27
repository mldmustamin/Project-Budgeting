# Backend API Contract

**Status:** 17 API endpoints + 8 web routes implemented. Full backend suite: 80 tests, 257 assertions + web tests.
**Backend framework:** Laravel 11 with Sanctum auth + Blade + Livewire web dashboard.

## Implemented API Surface

| Group | Endpoints | Tests |
|-------|-----------|-------|
| Auth | `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `POST /api/v1/auth/logout` | 4 tests (21 assertions) |
| Devices | `POST /api/v1/devices/register` | 3 tests (10 assertions) |
| Projects | `GET /api/v1/projects`, `POST /api/v1/projects`, `PATCH /api/v1/projects/{uuid}`, `POST /api/v1/projects/{project}/assignments` | 5 tests (14 assertions) |
| Transactions | `GET /api/v1/transactions`, `POST /api/v1/transactions`, `GET /api/v1/transactions/{uuid}`, `POST /api/v1/transactions/{uuid}/submit`, `POST /api/v1/transactions/{uuid}/approve`, `POST /api/v1/transactions/{uuid}/reject` | 23 tests (73 assertions) |
| Sync | `POST /api/v1/sync/push`, `GET /api/v1/sync/changes`, `GET /api/v1/sync/status` | 39 tests (105 assertions) |

## Implemented Web Dashboard (Phase 6)

| Page | Route | Auth |
|------|-------|------|
| Login | `GET /login`, `POST /login` | Guest |
| Logout | `POST /logout` | Session |
| Dashboard | `GET /` or `/dashboard` | Session |
| Projects | `GET /projects` | Session |
| Transactions | `GET /transactions`, `GET /transactions/{uuid}` | Session |
| Approval Queue | `GET /approval` | Session + role:OWNER/ADMIN/FINANCE_MANAGER |
| Approve | `POST /transactions/{uuid}/approve` | Session + role |
| Reject | `POST /transactions/{uuid}/reject` | Session + role |
| Sync Monitor | `GET /sync` | Session + role:OWNER/ADMIN |

## Implemented Period Closing (Phase 7)

| Endpoint | Description | Access |
|----------|-------------|--------|
| `GET /api/v1/periods` | List all accounting periods | Sanctum |
| `POST /api/v1/periods/{period}/close` | Close a period (audited) | Sanctum + role:OWNER/FINANCE_MANAGER |
| `POST /api/v1/periods/{period}/reopen` | Reopen a period (audited) | Sanctum + role:OWNER/FINANCE_MANAGER |

**Enforcement:** Transaction create via API and sync push CREATE/UPDATE/SOFT_DELETE are rejected if date falls within a closed period.

## Implemented Attachment Sync (Phase 8)

| Endpoint | Description | Access |
|----------|-------------|--------|
| `POST /api/v1/transactions/{transaction}/attachments` | Upload attachment (multipart file) | Sanctum + project assignment |
| `GET /api/v1/attachments/{attachment}` | Download attachment file | Sanctum + project assignment or admin |

**Validation:** Max 10MB, mime types: image/jpeg, image/png, image/webp, application/pdf.
**Storage:** Local disk (`storage/app/private/attachments/`), configurable to S3.
**Authorization:** OWNER/ADMIN/AUDITOR can access any; others require project assignment.

## Implemented Correction & Void (Phase 9)

| Endpoint | Description | Access |
|----------|-------------|--------|
| `POST /api/v1/transactions/{transaction}/void` | Void approved transaction (reason required, audited) | OWNER/FINANCE_MANAGER |
| `POST /api/v1/transactions/{transaction}/correction` | Create correction (new linked tx, original marked CORRECTED) | OWNER/FINANCE_MANAGER/SUPERVISOR |

**Immutability:** Approved transactions reject direct UPDATE via sync push.
**Finance Status:** VOIDED — excluded from FINAL_APPROVED totals; CORRECTED — original marked, correction is new ACTIVE row.

**Deferred:** refresh token rotation

---

## Conventions

- Base URL: `https://api.fundmanager.example/v1`
- Auth: `Authorization: Bearer {accessToken}`
- Device header: `X-Device-Id: {deviceUuid}`
- Idempotency: `Idempotency-Key: {serverUserId}:{deviceId}:{operationId}`
- Money: integer (smallest currency unit / whole rupiah)
- Timestamps: ISO 8601 or epoch ms (document per endpoint)
- Entity identity: `uuid` (string, UUID v4)

---

## Auth

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| POST | `/api/v1/auth/login` | email/password → Sanctum token + user profile (with roles) | ✅ Implemented |
| GET | `/api/v1/auth/me` | current user + roles (bearer token) | ✅ Implemented |
| POST | `/api/v1/auth/logout` | revoke all user tokens | ✅ Implemented |
| POST | `/auth/refresh` | refresh token rotation | ⏳ Deferred |
| POST | `/api/v1/devices/register` | register device, return uuid + user_uuid + revoked status | ✅ Implemented |

---

## Sync

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| POST | `/api/v1/sync/push` | batch outbox CREATE, UPDATE, and SOFT_DELETE operations with idempotency, device validation, project assignment enforcement | ✅ Implemented |
| GET | `/api/v1/sync/changes` | pull delta since cursor with assignment scoping | ✅ Implemented |
| GET | `/api/v1/sync/status` | device sync health with outbox counts | ✅ Implemented |

### Push Request (example)
```json
{
  "operations": [{
    "idempotencyKey": "usr_1:dev_abc:op_123",
    "entityType": "transaction",
    "entityUuid": "550e8400-e29b-41d4-a716-446655440000",
    "operation": "CREATE",
    "payload": {
      "projectUuid": "...",
      "type": "OFFICE_EXPENSE",
      "date": "2026-06-27",
      "reportedAmount": 500000,
      "realAmount": 450000,
      "description": "Transport",
      "approvalStatus": "PENDING"
    }
  }]
}
```

### Push Response (example)
```json
{
  "results": [{
    "idempotencyKey": "usr_1:dev_abc:op_123",
    "status": "ACCEPTED",
    "serverId": "tx_789",
    "approvalStatus": "PENDING"
  }]
}
```

---

## Transactions

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| GET | `/api/v1/transactions` | list with filters (`project_uuid`, `type`, `approval_status`, `finance_status`) | ✅ Implemented |
| GET | `/api/v1/transactions/{uuid}` | detail | ✅ Implemented |
| POST | `/api/v1/transactions` | create (BIGINT amounts, resolves `project_uuid`) | ✅ Implemented |
| POST | `/api/v1/transactions/{uuid}/approve` | approve | ⏳ Deferred |
| POST | `/transactions/{uuid}/reject` | reject with reason |
| POST | `/transactions/{uuid}/request-revision` | need revision |
| POST | `/transactions/{uuid}/void` | void (soft) |
| POST | `/transactions/{uuid}/correction` | create correction tx |

---

## Projects

| Method | Path | Description |
|--------|------|-------------|
| GET | `/projects` | assigned projects |
| POST | `/projects` | create (admin) |
| GET | `/api/v1/projects` | assigned projects (OWNER/ADMIN see all; others see assigned only) | ✅ Implemented |
| POST | `/api/v1/projects` | create (admin) | ✅ Implemented |
| PATCH | `/api/v1/projects/{uuid}` | update | ✅ Implemented |
| POST | `/api/v1/projects/{project}/assignments` | assign users (project resolved by UUID) | ✅ Implemented |

---

## Attachments

| Method | Path | Description |
|--------|------|-------------|
| POST | `/attachments/presign` | get upload URL |
| POST | `/attachments/{uuid}/confirm` | confirm upload |
| GET | `/attachments/{uuid}` | download (authorized) |

---

## Finance Operations (Web-only)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/periods/{uuid}/close` | close accounting period |
| GET | `/reconciliation` | reconciliation workspace |
| POST | `/settlements` | PIC settlement |
| POST | `/transfers` | inter-project transfer |

---

## Admin

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/users` | user management |
| GET/POST | `/roles` | role assignment |
| GET | `/audit` | audit trail query |

---

## Error Format

```json
{
  "error": {
    "code": "PERIOD_CLOSED",
    "message": "Cannot post transaction to closed period 2026-05",
    "details": {}
  }
}
```

Android must persist rejection reason on local row.

---

## Versioning

- API version in path `/v1`
- Breaking changes → `/v2` with parallel support period
- Mobile app sends `X-App-Version` header

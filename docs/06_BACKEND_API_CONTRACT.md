# Backend API Contract (Draft)

**Status:** 11 endpoints implemented, 17 test cases passing (63 assertions).
**Backend framework:** Laravel 11 with Sanctum auth.

## Implemented API Surface

| Group | Endpoints | Tests |
|-------|-----------|-------|
| Auth | `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `POST /api/v1/auth/logout` | 4 tests (21 assertions) |
| Devices | `POST /api/v1/devices/register` | 3 tests (10 assertions) |
| Projects | `GET /api/v1/projects`, `POST /api/v1/projects`, `PATCH /api/v1/projects/{uuid}`, `POST /api/v1/projects/{project}/assignments` | 5 tests (14 assertions) |
| Transactions | `GET /api/v1/transactions`, `POST /api/v1/transactions`, `GET /api/v1/transactions/{uuid}` | 5 tests (18 assertions) |

**Deferred:** refresh token rotation, transaction approval/rejection, correction/void, push/pull sync, attachments, web dashboard

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
| POST | `/api/v1/sync/push` | batch outbox CREATE operations with idempotency, device validation, project assignment enforcement | ✅ Implemented |
| GET | `/sync/changes` | pull delta since cursor | ⏳ Deferred |
| GET | `/sync/status` | device sync health | ⏳ Deferred |

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

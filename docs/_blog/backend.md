---
layout: default
created: 2026-06-30
updated: 2026-07-01
status: production
tags: [backend, api, laravel, controllers]
---

# Backend — Laravel 11 API

## Overview

The backend is a Laravel 11 REST API serving both the Android app (Bearer token) and web dashboard (session). All routes under `api/v1/` with `auth:sanctum` middleware. 22 endpoints across 4 resource groups, each with role-gated authorization.

## API Routes

### Budget Templates (Read-Only)

`GET /api/v1/budget-templates`

Returns 35 budget categories with metadata: category name, pagu type (FIXED_PAGU / TICKET / MANAGER_APPROVAL), unit, job type applicability. Cached on Android for offline form population.

### Equipment Options (Read-Only)

`GET /api/v1/equipment-options?field_key=JENIS_ANTENNA`

Dynamic dropdown values for forms: antenna types, modem types, gangguan options, SCM photo checklist items. Param `field_key` filters by category. Used by both Android and Web forms to keep dropdowns consistent.

### Master Locations

| Method | Route | Roles | Purpose |
|--------|-------|-------|---------|
| GET | `/projects/{project}/locations` | Assigned users | List locations for a project |
| POST | `/projects/{project}/locations` | ADMIN, SUPERVISOR | Create location |
| GET | `/locations/{location}` | Assigned users | Detail view |
| PUT | `/locations/{location}` | ADMIN, SUPERVISOR | Update location |
| DELETE | `/locations/{location}` | ADMIN | Soft-delete location |
| GET | `/locations/{location}/history` | OWNER | Budget spending history at this location |

Location history is critical for the OWNER approval flow — before approving a budget, the owner sees "how much was spent here before?". This prevents duplicate work claims at the same site.

### Task Expenses — Core 7-Stage Workflow

| Method | Route | Actor | Stage Transition |
|--------|-------|-------|-----------------|
| GET | `/task-expenses` | All (role-scoped) | — List own/team/all |
| POST | `/task-expenses` | FE | Create DRAFT (max 5) |
| GET | `/task-expenses/{uuid}` | Assigned | Detail view |
| PUT | `/task-expenses/{uuid}` | FE | Edit DRAFT only |
| DELETE | `/task-expenses/{uuid}` | FE | Delete DRAFT only |
| POST | `/task-expenses/{uuid}/submit` | FE | DRAFT → ESTIMASI |
| POST | `/task-expenses/{uuid}/forward` | SUP | ESTIMASI → FORWARDED |
| POST | `/task-expenses/{uuid}/approve` | OWNER | FORWARDED → APPROVED |
| POST | `/task-expenses/{uuid}/reject` | SUP or OWNER | → DRAFT (cascade reset) |
| POST | `/task-expenses/{uuid}/realize` | FE | APPROVED → REALISASI |
| POST | `/task-expenses/{uuid}/verify` | ADMIN or FM | REALISASI → VERIFIED |
| POST | `/task-expenses/{uuid}/reconcile` | FM | VERIFIED → RECONCILED |
| GET | `/task-expenses/{uuid}/histories` | Assigned | Audit trail |

Each stage transition records: `from_stage`, `to_stage`, `actor_id`, `notes`, `changes` (JSON diff of what changed). Optimistic locking (`lockVersion` field) prevents race conditions on concurrent transitions.

### Sync Endpoints

| Method | Route | Purpose |
|--------|-------|---------|
| POST | `/sync/push` | Upload outbox entries from device |
| GET | `/sync/pull?since=&userId=` | Download changes since last sync |

Both use idempotency key `{serverUserId}:{deviceId}:{operationId}`. Server deduplicates on receipt. Rejected operations return with reason, visible in Android Sync Monitor.

## Controllers

| Controller | Lines | Responsibility |
|-----------|-------|---------------|
| `TaskExpenseController` | 700+ | CRUD + 7 stage transitions + pagu validation |
| `MasterLocationController` | 200+ | CRUD + location budget history |
| `BudgetItemTemplateController` | 50 | Read-only GET, cached response |
| `MasterEquipmentOptionController` | 50 | Read-only GET with field_key filter |
| `SyncController` | 300+ | Push/pull with idempotency + validation |
| `BudgetWebController` | 600+ | Web Blade views: inbox, approval, verification, forms |

## Authorization Matrix

Every write endpoint enforces role checks. No endpoint trusts the client to self-identify: roles are read from server-side tokens/sessions.

| Action | Allowed Roles | Additional Checks |
|--------|--------------|-------------------|
| Create draft | FIELD_ENGINEER | Max 5 drafts, assigned to project |
| Submit | FIELD_ENGINEER | Own task, stage = DRAFT |
| Forward | SUPERVISOR | Project assignment, stage = ESTIMASI |
| Approve | OWNER | Stage = FORWARDED, location history check |
| Reject | SUPERVISOR or OWNER | Stage in [ESTIMASI, FORWARDED] |
| Realize | FIELD_ENGINEER | Own task, stage = APPROVED |
| Verify | ADMIN or FINANCE_MANAGER | Stage = REALISASI, bukti kwitansi check |
| Reconcile | FINANCE_MANAGER | Stage = VERIFIED, crosscheck supervisor |

## Pagu Enforcement (Server-Side)

Pagu values defined in `config/budget.php` and `pagu_job_type_amounts` pivot table:

| Category | Type | Values |
|----------|------|--------|
| VOUCHER | FIXED_PAGU | 15rb/hari (INSTALASI), 5rb (others) |
| BURUH | FIXED_PAGU | 120rb/titik (INSTALASI), null (others) |
| BALLAST | FIXED_PAGU | 200rb/titik (INSTALASI), null (others) |
| FEE | FIXED_PAGU | 40rb/75rb/15rb/null per job_type |

Enforcement occurs server-side during `submit`:
1. Field is FIXED_PAGU → system sets `estimated_amount`, field engineer CANNOT override
2. Field is TICKET → wajib bukti flag true, receipt upload required at verification
3. Field is MANAGER_APPROVAL → passes through, OWNER sets final amount at approval

## Validation Rules

- `estimated_amount` ≥ 0 for FIXED_PAGU (system-enforced)
- `revised_amount` ≥ 0 (supervisor can adjust)
- `approved_amount` ≥ 0 (owner final)
- `realization_amount` ≥ 0 (FE actual spend)
- `bill_verified` boolean (ADMIN/FM verification checkbox)
- Accounting period must be open for all writes
- Project must exist and user must be assigned

## Test Coverage

139 tests, 437 assertions covering:
- CRUD operations (create, read, update, delete)
- All 7 stage transitions with happy paths
- Rejection flow (2 stages → DRAFT)
- Role authorization (6 roles, 22 endpoints)
- Pagu enforcement (FIXED, TICKET, MANAGER)
- Sync push/pull with idempotency
- Validation edge cases (max 5 drafts, closed period, wrong stage)

Run: `php artisan test --parallel`

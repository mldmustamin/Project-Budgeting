---
created: 2026-06-30
status: complete
tags: [backend, api, routes]
---

# API Routes — Backend

## New Routes (Phase 1-5)
All routes under `api/v1/` with `auth:sanctum` middleware.

### Budget Templates
- `GET /api/v1/budget-templates` → 35 categories with meta (FIXED/TICKET/MANAGER counts)

### Equipment Options
- `GET /api/v1/equipment-options?field_key=JENIS_ANTENNA` → Dropdown data

### Master Locations
- `GET /api/v1/projects/{project}/locations` — List locations
- `POST /api/v1/projects/{project}/locations` — Create (ADMIN, SUPERVISOR)
- `GET/PUT/DELETE /api/v1/locations/{location}` — CRUD
- `GET /api/v1/locations/{location}/history` — Budget history

### Task Expenses (Core Workflow)
- `GET /api/v1/task-expenses` — List (role-scoped)
- `POST /api/v1/task-expenses` — Create draft (FE only, max 5)
- `GET/PUT/DELETE /api/v1/task-expenses/{uuid}` — CRUD draft
- `POST /api/v1/task-expenses/{uuid}/submit` — FE → ESTIMASI
- `POST /api/v1/task-expenses/{uuid}/forward` — SUP → FORWARDED
- `POST /api/v1/task-expenses/{uuid}/approve` — OWNER → APPROVED
- `POST /api/v1/task-expenses/{uuid}/reject` → DRAFT (cascade)
- `POST /api/v1/task-expenses/{uuid}/realize` — FE → REALISASI
- `POST /api/v1/task-expenses/{uuid}/verify` — ADMIN/FM → VERIFIED
- `POST /api/v1/task-expenses/{uuid}/reconcile` — FM → RECONCILED
- `GET /api/v1/task-expenses/{uuid}/histories` — Audit trail

## Controllers
- [[TaskExpenseController]] — 700+ lines, 7 stage transitions
- [[MasterLocationController]] — CRUD + history
- [[BudgetItemTemplateController]] — Read-only GET
- [[MasterEquipmentOptionController]] — Read-only GET

## Authorization
All write endpoints enforce role checks:
- `store`: FIELD_ENGINEER
- `forward`: SUPERVISOR
- `approve`: OWNER
- `reject`: SUPERVISOR or OWNER
- `realize`: FIELD_ENGINEER (own task)
- `verify`: ADMIN or FINANCE_MANAGER
- `reconcile`: FINANCE_MANAGER

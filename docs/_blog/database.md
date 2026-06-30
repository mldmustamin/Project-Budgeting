---
created: 2026-06-30
updated: 2026-07-01
status: production
tags: [database, schema, postgresql, migrations]
---

# Database Schema — PostgreSQL 14

## Overview

31 tables total: 22 existing core tables + 9 new budget workflow tables + 1 pivot table. All FK relationships enforced at database level. Soft-delete pattern on financial records (never hard-delete). UUID primary keys for syncable entities, local Long id for Android Room.

## New Tables (Budget Workflow Phase)

### Core Workflow — 7 Tables

**`task_expenses`** — The central entity. One form = one row. Tracks the 7-stage lifecycle.

| Column | Type | Purpose |
|--------|------|---------|
| `uuid` | UUID | Server identifier, used for sync |
| `project_id` | FK → projects | Which project |
| `location_id` | FK → master_locations | Work site |
| `job_type` | enum | INSTALASI, RELOKASI, PMCM, DISMANTLE, SURVEY |
| `stage` | enum | DRAFT → ESTIMASI → FORWARDED → APPROVED → REALISASI → VERIFIED → RECONCILED |
| `submitted_by` | FK → users | FE who created |
| `forwarded_by` | FK → users | SUP who forwarded |
| `approved_by` | FK → users | OWNER who approved |
| `verified_by` | FK → users | ADMIN/FM who verified |
| `reconciled_by` | FK → users | FM who reconciled |
| `notes` | text | Free-text notes |
| `rejection_reason` | text | Why rejected (if applicable) |
| `lock_version` | integer | Optimistic locking |

**`expense_items`** — Per-item budget lines inside a task_expense. Four-layer nominal tracking:

| Column | Type | Purpose |
|--------|------|---------|
| `task_expense_id` | FK | Parent form |
| `budget_item_template_id` | FK | Category reference (35 options) |
| `estimated_amount` | bigint | FE estimate (system-set for FIXED_PAGU) |
| `revised_amount` | bigint | SUP revision (can adjust) |
| `approved_amount` | bigint | OWNER final (the actual approved budget) |
| `realization_amount` | bigint | FE actual spending |
| `bill_verified` | boolean | ADMIN/FM checkmark |
| `bill_file_path` | text | Uploaded receipt image path |

Why 4-layer amounts? Because every actor in the chain may adjust: FE estimates → SUP revises → OWNER approves → FE realizes. All versions preserved for audit, not overwritten.

**`task_expense_histories`** — Immutable audit trail per stage transition:

| Column | Type | Purpose |
|--------|------|---------|
| `task_expense_id` | FK | Parent form |
| `from_stage` | enum | Stage before transition |
| `to_stage` | enum | Stage after transition |
| `actor_id` | FK → users | Who performed the transition |
| `notes` | text | Actor notes |
| `changes` | JSONB | Diff of changed fields (PostgreSQL JSONB) |

JSONB `changes` field stores what actually changed: `{"approved_amount": {"old": 500000, "new": 450000}}`. Allows precise audit reconstruction.

**`budget_item_templates`** — 35 predefined budget categories:

| Column | Type | Purpose |
|--------|------|---------|
| `name` | string | Category name (VOUCHER, BURUH, AKOMODASI, etc.) |
| `pagu_type` | enum | FIXED_PAGU, TICKET, MANAGER_APPROVAL |
| `unit` | string | HARI, TITIK, ORANG, UNIT |
| `wajib_bukti` | boolean | Receipt mandatory? (TICKET only) |
| `description` | text | Help text for field engineers |

**`pagu_job_type_amounts`** — Pivot: pagu amount per job type per category. 7 FIXED_PAGU categories × 5 job types = 35 rows maximum:

| Column | Type | Purpose |
|--------|------|---------|
| `budget_item_template_id` | FK | Category |
| `job_type` | enum | Which job type this amount applies to |
| `amount` | bigint | Fixed pagu amount in Rupiah |

Example: BURUH = 120rb for INSTALASI, null for SURVEY (no buruh needed for survey).

**`master_locations`** — Work site registry:

| Column | Type | Purpose |
|--------|------|---------|
| `uuid` | UUID | Sync identifier |
| `project_id` | FK → projects | Parent project |
| `provinsi` | string | Province |
| `kota` | string | City/Regency |
| `alamat` | text | Full address |
| `latitude` | decimal | GPS coordinate |
| `longitude` | decimal | GPS coordinate |

### Laporan Pekerjaan — 4 Tables

Linked to `task_expenses` (one budget = one optional laporan). Captures the technical side beyond money.

**`laporan_pekerjaan`** — Technical work report header:
- FK → `task_expenses`
- Parameter sinyal: RX, TX, EbNo, MODCOD (telco-specific)
- Sarpen: antenna type, azimuth, elevation
- Tindakan: corrective actions text field
- Catatan: open notes

**`perangkat_terpasang`** — Installed equipment:
- FK → `laporan_pekerjaan`
- Type: VSAT or M2M
- Merk, model, serial number

**`perangkat_rusak`** — Damaged/replaced equipment:
- FK → `laporan_pekerjaan`
- Type, merk, model, serial number
- Damage description

**`laporan_pekerjaan_foto`** — SCM photo checklist (19 items):
- FK → `laporan_pekerjaan`
- Check item name (from equipment options)
- Photo file path

### Master Data — 1 Table

**`master_equipment_options`** — Dynamic dropdown values:
- `field_key`: JENIS_ANTENNA, JENIS_MODEM, GANGGUAN, FOTO_SCM
- `value`: Display text
- Used by both Android and Web forms

## Relationships Diagram

```
task_expenses ──┬── project (FK → projects)
                ├── location (FK → master_locations)
                ├── submitted_by (FK → users)
                ├── forwarded_by (FK → users)
                ├── approved_by (FK → users)
                ├── verified_by (FK → users)
                ├── reconciled_by (FK → users)
                ├── items (1:N → expense_items)
                │     └── budget_item_template (FK → budget_item_templates)
                │           └── pagu amounts (1:N → pagu_job_type_amounts)
                ├── histories (1:N → task_expense_histories)
                │     └── actor (FK → users)
                └── laporan (1:1 → laporan_pekerjaan)
                      ├── perangkat_terpasang (1:N)
                      ├── perangkat_rusak (1:N)
                      └── foto (1:N → laporan_pekerjaan_foto)
```

## Compatibility Rules

- All money columns: `bigint` (PostgreSQL), `Long` (Kotlin Room). Never `float`/`double`/`decimal` for currency.
- Soft-delete: financial records have `deleted_at` timestamp. Hard-delete only for non-audit data.
- Approved transactions are immutable at DB level — enforced by application logic + stage checks.
- `uuid` for sync-enabled entities. Local Room `Long id` preserved for Android internal use.
- Additive migrations only: new columns/tables, never drop or rename existing.

## Config

All business parameters centralized in `config/budget.php`:

```php
return [
    'max_drafts' => env('BUDGET_MAX_DRAFTS', 5),
    'pagination' => env('BUDGET_PAGINATION', 20),
    'history_limit' => env('BUDGET_HISTORY_LIMIT', 10),
    'rejectable_stages' => ['ESTIMASI', 'FORWARDED'],
    'job_types' => ['INSTALASI', 'RELOKASI', 'PMCM', 'DISMANTLE', 'SURVEY'],
];
```

## See Also

- [[backend]] — API routes and controllers that query these tables
- [[workflows]] — 7-stage flow that drives state transitions
- [[architecture]] — Where this database fits in the stack

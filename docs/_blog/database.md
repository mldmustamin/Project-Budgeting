---
created: 2026-06-30
status: complete
tags: [database, schema, migration]
---

# Database Schema

## New Tables (Phase 1)
9 new tables + 1 pivot = 10 additions to existing 22 = **31 total**

### Core Workflow
- `budget_item_templates` — 35 kategori biaya with pagu rules
- `pagu_job_type_amounts` — Pivot: pagu per job_type (7 cats × 5 jobs)
- `master_locations` — Location master data (provinsi, kota, address)
- `task_expenses` — Single form, 7-stage workflow
- `expense_items` — Per-item with 4-layer nominal (estimated/revised/approved/realization)
- `task_expense_histories` — Audit trail per stage transition

### Laporan Pekerjaan
- `laporan_pekerjaan` — Technical work report (parameter sinyal, sarpen, tindakan)
- `perangkat_terpasang` — Installed equipment (VSAT/M2M)
- `perangkat_rusak` — Damaged/replaced equipment (VSAT/M2M)
- `laporan_pekerjaan_foto` — Photo uploads (19 SCM checklist)

### Master Data
- `master_equipment_options` — Dropdown values (antenna, modem, gangguan, foto checklist)

## Existing Tables (22)
users, projects, categories, transactions, attachments, devices, sync_outboxes, audit_events, accounting_periods, project_assignments, roles, permissions, sessions, personal_access_tokens, jobs, failed_jobs, cache, telescope, model_has_permissions, model_has_roles, role_has_permissions

## Relationships Diagram
```
task_expenses ──┬── project (FK)
                ├── location → master_locations
                ├── submitted_by → users
                ├── forwarded_by → users
                ├── approved_by → users
                ├── verified_by → users
                ├── reconciled_by → users
                ├── items → expense_items → budget_item_templates → pagu_job_type_amounts
                ├── histories → task_expense_histories → users
                └── laporan_pekerjaan → perangkat_terpasang, perangkat_rusak, laporan_pekerjaan_foto
```

## Config
All business parameters in `config/budget.php`:
- max_drafts: 5 (env: BUDGET_MAX_DRAFTS)
- pagination: 20 (env: BUDGET_PAGINATION)
- history_limit: 10 (env: BUDGET_HISTORY_LIMIT)
- rejectable_stages: [ESTIMASI, FORWARDED]
- job_types: [INSTALASI, RELOKASI, PMCM, DISMANTLE, SURVEY]

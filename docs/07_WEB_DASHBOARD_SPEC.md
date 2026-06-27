# Web Dashboard Specification

## Purpose

Advanced finance control center for owner, admin, and finance teams. **Not a read-only viewer.**

---

## Tech Stack (Locked — Laravel Blade + Livewire)

- **Framework:** Laravel 11 (rendered via Blade layouts + Livewire components)
- **UI:** Tailwind CSS with Livewire data tables (native `wire:model` + server-side pagination)
- **State:** Livewire component state — no client-side state library
- **Auth:** Laravel session-based auth (web), Sanctum for direct API calls
- **Charts:** Livewire-compatible chart library (e.g., ApexCharts)
- **Interactivity:** Alpine.js for lightweight client interactivity (modals, toggles, dropdowns)
- **No SPA complexity.** Server-rendered HTML with Livewire wire:model for data binding.

---

## Module Specifications

### Dashboard
- Total fund in / cash out / net position (org-wide, FINAL_APPROVED mode)
- Pending approval count
- Devices with sync errors
- Recent audit events

### Project
- CRUD with archive
- Project assignment matrix
- Start/end dates, PIC assignment

### Saldo Project
- Per-project balance cards
- Toggle: LOCAL_VIEW / FINAL_APPROVED / PROJECTED
- Drill-down to transaction list

### Transaction
- Searchable table: date, project, type, amounts, approval status
- Bulk export
- Row actions based on role

### Approval Center
- Queue: PENDING transactions
- Actions: Approve, Reject (reason required), Need Revision
- Immutable after approve — correction/void only

### Reconciliation
- Match reported vs real vs bank statements
- Mark matched/unmatched
- Export reconciliation report

### Settlement PIC
- PIC-level settlement batches
- Sign-off workflow

### Transfer
- Inter-project fund transfers
- Dual-entry creation (out + in)

### Budget
- Budget lines per project/category
- Actual vs budget variance

### Reports
- Superset of mobile PDF/Excel
- Scheduled reports (future)

### Sync Monitor
- Device list, last sync, pending/rejected counts
- Admin retry/clear conflict

### Audit Trail
- Filter by user, action, entity, date
- Export CSV

### User Management
- CRUD users, assign roles
- Deactivate (no hard delete)

### Device Management
- Register/revoke devices
- View active sessions

### Project Assignment
- Map FIELD_ENGINEER / PIC to projects
- Effective date ranges

---

## UX Rules

1. All money displayed formatted; stored/transmitted as integer
2. Dangerous actions: modal with reason textarea + confirm button
3. Approved transaction edit button hidden/disabled
4. Correction flow creates linked new transaction
5. Void shows strikethrough in lists, excluded from FINAL_APPROVED totals
6. Role-based nav — hide unauthorized modules entirely

---

## Role → Module Access Matrix (Initial)

| Module | OWNER | ADMIN | FIN_MGR | SUPERVISOR | PIC | FIELD_ENG | AUDITOR | VIEWER |
|--------|-------|-------|---------|------------|-----|-----------|---------|--------|
| Dashboard | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| Approval | ✓ | ✓ | ✓ | ✓ | — | — | — | — |
| Reconciliation | ✓ | ✓ | ✓ | — | — | — | ✓ | — |
| User Mgmt | ✓ | ✓ | — | — | — | — | — | — |
| Device Mgmt | ✓ | ✓ | — | — | — | — | — | — |
| Audit | ✓ | ✓ | ✓ | — | — | — | ✓ | — |

Detailed permissions in `docs/09_RBAC_SECURITY.md`.

---

## Phase 6 Deliverables

Blade + Livewire foundation scaffolded in `backend/`. Deliverables:
1. Auth shell + role router
2. Project + Transaction list (read)
3. Approval Center MVP
4. Sync Monitor read-only
5. Expand modules in Phase 7

Current scaffold: `resources/views/welcome.blade.php` exists as entry point.

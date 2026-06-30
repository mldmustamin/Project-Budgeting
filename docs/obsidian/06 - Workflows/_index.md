---
created: 2026-06-30
updated: 2026-07-01
status: production
tags: [workflow, budget, approval, sync]
---

# Budget Request Workflow — Full Lifecycle

## Why This Workflow Exists

Manual budget approval in field engineering is slow and opaque: paper receipts get lost, managers don't know real-time spending, finance teams reconcile Excel spreadsheets after the project ends. This 7-stage digital workflow solves all three: (1) real-time visibility for OWNER, (2) structured estimates with pagu enforcement for FIELD_ENGINEER, (3) automated audit trail for FINANCE_MANAGER.

## The 7 Stages

```
STAGE 0:   ASSIGN — SUP assigns task (out-of-band, not in state machine)

STAGE 1:   DRAFT — FE creates estimate: location, job type, 35 category items
           │         Max 5 drafts. FIXED_PAGU auto-calculated. Hotel = hari × tarif.
           │         Save to Room DB (offline-safe). Pending outbox entry.
           ▼
STAGE 2:   ESTIMASI — FE clicks "Submit". Form enters SUP Inbox.
           │           No going back: FE can no longer edit.
           ▼
STAGE 3:   FORWARDED — SUP reviews item by item. Adjusts revised_amount.
           │             Commits revision. Goes to OWNER queue.
           ▼
STAGE 4:   APPROVED — OWNER sees location history ("spent Rp X here before?").
           │            Sets final approved_amount. MANAGER_APPROVAL items: OWNER decides value.
           │            FIXED_PAGU: auto-set, OWNER sees but can't change.
           │            ▶ IMMUTABLE from this point. Correction = new form. Void = soft-delete.
           ▼
           ═══ PEKERJAAN DI LAPANGAN (days/weeks) ═══

STAGE 5:   REALISASI — FE returns from field. Inputs actual spending per item.
           │            Uploads bukti kwitansi (TICKET items mandatory).
           │            Completes LAPORAN PEKERJAAN (equipment, signals, 19 photos, actions).
           ▼
STAGE 6:   VERIFIED — ADMIN / FM checks receipts. Sets bill_verified flag.
           │            TICKET without receipt: FM sets nominal manually.
           │            Cross-checks realization vs approved budget.
           ▼
STAGE 7:   RECONCILED — FM crosschecks with Supervisor. Closes against accounting period.
                        Final state. No further transitions. Budget is closed and reportable.
```

## What Happens at Each Stage Transition (Server-Side)

Every transition is a single API call that does multiple things atomically:

### submit (DRAFT → ESTIMASI)

```
POST /api/v1/task-expenses/{uuid}/submit
Authorization: FIELD_ENGINEER, own task only, stage == DRAFT

Server:
  1. Validate: user is assigned to project, stage is DRAFT, ≤ 5 drafts
  2. Pagu enforcement:
     - FIXED_PAGU items: system sets estimated_amount from config/budget.php
     - TICKET items: sets wajib_bukti = true on item
     - MANAGER_APPROVAL items: passes through (amount = 0, OWNER sets later)
  3. UPDATE task_expenses SET stage = 'ESTIMASI', submitted_at = NOW()
  4. INSERT task_expense_histories (from=DRAFT, to=ESTIMASI, actor=FE, changes=null)
  5. Return updated task_expense with items
```

### forward (ESTIMASI → FORWARDED)

```
POST /api/v1/task-expenses/{uuid}/forward
Authorization: SUPERVISOR, assigned to project, stage == ESTIMASI

Server:
  1. Validate SUP's project assignment, stage check
  2. UPDATE expense_items SET revised_amount for any SUP-modified items
  3. UPDATE task_expenses SET stage = 'FORWARDED', forwarded_by = SUP.id
  4. INSERT history with changes JSON diff: {items: [{id, estimated, revised}]}
  5. OWNER's approval queue now includes this form
```

### approve (FORWARDED → APPROVED)

```
POST /api/v1/task-expenses/{uuid}/approve
Authorization: OWNER only, stage == FORWARDED

Server:
  1. Validate OWNER role (sole approver — no other role can call this)
  2. Load location history: SELECT SUM(approved_amount) FROM task_expenses
     WHERE location_id = ? AND stage IN ('APPROVED','REALISASI','VERIFIED','RECONCILED')
  3. For each expense_item:
     - FIXED_PAGU: approved_amount = estimated_amount (auto, OWNER can't change)
     - TICKET: approved_amount = OWNER's input
     - MANAGER_APPROVAL: approved_amount = OWNER's input (the whole point of this category)
  4. UPDATE task_expenses SET stage = 'APPROVED', approved_by = OWNER.id
  5. INSERT history with JSON diff of all approved_amounts
  6. ▶ ROW IS NOW IMMUTABLE. No further edits allowed.
```

### reject (ESTIMASI/FORWARDED → DRAFT)

```
POST /api/v1/task-expenses/{uuid}/reject
Authorization: SUPERVISOR or OWNER, stage in [ESTIMASI, FORWARDED]

Server:
  1. Validate stage is rejectable
  2. RESET: clear revised_amount on all items, clear approved_amount
  3. UPDATE task_expenses SET stage = 'DRAFT', rejection_reason = ?
  4. INSERT history (from=ESTIMASI/FORWARDED, to=DRAFT, notes=reason)
  5. FE sees form back in Draft tab with rejection_reason displayed
```

### realize (APPROVED → REALISASI)

```
POST /api/v1/task-expenses/{uuid}/realize
Authorization: FIELD_ENGINEER, own task, stage == APPROVED

Server:
  1. Validate FE owns the task, stage is APPROVED
  2. UPDATE expense_items SET realization_amount for each item
  3. If LAPORAN PEKERJAAN attached:
     - INSERT/UPDATE laporan_pekerjaan (signal params, tindakan)
     - UPSERT perangkat_terpasang (installed equipment)
     - UPSERT perangkat_rusak (damaged equipment)
     - INSERT laporan_pekerjaan_foto (19 SCM checklist items)
  4. UPDATE task_expenses SET stage = 'REALISASI'
  5. INSERT history with realization data
```

### verify (REALISASI → VERIFIED)

```
POST /api/v1/task-expenses/{uuid}/verify
Authorization: ADMIN or FINANCE_MANAGER, stage == REALISASI

Server:
  1. Validate role, stage
  2. For each expense_item:
     - Check bukti_kwitansi file exists for TICKET items
     - Set bill_verified = true/false
     - If TICKET and no receipt: FM manually sets realization_amount
  3. UPDATE task_expenses SET stage = 'VERIFIED', verified_by = actor.id
  4. INSERT history
```

### reconcile (VERIFIED → RECONCILED)

```
POST /api/v1/task-expenses/{uuid}/reconcile
Authorization: FINANCE_MANAGER only, stage == VERIFIED

Server:
  1. Validate FM role
  2. Check accounting period is open
  3. Final cross-check: does realization match approved within tolerance?
  4. UPDATE task_expenses SET stage = 'RECONCILED', reconciled_by = FM.id
  5. INSERT history
  6. ▶ TERMINAL STATE. Budget closed. Reportable.
```

## Why Rejection Resets to DRAFT (Not Previous Stage)

If rejection returned to ESTIMASI, the system would need to track "which stage did we reject from?" — adding complexity. Resetting to DRAFT is simpler: FE gets a clean slate with `rejection_reason` as guidance. The history table preserves the full audit trail of what happened before rejection (estimated amounts, supervisor's revised amounts, OWNER's attempted approval).

Available only at: ESTIMASI, FORWARDED. After APPROVED: correction form or void.

## Why 4 Layers of Nominal Tracking

| Layer | Set By | Stage | Purpose |
|--------|--------|-------|---------|
| `estimated_amount` | FE or System | DRAFT | Initial estimate (auto for FIXED_PAGU) |
| `revised_amount` | SUP | FORWARDED | Supervisor adjustment after review |
| `approved_amount` | OWNER | APPROVED | Final approved budget (immutable after) |
| `realization_amount` | FE | REALISASI | Actual spending in the field |

All four preserved per item. Not overwritten. This gives FINANCE_MANAGER full visibility: "FE estimated 500K, SUP revised to 450K, OWNER approved 430K, FE actually spent 425K." Every difference is audit-able.

## Pagu Enforcement: When and Where

| Type | Enforced At | By Whom | Rule |
|------|------------|---------|------|
| FIXED_PAGU | submit() | Server | System sets amount from config/budget.php. FE input ignored. |
| TICKET | verify() | ADMIN/FM | Receipt mandatory. No receipt → FM sets nominal. |
| MANAGER_APPROVAL | approve() | OWNER | No ceiling. OWNER decides. The "pagu" is the OWNER's judgment. |

Config source: `config/budget.php` + `pagu_job_type_amounts` pivot table. VOUCHER=15rb/hari (INSTALASI), BURUH=120rb/titik, BALLAST=200rb/titik, etc.

## Audit Trail: Every Mutation Tracked

`task_expense_histories` records:

| Column | Example | Purpose |
|--------|---------|---------|
| `from_stage` | FORWARDED | Stage before |
| `to_stage` | APPROVED | Stage after |
| `actor_id` | 3 (OWNER) | Who did it |
| `notes` | "Disetujui sesuai revisi kordinator" | Human reason |
| `changes` (JSONB) | `{"approved_amount": {"old": 0, "new": 430000}}` | What changed |

History rows are append-only, never edited, never deleted. Timeline is reconstructable: "Who approved this? When? From what stage? What amounts did they set?"

## Immutability After Approval

Once stage = APPROVED:
- The `task_expenses` row and its `expense_items` are logically immutable
- No further edits via PUT endpoint
- To fix a mistake: create a CORRECTION form (new task_expenses, `correction_of` FK to original)
- To cancel entirely: create a VOID (sets `deleted_at`, excluded from active balance, still in DB)
- Hard-delete: never. Financial records are never physically removed

This is not a preference — it's a hard constraint matching the `.cursorrules` finance ledger rules.

## Race Condition Prevention

Optimistic locking on `task_expenses`:

```
UPDATE task_expenses
SET stage = 'FORWARDED', lock_version = lock_version + 1
WHERE uuid = ? AND lock_version = ?

If rows_affected == 0 → conflict (someone else changed it)
If rows_affected == 1 → success
```

Prevents: SUP forwarding at the exact same moment OWNER rejects. One wins, the other gets HTTP 409 Conflict and must re-fetch.

## Edge Cases Handled

| Case | Behavior |
|------|----------|
| FE submits while offline | Outbox entry queued. Toast "Menunggu jaringan". Pushes when online. |
| SUP rejects then FE already revised offline | Conflict: FE's outbox push returns 409. FE sees rejection_reason, merges changes. |
| OWNER tries to approve already-rejected form | Stage mismatch: API returns 422 "Cannot approve from DRAFT stage" |
| Multiple SUP in same project | Any SUP with project assignment can forward. History tracks which SUP. |
| FM tries to close period with pending verification | Period close rejects if any task_expenses in REALISASI stage without VERIFIED |
| FE creates 6th draft | API returns 422 "Maximum 5 drafts". Configurable via BUDGET_MAX_DRAFTS. |
| Device lost with pending outbox | Outbox scoped by deviceId. New device login starts fresh session. Old outbox ages out. |

## See Also

- [[backend]] — API endpoints and controller code for each transition
- [[database]] — Full schema: task_expenses, expense_items, histories, laporan tables
- [[product]] — Why this workflow exists: the problem it solves
- [[architecture]] — Where this fits in the security model and data flow

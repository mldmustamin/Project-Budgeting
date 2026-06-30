---
created: 2026-06-30
updated: 2026-07-01
status: production
tags: [workflow, budget, approval, sync]
---

# Budget Request Workflow — Full Lifecycle

## Overview

The budget request moves through 7 stages involving 4 roles. Each transition is audited. Rejection resets to DRAFT. The workflow ensures: (1) pagu enforcement upstream, (2) supervisor review, (3) manager-only approval, (4) post-work realization with receipts, (5) finance verification and reconciliation.

## Stage-by-Stage Flow

```
┌────────────┐
│ STAGE 0    │  SUPERVISOR assigns task via app → FE gets notification
│ ASSIGN     │  (Out of band — not part of the 7-stage state machine)
└─────┬──────┘
      │
┌─────▼──────┐
│ STAGE 1    │  FIELD_ENGINEER fills estimate form:
│ DRAFT      │  - Select location (cached from server)
│            │  - Pick job type (INSTALASI/RELOKASI/PMCM/DISMANTLE/SURVEY)
│            │  - Add expense items (35 categories, auto-calc FIXED_PAGU)
│            │  - Hotel auto-calc: jumlah_hari × tarif_per_hari
│            │  - Save draft → Room DB (offline-safe)
│            │  Max 5 concurrent drafts per user
└─────┬──────┘
      │ FE: "Submit"
┌─────▼──────┐
│ STAGE 2    │  Inbox SUPERVISOR:
│ ESTIMASI   │  - Review all items line by line
│            │  - FIXED_PAGU: system-locked, FE cannot change
│            │  - TICKET: flagged as wajib bukti
│            │  - MANAGER_APPROVAL: passes through
│            │  - Adjust revised_amount if needed
│            │  Actions: Forward → Accept as-is → Reject (back to DRAFT)
└─────┬──────┘
      │ SUP: "Forward"
┌─────▼──────┐
│ STAGE 3    │  Visible to OWNER in approval queue:
│ FORWARDED  │  - Supervisor has committed their revision
│            │  - estimated_amount + revised_amount both recorded
│            │  Awaiting OWNER decision
└─────┬──────┘
      │ OWNER: "Approve"
┌─────▼──────┐
│ STAGE 4    │  OWNER reviews and finalizes:
│ APPROVED   │  - Location history: "how much was spent here before?"
│            │  - Set approved_amount per item (final budget)
│            │  - MANAGER_APPROVAL items: OWNER decides the value
│            │  - FIXED_PAGU items: auto-set, OWNER can see but not change
│            │  Once approved → immutable. Correction = new form, void = soft-delete
│            │  Actions: Approve with amounts → Reject (back to DRAFT)
└─────┬──────┘
      │
      │   ═══ PEKERJAAN DI LAPANGAN ═══
      │   (Field work happens here — days/weeks)
      │
┌─────▼──────┐
│ STAGE 5    │  FIELD_ENGINEER returns from field:
│ REALISASI  │  - Input actual spending per item (realization_amount)
│            │  - Upload bukti kwitansi for TICKET items
│            │  - Complete LAPORAN PEKERJAAN:
│            │    · Installed equipment (VSAT/M2M models)
│            │    · Damaged/replaced equipment
│            │    · Signal parameters: RX, TX, EbNo, MODCOD
│            │    · 19-point SCM photo checklist
│            │    · Corrective actions (tindakan)
│            │  Submit → enters verification queue
└─────┬──────┘
      │ ADMIN/FM: "Verify"
┌─────▼──────┐
│ STAGE 6    │  ADMIN or FINANCE_MANAGER verification:
│ VERIFIED   │  - Check bukti kwitansi per item
│            │  - Set bill_verified flag
│            │  - TICKET items without receipt: FM sets nominal
│            │  - Cross-check: does realization match approved budget?
│            │  Admin + FM share this responsibility
└─────┬──────┘
      │ FM: "Reconcile"
┌─────▼──────┐
│ STAGE 7    │  FINANCE_MANAGER final reconciliation:
│ RECONCILED │  - Crosscheck with Supervisor (was work actually done?)
│            │  - Match against accounting period
│            │  - Final state — no further transitions
│            │  Budget is now closed and reportable
└────────────┘
```

## Rejection Flow

Rejection is available at ESTIMASI and FORWARDED stages only. After APPROVED, rejection is not allowed — use correction or void instead.

```
ESTIMASI ── reject ──▶ DRAFT (reset all revisions)
  SUP clicks "Reject" with reason "Harga terlalu tinggi"
  → supervisor's revised_amount cleared
  → FE sees form back in Draft tab with rejection_reason
  → FE revises → Submit → ESTIMASI again

FORWARDED ── reject ──▶ DRAFT
  OWNER clicks "Reject" with reason "Lokasi sudah ada budget sebelumnya"
  → all revisions cleared
  → FE must restart from scratch
```

## Offline Flow (Field Engineer)

```
FE opens app in remote area (no signal)
  ↓
FE creates budget estimate:
  - Location dropdown: cached from last sync
  - Category dropdown: cached 35 templates
  - Hotel calc: local Room query for tarif_per_hari
  - Save → INSERT into Room (status=DRAFT, syncStatus=PENDING)
  ↓
FE clicks "Submit":
  - Stage: DRAFT → ESTIMASI in Room
  - Outbox entry created: {operationId, type: "SUBMIT_TASK_EXPENSE", payload: {uuid, ...}}
  - Toast: "Menunggu jaringan — akan dikirim otomatis"
  ↓
Later: FE back in coverage area
  ↓
WorkManager PeriodicSyncWorker triggers (15 min)
  OR FE pulls-to-refresh
  ↓
POST /api/v1/sync/push
  → Server processes, returns {accepted: [uuid], rejected: []}
  ↓
GET /api/v1/sync/pull
  → Room updates: supervisor's forward, owner's approval
  ↓
FE sees: "Budget disetujui: Rp 12.500.000" on Dashboard
```

## Pagu Enforcement Detail

| Pagu Type | Who Sets Amount | Can FE Change? | Receipt Required? |
|-----------|----------------|---------------|-------------------|
| FIXED_PAGU | System (config/budget.php) | No — grayed out | No |
| TICKET | FE estimates, FM verifies | Yes | Yes — wajib |
| MANAGER_APPROVAL | OWNER at approval stage | No (passes through) | No |

## Immutability After Approval

Once stage = APPROVED:
- The `task_expense` row is logically immutable
- To fix a mistake: create a CORRECTION form (new task_expense, links to original)
- To cancel entirely: create a VOID form (sets `deleted_at`, excluded from balance)
- Hard-delete is never used for financial records

This matches the [[finance-rules]]: approved transaction immutable, correction/void for changes.

## See Also

- [[backend]] — API endpoints for each stage transition
- [[database]] — Table schema behind this workflow
- [[product]] — Why this workflow exists (problem it solves)
- [[sessions]] — Session logs of building this workflow

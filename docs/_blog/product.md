---
created: 2026-06-30
status: active
tags: [product, prd, requirements, personas]
---

# Product Requirements — FundManager V2

## The Problem

Field engineering projects (telco, infrastructure) have budget distributed across teams. Each field engineer estimates costs, supervisor validates, manager approves, then the actual spending (realization) must match. Without a structured system:

- Paper receipts lost, amounts unrecorded
- No visibility: manager doesn't know budget vs actual until project closes
- Field engineers can't submit while offline (common in remote areas)
- Multiple engineers share one device — no user isolation
- Finance team manually reconciles Excel spreadsheets

**FundManager V2 replaces this with a 7-stage digital workflow across Android + Web.**

## 6 User Personas

### OWNER (Manager Proyek)

The budget owner. Only person who can approve budget requests and set final nominal amounts. Views location history (how much was spent here before?) before approving. Full dashboard access across all projects.

- **Key need:** Visibility — know budget vs realization in real-time, not after project ends.
- **Platform:** Web (advanced finance control center)

### FINANCE_MANAGER

Does NOT approve budgets. Matches realization data against supervisor-submitted estimates. Cross-checks receipts (bukti kwitansi) per expense item. Manages period closing and final reconciliation.

- **Key need:** Audit trail — who submitted, who approved, who verified, when.
- **Platform:** Web

### ADMIN

Data reconciliation partner with FINANCE_MANAGER. Manages master data (locations, equipment options). Full dashboard access but cannot approve budgets. Verifies realization data.

- **Key need:** Clean master data — dropdown options consistent across all forms.
- **Platform:** Web

### SUPERVISOR (Kordinator Lapangan)

Team lead. Receives budget estimates from field engineers in inbox. Reviews, adjusts item amounts (revised_amount), forwards to OWNER. Approves daily transactions from team. Assigns tasks to field engineers.

- **Key need:** Inbox workflow — don't let estimates pile up unseen.
- **Platform:** Web + Android (both)

### FIELD_ENGINEER

The boots-on-ground. Fills estimate forms and realization forms. Submits Laporan Pekerjaan (technical work report with equipment details, signal parameters, photo checklist). Works in remote areas — must function offline.

- **Key need:** Offline-first — create drafts without internet, sync when back online.
- **Platform:** Android (primary), Web (secondary)

### AUDITOR

Read-only across all data. Cannot create, edit, delete, or approve. Views reports, audit trails, sync logs.

- **Key need:** Transparency — every mutation has timestamp and actor recorded.

## Core Capabilities

### 1. Budget Request Workflow (7 Stages)

DRAFT → ESTIMASI → FORWARDED → APPROVED → REALISASI → VERIFIED → RECONCILED. Every stage transition is audited (who, when, old value → new value). Rejection resets to DRAFT for revision.

### 2. Pagu Enforcement (35 Categories, 3 Channels)

- **FIXED_PAGU (10 categories):** Auto-locked — system determines value, field engineer cannot change. Example: VOUCHER = 15rb/hari, BURUH = 120rb/titik.
- **TICKET (12 categories):** Wajib bukti. If no receipt uploaded, Finance sets the nominal during verification.
- **MANAGER_APPROVAL (13 categories):** OWNER decides final amount. No system-enforced ceiling.

### 3. Laporan Pekerjaan

Technical work report beyond just money: installed equipment (VSAT/M2M models), damaged/replaced equipment, signal parameters (RX/TX, EbNo, MODCOD), 19-point SCM photo checklist, tindakan (corrective actions). Linked to budget realization — you can't close realization without completing laporan.

### 4. Offline-First Architecture

Room DB as local SSOT. WorkManager periodic sync (15 min) + one-shot after login. Outbox pattern: every mutation creates an outbox entry scoped by user, device, and session. Retry = idempotency key prevents duplicates on server.

### 5. Multi-User Per Device

Single APK, multiple users. Login/logout doesn't clear pending sync. Each user has isolated sync outbox. User A's pending changes never sent as User B. Active session includes `localUserId`, `serverUserId`, `userUuid`, `deviceId`, `sessionId`.

### 6. Hard Money Rules

All currency = `Long`/`BigInt`. No `Double`/`Float` anywhere — not in Kotlin, not in PHP, not in JavaScript. Approved transactions are immutable — correction creates a new transaction, void marks soft-deleted (never hard-deleted). Rejected/voided excluded from active balance.

### 7. Audit Trail

Every stage transition logged: `task_expense_histories` table with `from_stage`, `to_stage`, `actor_id`, `notes`, `changes` (JSON diff). Server-side audit_events for all write operations. Android CrashReporter for client-side failures.

## What This Replaces

Manual paper-based workflow: engineer fills paper form → supervisor signs → manager approves → paper filed. No search, no history, no real-time balance. FundManager V2 digitizes the entire chain with sync, offline support, and multi-role access control.

## See Also

- [[architecture]] — System design and tech stack
- [[workflows]] — 7-stage budget flow in detail
- [[backend]] — API routes and controllers
- [[android]] — Screen catalog and architecture

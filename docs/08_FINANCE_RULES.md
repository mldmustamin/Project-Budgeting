# Finance Rules

## Money Representation

- **Always** `Long` (Android/Kotlin), `BIGINT` (PostgreSQL), `BigInt` (TypeScript) for whole rupiah
- **Never** `Double`, `Float`, or floating-point JSON numbers for amounts
- Display formatting only in UI layer (`UiFormatters`, locale id-ID)

---

## Core Summary Formula (LOCAL_VIEW — SSOT)

Source: `CalculateProjectSummaryUseCase`

```
totalFundIn          = Σ FUND_IN.reportedAmount
totalOfficeReported  = Σ OFFICE_EXPENSE.reportedAmount
totalOfficeReal      = Σ OFFICE_EXPENSE.realAmount
totalPersonalExpense = Σ PERSONAL_EXPENSE.realAmount

saving               = totalOfficeReported - totalOfficeReal
remainingReported    = totalFundIn - totalOfficeReported
remainingReal        = totalFundIn - totalOfficeReal
totalCashOut         = totalOfficeReal + totalPersonalExpense
netPosition          = totalFundIn - totalCashOut
```

**Filter:** Include only rows where `deletedAt IS NULL`.

---

## Calculation Modes (Planned)

| Mode | Include | Use Case |
|------|---------|----------|
| LOCAL_VIEW | All non-deleted local rows | Current Android default; field view |
| FINAL_APPROVED | APPROVED, non-voided only | Official org balance |
| PROJECTED | FINAL_APPROVED + PENDING approval | Forecast / planning |

Implementation: extend `CalculateProjectSummaryUseCase` with optional `mode` parameter; default `LOCAL_VIEW` preserves current behavior.

---

## Transaction Lifecycle

```
DRAFT → PENDING → APPROVED
              ↘ REJECTED
              ↘ NEED_REVISION → (edit) → PENDING
APPROVED → VOID (soft, excluded from FINAL_APPROVED)
APPROVED → CORRECTION (new linked transaction)
```

### Immutability Rule
Once `approvalStatus = APPROVED`:
- No in-place edit of amounts, date, type, or description
- Changes via `correction` (new row) or `void` (soft exclude)

---

## Transaction Types

| Type | reportedAmount | realAmount | In totalFundIn | In office totals | In personal |
|------|----------------|------------|----------------|------------------|-------------|
| FUND_IN | ✓ | = reported | ✓ | — | — |
| OFFICE_EXPENSE | ✓ | ✓ | — | ✓ | — |
| PERSONAL_EXPENSE | ✓ | ✓ | — | — | ✓ |

---

## Soft Delete vs Void

| Action | deletedAt | financeStatus | In LOCAL_VIEW | In FINAL_APPROVED |
|--------|-----------|---------------|---------------|-------------------|
| Soft delete (current) | set | — | excluded | excluded |
| Void (future) | null | VOIDED | visible with badge | excluded |
| Correction | — | CORRECTED on original | both rows visible | net effect |

Do not conflate soft delete with void without migration plan.

---

## Period Closing

- Server rejects push for transactions dated in closed period
- Android shows rejection reason locally
- Web admin can reopen period (audited, rare)

---

## Reconciliation Rules

- reportedAmount = what field engineer reported to office
- realAmount = actual cash spent
- saving = reported - real (office expense only)
- Reconciliation matches realAmount against external sources (bank, receipts)

---

## Transfer Rules (Planned)

Inter-project transfer creates:
1. FUND_OUT or equivalent deduction on source project
2. FUND_IN on destination project
Linked by `transferGroupUuid`

---

## Validation (Existing)

`ValidateTransactionUseCase`:
- Date required, format yyyy-MM-dd
- reportedAmount > 0, realAmount > 0
- accountId required
- description required for expenses
- Duplicate warning (non-blocking)

---

## Test Requirements

Any formula change requires updates to:
- `CalculateProjectSummaryUseCaseTest`
- `CsvExportConsistencyTest`
- New mode-specific tests when modes added

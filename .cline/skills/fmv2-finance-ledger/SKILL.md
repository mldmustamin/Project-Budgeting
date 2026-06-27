# FMv2 Finance Ledger

Use this skill for balance rules, approval state, and ledger integrity.

## Read first
- docs/08_FINANCE_RULES.md
- docs/10_DATABASE_SCHEMA.md
- docs/12_TEST_PLAN.md

## Rules
- Keep all money values as Long.
- Preserve the SSOT summary formula unless the docs change.
- Do not mutate approved transactions in place.
- Use correction or void flows for approved records.
- Keep local view and final-approved view distinct.

## Preferred changes
- Ledger formula updates
- Summary calculation tests
- Approval-state guardrails
- Export consistency checks

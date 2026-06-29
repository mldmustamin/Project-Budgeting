---
created: 2026-06-30
status: pending
tags: [android, compose, room, sync]
---

# Android — Phase 6 Implementation

## Current Codebase
- 106 Kotlin files, 8,210 LOC
- Jetpack Compose UI + Room DB + WorkManager + Hilt DI
- Existing screens: Login, Dashboard, Transaction, Project, Settings

## Screens to Build (8 new)

| # | Screen | Role | ViewModel | Backend Endpoint |
|---|--------|------|-----------|-----------------|
| 1 | MyTasksScreen | FE | MyTasksViewModel | GET /task-expenses |
| 2 | BudgetEstimateFormScreen | FE | BudgetEstimateViewModel | POST /task-expenses |
| 3 | RealizationFormScreen | FE | RealizationViewModel | POST .../realize |
| 4 | SupervisorInboxScreen | SUP | SupervisorInboxViewModel | List + forward/reject |
| 5 | AssignTaskScreen | SUP | AssignTaskViewModel | POST /task-expenses |
| 6 | ApprovalScreen | OWNER | ApprovalViewModel | POST .../approve |
| 7 | VerificationScreen | ADMIN/FM | VerificationViewModel | POST .../verify |
| 8 | LaporanPekerjaanScreen | FE | LaporanViewModel | (API belum dibuat) |

## New Data Layer
- Room Entities: TaskExpenseEntity, ExpenseItemEntity, BudgetTemplateEntity, MasterLocationEntity, LaporanPekerjaanEntity, PerangkatEntity
- DAOs: TaskExpenseDao, BudgetTemplateDao, MasterLocationDao, LaporanDao
- Repositories: BudgetRepository, LaporanRepository
- Sync Extension: Expand SyncWorker for task_expense entities
- File Upload Worker: BuktiUploadWorker (separate from sync)

## Navigation Update
Bottom nav (`FundsManagerNavHost`):
- FE: Tasks | Transaksi | Projects | Sync | Settings
- SUP: Inbox | Transaksi | Locations | Projects | Settings
- OWNER: Approval | Transaksi | Projects | Settings
- ADMIN/FM: Verification | Transactions | Locations | Projects | Settings

## Required Skills
- `android-apk-build` — Build & distribute APK
- `android-debugging` — Runtime debugging checklist
- `simplify-code` — 3-agent parallel cleanup
- `test-driven-development` — RED-GREEN-REFACTOR per screen

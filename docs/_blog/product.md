---
created: 2026-06-30
status: active
tags: [product, prd, requirements]
---

# Product Requirements

## Executive Summary
FundManager V2: Budget management app for field engineering teams. Single APK with role-based UI. Server-authoritative backend with offline-first Android client.

## Target Users (6 Roles)
- **OWNER (Manager)**: Sole budget approver — approve + set nominal with location history
- **ADMIN**: Data reconciliation with FINANCE_MANAGER
- **FINANCE_MANAGER**: Realization matching, period closing, final reconciliation
- **SUPERVISOR (Kordinator)**: Forward budget + approve transactions
- **FIELD_ENGINEER**: Estimate + realization input (offline-capable)
- **AUDITOR**: Read-only across all data

## Core Capabilities
1. Budget Request Workflow (7-stage: DRAFT → RECONCILED)
2. Pagu Enforcement (35 categories, 3 types: FIXED/TICKET/MANAGER)
3. Laporan Pekerjaan (technical report: equipment, parameters, photos)
4. Offline-First (Room DB + WorkManager sync)
5. Role-Based UI (single APK, different screens per role)
6. Audit Trail (stage transition history)
7. Master Data Management (locations, equipment options)

## Full Document
See [[PRD]] (original file in project root)

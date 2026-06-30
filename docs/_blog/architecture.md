---
created: 2026-06-30
updated: 2026-07-01
status: production
tags: [architecture, system-design, tech-stack]
---

# System Architecture — FundManager V2

## Overview

FundManager V2 is a client-server platform: single Android APK (Kotlin/Compose) talks to a Laravel 11 API backend, with a Blade-based web dashboard for advanced finance operations. The system handles 6 roles across 7 budget stages, with offline support for field engineers working in remote areas.

```
┌─────────────────────────┐              ┌──────────────────────────┐
│  Android APK (Kotlin)   │──── HTTPS ──▶│  Laravel 11 API (PHP)    │
│  ┌─────────────────────┐│              │  ┌──────────────────────┐│
│  │ Room DB (offline)   ││              │  │ PostgreSQL 14       ││
│  │ WorkManager (sync)  ││◀─── JSON ───│  │ Spatie RBAC         ││
│  │ Hilt DI             ││              │  │ Sanctum Auth        ││
│  │ Jetpack Compose UI  ││              │  │ Blade + Tailwind    ││
│  └─────────────────────┘│              │  └──────────────────────┘│
│  Role-gated UI          │              │  Server-authoritative    │
│  Multi-user per device  │              │  VPS: 103.94.11.78      │
└─────────────────────────┘              └──────────────────────────┘
```

## Design Decisions

### Single APK, Not Multiple Apps

One Android APK serves all 6 roles. Role-based UI gating: after login, the app shows only screens the user's role permits. This avoids maintaining separate APKs for field engineers, supervisors, and managers — one build, one deployment, less fragmentation.

### Server-Authoritative

All business rules live on the server. The Android client is an input/output device — it never decides "is this pagu valid?" or "can this user approve?". The server rejects invalid operations, and the client syncs to reflect the authoritative state. This means:

- Pagu enforcement is server-side only (not duplicated on Android)
- Role checks run on every API write endpoint
- Android shows what the server last told it (pull sync reflects reality)

### Offline-First Android

Room DB is the single source of truth (SSOT) for the Android client. Every form, draft, and cached bit of master data lives in Room. The network is optional:

| State | Behavior |
|-------|----------|
| Online | Outbox pushes immediately, server responds, Room updates |
| Offline | Save to Room → enqueue in outbox → toast "menunggu jaringan" |
| Back online | WorkManager picks up outbox → push → pull → Room refreshed |

Sync is per-user, per-device, per-session. Logout doesn't flush the outbox.

### Dual Auth

| Channel | Auth Method | Purpose |
|---------|------------|---------|
| Android API | Laravel Sanctum (Bearer token) | Stateless API auth from mobile |
| Web Dashboard | Laravel Session + CSRF | Traditional web auth with Blade |
| Both | Spatie Permission + `hasRole()` | RBAC enforcement |

## Data Flow

### Write Path (Offline → Server)

```
Field Engineer fills form
  ↓
Room DB INSERT (local Long id + generated UUID)
  ↓
SyncOutbox INSERT {operationId, type, payload, userId, deviceId, sessionId}
  ↓
WorkManager picks up (15min periodic OR one-shot after login)
  ↓
POST /api/v1/sync/push {operations: [...]}
  ↓
Server validates: user, device, project assignment, permission, closing period
  ↓
Server responds: {accepted: [...], rejected: [{reason}]}
  ↓
Android marks outbox entries: synced / failed_with_reason
```

### Read Path (Server → Local)

```
WorkManager (periodic)
  ↓
GET /api/v1/sync/pull?since={lastSyncTimestamp}&userId={serverUserId}
  ↓
Server returns: {changes: [...], deleted: [uuid, ...]}
  ↓
Android upserts by UUID → Room DB
  ↓
UI recomposes via StateFlow
```

## Security Model

### Network

- All API calls over HTTPS
- Android: Ktor HttpClient with 15s connect/30s request timeout
- Server: rate limiting (Laravel throttle middleware)
- File uploads (attachments, photos) use presigned URLs or direct multipart

### Authentication

- Sanctum tokens: device-scoped, revocable from server
- Web sessions: standard Laravel session with CSRF protection
- Device registration: first login from new device → background coroutine registers device with server

### Authorization

Every write endpoint checks: (1) role permission, (2) project membership, (3) device authorization, (4) accounting period open.

```
TaskExpenseController@forward:
  Gate::authorize('forward-budget', $taskExpense)
    → checks: role=SUPERVISOR, project_assignment exists,
              current stage=ESTIMASI, period not closed
```

## Infrastructure

| Component | Spec |
|-----------|------|
| VPS | Ubuntu 22.04 @ 103.94.11.78 |
| PHP | 8.2-FPM via Unix socket (`/run/php/php8.2-fpm.sock`) |
| PostgreSQL | 14, user `fundsmanager`, DB `fundsmanager_production` |
| Redis | 7, used for cache + queue + session |
| Nginx | Reverse proxy → PHP-FPM, static assets |
| GitHub | Source @ `mldmustamin/Project-Budgeting`, Pages blog @ `/docs` |

## Tech Stack Detail

| Layer | Technology | Why |
|-------|-----------|-----|
| Android lang | Kotlin 2.0 | First-class Android language, null safety |
| UI | Jetpack Compose | Declarative, less boilerplate than XML |
| Local DB | Room | Official Android persistence, compile-time SQL verification |
| DI | Hilt | Official Android DI, less boilerplate than Dagger |
| Sync | WorkManager | Guaranteed execution, survives app restart |
| HTTP | Ktor Client | Kotlin-native, coroutine-friendly, configurable timeouts |
| Backend | Laravel 11 | Mature PHP framework, rich ecosystem (Sanctum, Spatie, Horizon) |
| DB | PostgreSQL 14 | ACID, JSONB for audit changes, window functions |
| Auth | Sanctum | SPA + mobile token auth, simpler than Passport for this use case |
| RBAC | Spatie | Battle-tested roles + permissions with Blade directives |
| Web UI | Blade + Tailwind + Alpine.js | Server-rendered (no SPA overhead), lightweight interactivity |

## Key Principles

- **Don't Repeat Yourself in data:** One source of truth per concept. Pagu amounts in `config/budget.php`, not duplicated in Android.
- **Additive only:** New columns/tables are additive. Never change existing column semantics without migration plan.
- **Offline parity:** Every Android screen has a Web equivalent. Both use the same API contracts.
- **Audit everything:** Money mutations, stage transitions, device registrations, login attempts — all logged.

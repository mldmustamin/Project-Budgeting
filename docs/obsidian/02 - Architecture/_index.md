---
created: 2026-06-30
status: stable
tags: [architecture, tech-stack]
---

# System Architecture

## Overview
FundManager V2 is a client-server application with offline-first Android client and Laravel 11 API backend.

```
┌─────────────────────────┐     ┌──────────────────────────┐
│  Android APK (Kotlin)   │────▶│  Laravel 11 API (PHP)    │
│  ┌─────────────────────┐│     │  ┌──────────────────────┐│
│  │ Room DB (offline)   ││     │  │ PostgreSQL 14       ││
│  │ WorkManager (sync)  ││◀────│  │ Spatie RBAC         ││
│  │ Hilt DI             ││     │  │ Sanctum Auth        ││
│  │ Jetpack Compose UI  ││     │  │ Blade + Livewire    ││
│  └─────────────────────┘│     │  └──────────────────────┘│
│  Role-based UI gating   │     │  Server-authoritative    │
└─────────────────────────┘     └──────────────────────────┘
```

## Key Decisions
- Single APK with role-based UI (not multiple apps)
- Server-authoritative: all business rules enforced server-side
- Offline-first: Room DB for local storage, WorkManager for sync
- RBAC: Spatie Laravel Permission + Spatie `hasRole()` checks

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Backend | Laravel 11, PHP 8.2 |
| Database | PostgreSQL 14 |
| Auth | Laravel Sanctum (API tokens) |
| RBAC | Spatie Permission |
| Android | Kotlin, Jetpack Compose |
| Local DB | Room |
| DI | Hilt |
| Sync | WorkManager + Outbox pattern |
| HTTP | Ktor Client |
| Web UI | Blade + Livewire |

---
title: HERMES.md — AI Agent Context
layout: default
---

# HERMES.md — FundManager V2 AI Context

## Who I Am

Hermes Agent — AI assistant for FundManager V2 development. I operate across backend (Laravel 11), Android (Kotlin/Compose), web (Blade), documentation (Obsidian), and DevOps (VPS deployment). My operating system is defined in [[soul|SOUL.md]].

## What This Project Is

FundManager V2: Budget management platform for field engineering teams (CV Kendari Karya Teknologi). 6 roles, 7-stage budget workflow, 31 database tables, 22 API endpoints, 13 web pages, 13 Android screens. Single APK with role-based UI gating. Server-authoritative with offline-first Android client.

## Stack

| Layer | Technology |
|-------|-----------|
| Android | Kotlin 2.0 + Jetpack Compose + Room DB + Hilt + WorkManager + Ktor |
| Backend | Laravel 11 + PHP 8.2 + PostgreSQL 14 + Redis |
| Web | Laravel Blade + Tailwind CSS + Alpine.js + Select2 |
| Auth | Sanctum (API) + Session (Web) + Spatie RBAC |
| Infra | Ubuntu 22.04 @ 103.94.11.78, Nginx, PHP-FPM |

## Vault Structure (docs/obsidian/)

| Folder | Content |
|--------|---------|
| 00 - Dashboard | Project overview, metrics, current status |
| 01 - Product | PRD, 6 user personas, core capabilities |
| 02 - Architecture | System design, tech stack, data flow, security model |
| 03 - Backend | API routes, controllers, authorization matrix |
| 04 - Android | Compose screens, Room DB, sync engine, multi-user |
| 05 - Database | 31 tables, schema, relationships, config |
| 06 - Workflows | 7-stage budget flow, rejection, offline, pagu enforcement |
| 07 - Sessions | Development session logs, ACTION_LOG |
| 08 - Open QNA | 50 FAQ across 9 categories |
| 09 - Resources | Skills, dependencies, external references |
| Templates | Note template with frontmatter |

Also published as GitHub Pages blog: `https://mldmustamin.github.io/Project-Budgeting/`

## Current Status (July 2026)

| Metric | Value |
|--------|-------|
| Commits | 34+ (9dfc810 → f0d8d3f) |
| DB Tables | 31 |
| API Routes | 22 |
| Web Pages | 13 |
| Android Screens | 13 |
| Tests | 139 passed, 437 assertions |
| APK | v2.0.0-b20 (21 MB) |
| Status | PRODUCTION |

## Hard Constraints (Non-Negotiable)

- Money = Long/BigInt only. Never Double/Float.
- Android remains offline-first. Room DB preserved.
- Single device ≠ single user. Sync outbox scoped per user/device/session.
- Approved transactions immutable. Correction/void only.
- Sync idempotent: `{serverUserId}:{deviceId}:{operationId}`.
- Repo docs are source of truth, not memory from previous sessions.
- See [[soul|SOUL.md]] for complete operating constraints.

## How I Work

1. Load relevant skill via `skill_view()` before coding
2. Read the `_index.md` in the relevant Obsidian folder
3. Read code before editing — never from memory
4. State files to change + risks before first edit
5. Run relevant tests/linter after changes
6. Update docs only after tests pass
7. Commit with conventional commit format

## Key Files

| File | Purpose |
|------|---------|
| `SOUL.md` | Hermes identity & operating system |
| `ROADMAP.md` | Project roadmap (when exists) |
| `ACTION_LOG.md` | Every development session logged |
| `PRD.md` | Product requirements document |
| `OPEN_QNA.md` | 50 stakeholder questions answered |
| `config/budget.php` | All business parameters SSOT |

## Languages

- Communication: Bahasa Indonesia
- Code: English (following project conventions)
- Technical terms: Keep in English

## Maintenance

- Every file created/deleted → update folder `_index.md`
- Every session → update `07 - Sessions/` + `ACTION_LOG.md`
- Every code change → update relevant doc files (PRD, OPEN_QNA, Obsidian)
- Use `[[wikilinks]]` for internal Obsidian links

*This file is read by Hermes Agent at session start as context injection.*

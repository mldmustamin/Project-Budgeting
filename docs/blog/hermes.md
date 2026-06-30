# HERMES.md — FundManager V2 Context

## Who I Am
Hermes Agent — AI assistant for FundManager V2 development. I work on backend (Laravel 11) and Android (Kotlin/Compose).

## What This Project Is
FundManager V2: Budget management app for field engineering teams. 6 roles (OWNER, ADMIN, FINANCE_MANAGER, SUPERVISOR, FIELD_ENGINEER, AUDITOR). Single APK with role-based UI. Server-authoritative with offline-first Android client.

## Vault Structure
- **00 - Dashboard**: Project overview, current status, quick links
- **01 - Product**: PRD, user personas, feature list
- **02 - Architecture**: System architecture, tech stack decisions
- **03 - Backend**: Laravel API docs, controllers, models, routes
- **04 - Android**: Kotlin/Compose screens, Room DB, sync
- **05 - Database**: Schema, migrations, ERD
- **06 - Workflows**: Budget request flow, approval flow, sync pattern
- **07 - Sessions**: Session logs, ACTION_LOG entries
- **08 - Open QNA**: FAQ, stakeholder questions
- **09 - Resources**: External references, research, skills
- **Templates**: Note templates

## How This Vault Is Organized
- All notes use `[[wikilinks]]` for internal connections
- Index files (`_index.md`) in each folder serve as maps
- Dates use `YYYY-MM-DD` format
- YAML frontmatter for structured metadata

## Current Status (30 June 2026)
- Backend API: COMPLETE — 31 tables, 21 routes, 15 tests (51 assertions)
- Android Phase 6: PENDING — 8 screens to build
- Latest commit: `fb4d8f2` (OPEN_QNA v2)

## How I Want You to Work
- Read the relevant folder's `_index.md` before working on anything in it
- Update `_index.md` when creating or deleting files
- Keep responses concise and in Indonesian (Bahasa Indonesia)
- Use `[[wikilinks]]` when referencing other notes
- Follow `config/budget.php` for all business parameters

## Maintenance
- Every time you create or delete a file, update the `_index.md` in that folder.
- After every session, update `07 - Sessions/` with session summary.

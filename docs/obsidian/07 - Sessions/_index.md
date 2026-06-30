---
created: 2026-06-30
updated: 2026-07-01
session: 20260630_000100_447baa
status: complete
tags: [sessions, logs, development-history]
---

# Session Logs — FundManager V2

## Session `20260630_000100_447baa` — Budget Request Workflow (Major)

**Model:** deepseek-v4-pro  
**Duration:** ~6 hours (00:00 — 06:00 WITA)  
**Commits:** 21 (9dfc810 → 055c58f)  
**APK Builds:** #16 (21 MB)  

### What Was Built

Complete budget request workflow across all three platforms:

#### Backend (Phase 1-5)

Migrated from zero to production API:
- **Phase 1:** 10 new database tables — `task_expenses`, `expense_items`, `budget_item_templates`, `pagu_job_type_amounts`, `master_locations`, `task_expense_histories`, `laporan_pekerjaan`, `perangkat_terpasang`, `perangkat_rusak`, `laporan_pekerjaan_foto`, `master_equipment_options`
- **Phase 2:** 10 Eloquent models with relationships, casts, and soft-deletes
- **Phase 3:** 22 API endpoints — full CRUD + 7 stage transitions with role-gated authorization
- **Phase 4:** Pagu enforcement, optimistic locking, config/budget.php centralized params
- **Phase 5:** 139 tests / 437 assertions — all pass via `php artisan test --parallel`

#### Android (Phase 6)

- 12 Compose screens: Summary, MyTasks, Estimate, Realize, Laporan, Inbox, Assign, Approval, Verification, Crash, Sync, Login
- Room DB entities + DAOs for all new tables
- BudgetRepository + domain models + mappers
- Ktor HttpClient timeout fix (15s connect / 30s request)
- CrashReporter with CrashLogScreen
- Role-based navigation gating

#### Web Dashboard (Gap Closure)

6 items closed in parallel via 3 subagents:
- Transaction CRUD (create, read, update, soft-delete)
- Budget Inbox (SUP), Approval (OWNER), Verification (FM)
- Budget Estimate + Realization forms (FE)
- Laporan Pekerjaan form (FE)
- Master Locations + Equipment Options CRUD

#### Server Debug

PHP-FPM permission cascade fix:
- `config/` directory lacked execute bit → PHP-FPM couldn't traverse
- `bootstrap/cache/` not writable by www-data → config cache failures
- `storage/framework/views/` not writable → compiled view errors
- Fix: `chmod 755` dirs, `775` cache/storage, `644` PHP files

### Skills Used

`plan` `test-driven-development` `requesting-code-review` `simplify-code` `spike` `project-documentation` `android-apk-build` `android-debugging` `obsidian` `codebase-inspection`

### Subagents Deployed

5 parallel workers:
1. Code reviewer: 22 issues found → 17 fixed (5 noted for later)
2. Supervisor Inbox + Assign Task screens (Android)
3. Approval + Verification screens (Android)
4. Dashboard Summary + Sync Monitor screens (Android)
5. Budget Estimate + Realization + Equipment CRUD (Web)

---

## Session 2026-06-30 — Post-Gap Fixes + Polish

**Commits:** 12 (0a23e12 → 062a7dc)  
**Focus:** Stabilization and production hardening after the major build.

### Android Fixes

| Issue | Root Cause | Solution |
|-------|-----------|----------|
| Login screen freeze | `DeviceRegistration` HTTP call on UI thread | Move to background coroutine |
| Login retry loop | `SyncManager` starts before auth token | Guard sync behind authenticated session |
| Splash crash | `CrashReporter` init before login completes | Defer init until post-login |
| Dashboard freeze | Room migration v10 column name mismatch | Match SQL column names to Entity camelCase |
| DB version re-trigger | Same version number = migration skipped | Always bump version on schema change |

### Web Polish

| Feature | Detail |
|---------|--------|
| MyTasks page | `/mytasks` for field engineers — task assignments from supervisor |
| Logout fix | Added GET route alongside POST `/logout` |
| IDR formatting | JS formatter: `40000` → `40.000` on blur |
| Hotel auto-calc | `jumlah_hari × tarif_per_hari` — dynamic total |
| Alpine.js fix | `x-data` moved to `<script>` tag — quote escaping solved |
| Blade fixes | `@can` → `@role` directive corrected, password reset syntax fix |

### Build

- `scripts/bump-version.sh` — auto-increment `versionCode` + `versionName`
- APK v2.0.0-b20 (21 MB) — fresh build, wireless ADB deployed

---

## Session 2026-07-01 — Blog & Docs

**Commits:** (c3207db → f0d8d3f)  
**Focus:** GitHub Pages blog, Obsidian vault update, SOUL.md.

- GitHub Pages enabled via API + workflow
- 14-page blog with Jekyll collections
- SOUL.md — Hermes identity document
- Dashboard v2 — technical Obsidian-style rewrite
- All pages rewritten with full technical explanations

---

## Session 2026-07-01 — Hermes Backup

Committed 668 Hermes config files to `mldmustamin/Hermes-Lisa`:
- 18 skill categories (autonomous agents, creative, data-science, mlops, etc.)
- Cron jobs, memories, plugins (ponytail), config.yaml, SOUL.md
- .gitignore for auth tokens, state DB, caches

---

## Cumulative Stats

| Metric | Start | Current |
|--------|-------|---------|
| Commits | 0 | 34+ |
| DB Tables | 22 | 31 |
| API Routes | 0 | 22 |
| Web Pages | 0 | 13 |
| Android Screens | 0 | 13 |
| Tests | 0 | 139 (437 assertions) |
| APK Build | n/a | v2.0.0-b20 |

*Every session logged in ACTION_LOG.md with symptom/diagnosis/fix/verification.*

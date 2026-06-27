# Release Checklist

## Pre-Release (Every Phase)

### Code Quality
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew assembleDebug` succeeds
- [ ] No new linter errors on touched files
- [ ] Cursor rules and docs updated if behavior changed

### Financial Safety
- [ ] `CalculateProjectSummaryUseCaseTest` passes unchanged (or intentional mode-gated change documented)
- [ ] `CsvExportConsistencyTest` passes
- [ ] No Double/Float introduced for money
- [ ] No hard delete on transactions table

### Compatibility
- [ ] Room migration tested (if schema changed)
- [ ] Existing navigation routes work
- [ ] Export PDF/Excel/CSV verified manually
- [ ] Offline create/edit transaction works without network

### Security (Phase 2+)
- [ ] Tokens not logged
- [ ] Session switch isolation verified
- [ ] RBAC enforced server-side (not UI-only)

---

## Android Release Steps

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Run full test suite
3. Build release APK/AAB
4. Test migration on device with production-like data copy
5. Tag git release `android/v{version}`
6. Distribute APK + release notes
7. Monitor crash logs first 48h

---

## Backend Release Steps (Laravel 11 — `backend/` scaffolded)

1. Run test suite from `backend/`: `php vendor/bin/pest` (or `php artisan test`)
2. Run code quality: `php artisan pint --test`, `php artisan telescope:clear`
3. Apply DB migrations: `php artisan migrate` (forward-only, no `--force` until prod confirmation)
4. Deploy to staging (Docker deploy or `git push` + `php artisan optimize`)
5. Restart queue: `php artisan horizon:terminate` (signals graceful restart)
6. Smoke test sync push/pull on staging
7. Deploy to prod: deploy code, run `php artisan migrate --force`
8. Restart prod Horizon: `php artisan horizon:terminate`
9. Tag git release `api/v{version}`
10. Verify mobile app compatibility header

---

## Web Release Steps (Laravel Blade + Livewire — `backend/` scaffolded)

1. No separate build step (Blade compiled on-the-fly by Laravel)
2. Tailwind CSS rebuild: `npx tailwindcss -i ./resources/css/app.css -o ./public/css/app.css --minify`
3. Deploy to staging (same deploy as backend — shared server)
4. E2E approval flow test (Laravel Dusk)
5. Deploy to prod
6. Tag `web/v{version}`

---

## Phase Gate Checklist

| Phase | Gate Criteria |
|-------|---------------|
| 0 | Audit + rules + docs complete |
| 1 | v7 migration + uuid backfill + tests green |
| 2 | Multi-user session + no hardcoded userId=1 |
| 3 | Outbox enqueue + mock sync test |
| 4 | Server auth + project CRUD + assignment + device API (login/logout/me ✅, project CRUD ✅, assignments ✅, device register ✅) |
| 5 | End-to-end push/pull one transaction |
| 6 | Web login + transaction list + approval MVP |
| 7 | Correction/void/closing on web |
| 8 | Full backup/restore roundtrip |
| 9 | Load test + security review |

---

## Rollback Plan

### Android
- Previous APK available for sideload
- Room migrations are forward-only — rollback APK must tolerate newer schema or user must clear data (document risk)

### Server
- Blue/green or previous container image
- DB rollback via snapshot restore (last resort)

---

## Release Notes Template

```
FundManager v{X.Y.Z}

## Added
- ...

## Fixed
- ...

## Migration Notes
- DB v6 → v7: adds uuid columns, no user action required

## Known Issues
- ...
```

---

## Post-Release

- [ ] Update README if user-facing behavior changed
- [ ] Archive audit log of deployment
- [ ] Schedule retrospective for phase gate

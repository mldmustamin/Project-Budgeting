# Deployment, Backup, and Restore

## Android Deployment (Current)

### Build
```bash
./gradlew assembleDebug    # debug APK
./gradlew assembleRelease  # release APK (minify disabled currently)
```

Output: `app/build/outputs/apk/{debug|release}/`

### Requirements
- JDK 17
- Android SDK compileSdk 36
- minSdk 26

### Distribution (Current)
Manual APK sideload. Play Store not configured.

---

## Android Deployment (FMv2)

- Version code/name bump per release
- Room migration tested before rollout
- Staged rollout: internal testers → field team → production
- `X-App-Version` header for API compatibility checks

---

## Backend Deployment (Laravel 11 — `backend/` scaffolded)

- **Codebase:** `backend/` in this repo — deploy from this directory
- **Server:** PHP 8.3+ with FPM, Nginx as reverse proxy
- **Containerization:** Docker with `php:8.3-fpm` base image
- **Process manager:** Laravel Horizon for queues, Supervisord for daemon workers
- **Database:** PostgreSQL managed instance (RDS, Cloud SQL, or DO Managed DB)
- **Cache/Queue:** Redis (ElastiCache or DO Managed Redis)
- **Object storage:** S3-compatible (DO Spaces, MinIO, or AWS S3)
- **Environment separation:** `.env` per environment (dev / staging / prod)
- **Secrets:** Laravel `config/services.php` via env vars or secret manager
- **Artisan commands:** `php artisan migrate:fresh --seed` for new DB, `php artisan horizon:install` for queues
- **Scheduler:** Laravel Scheduler for periodic sync maintenance (cron `* * * * * cd /path && php artisan schedule:run >> /dev/null 2>&1`)

---

## Web Deployment (Laravel Blade + Livewire — `backend/` scaffolded)

- Same deploy as backend — Blade templates compiled on-the-fly by Laravel from `backend/resources/views/`
- No separate web app, no separate build step (except Tailwind CSS: `npx tailwindcss -i ./resources/css/app.css -o ./public/css/app.css --minify`)
- HTTPS required, env vars via Laravel `.env`

---

## Backup Strategy

### Android Local (Phase 8)
- Manual full backup export (JSON + attachment zip)
- Auto-backup via Android Auto Backup currently enabled in manifest (`allowBackup=true`)
- FMv2: consider excluding sensitive tokens from Auto Backup

### Server (Phase 4+)
- PostgreSQL daily automated snapshots
- Point-in-time recovery
- Object storage versioning for attachments
- Retention: 30 daily, 12 monthly (configurable)

---

## Restore Procedures

### Android Full Restore (Planned)
1. User selects backup file
2. Parse JSON, merge by uuid
3. Restore attachment files to internal storage
4. Rebuild indexes
5. Show sync status PENDING for re-push if needed

### Android App Upgrade (Room Migration)
1. User updates APK
2. Room runs MIGRATION_N_N+1 on first launch
3. Verify summary spot-check in release notes

### Server Restore:
1. Restore PG snapshot to staging
2. Validate row counts and sample balances
3. Promote or replay WAL to prod (runbook TBD)

---

## Disaster Recovery

| Scenario | RTO Target | RPO Target |
|----------|------------|------------|
| Single device lost | immediate (new device + login) | last sync |
| Server DB failure | 4 hours | 24 hours (daily backup) |
| Object storage loss | 4 hours | versioning |

---

## Monitoring (Laravel — Planned)

- Laravel Telescope (dev/staging only — never production for privacy)
- Horizon dashboard for queue metrics (production safe)
- Health check endpoint: `GET /health`
- Sync failure alerts (e.g., failed job notification via Horizon)
- DB connection pool metrics via Laravel `DB::`
- Attachment upload error rate
- Mobile crash reporting (optional Firebase)
- Laravel Pulse (optional — real-time server monitoring)

---

## Environment Config

| Env | Android | API | Web |
|-----|---------|-----|-----|
| dev | debug APK | localhost | localhost |
| staging | internal track | staging.api | staging.web |
| prod | release APK | api | app |

Config injection via BuildConfig (Android) and env vars (server/web).

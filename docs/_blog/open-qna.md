---
created: 2026-06-30
updated: 2026-07-01
status: active
tags: [faq, qna, knowledge-base]
---

# Open Q&A — FundManager V2

## A. Produk & Visi (Q1-Q3)

**Q1: Apa itu FundManager V2?**
Budget management platform untuk tim field engineering. Satu APK Android (Kotlin/Compose) + Laravel 11 API + Blade web dashboard. 6 roles, 7-stage budget workflow, offline-first.

**Q2: Kenapa satu APK untuk semua role, bukan app terpisah?**
Satu codebase = satu build pipeline, satu deployment target, lebih mudah maintain. UI di-gate berdasarkan role — field engineer tidak pernah lihat tombol approval, OWNER tidak pernah lihat form realisasi. Role-gating di Compose Navigation + ViewModel.

**Q3: Apa maksud "satu device untuk banyak user"?**
Satu tablet/LCD di site office bisa dipakai bergantian oleh 5 field engineer. Login/logout tidak flush sync outbox. Setiap user punya outbox terisolasi. Session tracking dengan `localUserId + serverUserId + userUuid + deviceId + sessionId`.

## B. Arsitektur & Teknis (Q4-Q10)

**Q4: Stack teknologi lengkap?**
Android: Kotlin 2.0 + Jetpack Compose + Room DB + Hilt + WorkManager + Ktor Client. Backend: Laravel 11 + PHP 8.2 + PostgreSQL 14 + Redis. Web: Blade + Tailwind CSS + Alpine.js + Select2. Auth: Sanctum (API) + Session (Web) + Spatie RBAC.

**Q5: Kenapa Room DB, bukan SQLite langsung atau Realm?**
Room adalah official Android persistence library — compile-time SQL verification, Flow-based reactive queries, migration support, dan integrasi sempurna dengan coroutines. SQLite mentah lebih verbose, Realm sudah deprecated untuk production Android.

**Q6: Bagaimana sync antar device bekerja?**
Outbox pattern: setiap operasi lokal (create/update/submit) bikin entry di `sync_outboxes` dengan idempotency key `{serverUserId}:{deviceId}:{operationId}`. WorkManager periodic (15 menit) push outbox ke server. Server deduplikasi by idempotency key. Pull mengembalikan perubahan baru via UUID, Android upsert ke Room.

**Q7: Kenapa Ktor Client, bukan Retrofit/OkHttp?**
Ktor adalah Kotlin-native HTTP client — coroutine-first, multiplatform-ready, konfigurasi timeout lebih straightforward. Retrofit + OkHttp juga valid, tapi menambah dependency Java yang tidak perlu. Proyek ini pure Kotlin.

**Q8: Apakah Android bisa jalan tanpa internet?**
Ya. Room DB adalah local SSOT. Semua form (estimate, realization, laporan) save ke Room dulu. Outbox menunggu jaringan. Saat online kembali, WorkManager otomatis push + pull. Toast memberi tahu user status sync.

**Q9: Kenapa Hilt, bukan Koin atau manual DI?**
Hilt adalah official Android DI — compile-time dependency graph, integrasi dengan ViewModel + WorkManager, less boilerplate daripada Dagger. Koin lebih ringan tapi tidak punya compile-time safety. Untuk proyek 13 screen + 5 DAO + repository, Hilt lebih tepat.

**Q10: State management di Compose pakai apa?**
Unidirectional Data Flow (UDF): ViewModel expose `StateFlow<UiState>`, Screen collect via `collectAsStateWithLifecycle()`. Event up dari UI ke ViewModel. Tidak ada two-way binding, tidak ada shared mutable state. `CalculateProjectSummaryUseCase` adalah SSOT untuk summary keuangan.

## C. Keuangan (Q11-Q16)

**Q11: Kenapa uang pakai Long, bukan Double/Float?**
Floating-point tidak presisi untuk uang. `0.1 + 0.2 = 0.30000000000000004` di floating-point. Long/integer (dalam satuan terkecil, biasanya Rupiah) selalu eksak. Ini aturan keras — tidak dinegosiasikan di layer manapun (Kotlin, PHP, JavaScript, PostgreSQL).

**Q12: 35 kategori budget — apa saja tipenya?**
- **FIXED_PAGU (10):** VOUCHER, BURUH, BALLAST, FEE, dll. Nilai ditentukan sistem, field engineer tidak bisa ubah.
- **TICKET (12):** AKOMODASI, TRANSPORT, KONSUMSI, dll. Wajib bukti kwitansi. Finance set nominal saat verifikasi kalau tidak ada bukti.
- **MANAGER_APPROVAL (13):** Sewa kendaraan, sewa alat, bahan bakar, dll. OWNER tentukan nominal final.

**Q13: Apa yang terjadi dengan transaksi yang sudah di-approve?**
Immutable. Tidak bisa diedit. Koreksi bikin record baru (`correction` type). Void bikin soft-delete (`deleted_at` timestamp). Tidak ada hard-delete untuk transaksi keuangan. Rejected dan voided transaction dikeluarkan dari active balance.

**Q14: Bagaimana cara closing period?**
FINANCE_MANAGER close accounting period via web dashboard. Saat period closed, semua write endpoint ditolak (stage transition, create, update). Read tetap bisa. Period closing punya audit trail.

**Q15: Apakah ada konversi mata uang?**
Tidak. Semua dalam Rupiah (IDR). Single currency = tidak ada risiko floating exchange rate. Kalau nanti butuh multi-currency, baru pertimbangkan.

**Q16: Apakah formula summary keuangan sama antara Android dan Backend?**
Ya. `CalculateProjectSummaryUseCase` di Android HARUS convergen dengan backend summary query. Tidak boleh ada dua sumber kebenaran untuk perhitungan keuangan. Satu formula, dua platform.

## D. Role & Keamanan (Q17-Q25)

**Q17: Berapa role dan apa bedanya?**
6 role: OWNER (approve budget + set nominal), FINANCE_MANAGER (verifikasi + rekonsiliasi), ADMIN (data reconciliation + master data), SUPERVISOR (forward budget + approve transaksi tim), FIELD_ENGINEER (estimate + realize), AUDITOR (read-only).

**Q18: Kenapa OWNER satu-satunya yang bisa approve budget?**
Budget approval adalah keputusan finansial tertinggi — OWNER yang punya akuntabilitas. ADMIN dan FM membantu verifikasi data, tapi keputusan nominal final ada di OWNER. Ini mencegah conflict of interest (yang verifikasi tidak boleh yang approve).

**Q19: Bagaimana Spatie RBAC diterapkan?**
Laravel Spatie Permission package. Setiap user punya satu role (`hasRole('OWNER')`). Blade: `@role('OWNER')`. Controller: `Gate::authorize()`. Android: role dibaca dari login response, UI di-gate di Navigation.

**Q20: Apakah Sanctum token bisa direvoke?**
Ya. Admin bisa revoke token dari web dashboard. Device authorization dicek setiap request sync. Token scope: device-specific.

**Q21: Apakah ada rate limiting?**
Ya. Laravel throttle middleware: 60 request/menit untuk API umum, lebih ketat untuk auth endpoint. Sync endpoint punya rate limit sendiri untuk mencegah spam.

**Q22: Apakah data attachment publik?**
Tidak. Attachment (kwitansi, foto SCM) disimpan di storage private. Akses melalui signed URL yang expire. Tidak ada public URL untuk file sensitif.

**Q23: Bagaimana audit trail bekerja?**
Setiap stage transition merekam: actor, timestamp, from_stage → to_stage, catatan, dan JSON diff perubahan. `task_expense_histories` table. Server-side `audit_events` untuk semua operasi write. Tidak bisa dihapus.

**Q24: Bagaimana kalau FIELD_ENGINEER pindah proyek?**
Admin reassign project membership via web dashboard. Android cache project assignment di Room. Setiap sync pull menyegarkan assignment. Outbox tetap valid — scoped by user, bukan project.

**Q25: Apakah AUDITOR bisa lihat semua data?**
Ya, read-only. Tidak bisa create, edit, delete, approve, atau stage transition apapun. Akses: semua project, semua transaksi, audit trail, sync logs. Ideal untuk compliance check.

## E. Web Dashboard (Q26-Q30)

**Q26: Kenapa Blade + Livewire, bukan React/Vue SPA?**
Server-authoritative nature proyek ini: semua business logic di Laravel. Blade rendering langsung dari controller — tidak perlu API layer terpisah, tidak perlu state management client-side. Livewire untuk komponen interaktif (inbox real-time, approval modal). SPA akan over-engineer untuk use case ini.

**Q27: Apakah web dashboard bisa diakses dari mobile browser?**
Tidak dioptimalkan untuk mobile. Web dashboard adalah advanced finance control center — dirancang untuk laptop/desktop Finance Manager dan Admin. Field engineer pakai Android app, bukan mobile web.

**Q28: Sync Monitor itu apa?**
Halaman web untuk ADMIN: lihat status sync semua device. Outbox pending, push berhasil/gagal, device terakhir sync. Equivalent: SyncMonitorScreen di Android (ADMIN-only).

**Q29: Apakah ada notifikasi di web dashboard?**
Count badges di navigasi: "Inbox (3)" untuk Supervisor, "Approval (2)" untuk OWNER, "Verification (5)" untuk Admin/FM. Belum ada push notification — direncanakan.

**Q30: Apakah web dashboard support dark mode?**
Belum. Menggunakan Tailwind CSS default theme. Dark mode bisa ditambahkan via `dark:` variant Tailwind tanpa rewrite CSS.

## F. Budget Request (Q31-Q40)

**Q31: Kenapa maksimal 5 draft per user?**
Mencegah spam form kosong. BUDGET_MAX_DRAFTS=5 di config/budget.php, bisa di-override via .env. Field engineer harus menyelesaikan draft sebelum bikin baru.

**Q32: Kenapa reject reset ke DRAFT, bukan ke stage sebelumnya?**
Simplifikasi state machine. Kalau reject ke stage sebelumnya, butuh tracking "dari stage mana". Reset ke DRAFT = clean slate, FE merevisi dari awal dengan rejection_reason sebagai panduan.

**Q33: Kenapa hanya 2 stage yang bisa di-reject?**
ESTIMASI dan FORWARDED. Setelah APPROVED, budget sudah final — tidak bisa dibatalkan begitu saja. Pakai correction (koreksi nominal) atau void (pembatalan) sebagai gantinya.

**Q34: Apakah bisa skip stage?**
Tidak. Stage transition linear: DRAFT → ESTIMASI → FORWARDED → APPROVED → REALISASI → VERIFIED → RECONCILED. Setiap stage punya aturan sendiri.

**Q35: Hotel auto-calc: bagaimana cara kerjanya?**
Jumlah hari × tarif per hari. Tarif diambil dari config/budget.php. Kalau job_type = INSTALASI dan durasi = 3 hari, hotel = 3 × tarif_instalasi. Kalau tidak ada hotel di kategori, return 0.

**Q36: Apakah bisa ada lebih dari satu Laporan Pekerjaan per budget?**
Tidak. One-to-one: satu task_expense = satu laporan_pekerjaan (opsional). Kalau field work butuh multiple reports, buat task_expense terpisah.

**Q37: Apakah FIXED_PAGU sama untuk semua proyek?**
Ya. FIXED_PAGU defined di config/budget.php dan pagu_job_type_amounts — nilainya sama untuk semua proyek. Kalau ada perbedaan tarif antar proyek, perlu adjust di config.

**Q38: Siapa yang bisa edit expense item setelah di-submit?**
Setelah submit (ESTIMASI): SUPERVISOR bisa edit revised_amount. Setelah forward (FORWARDED): OWNER bisa set approved_amount. Setelah approve: tidak ada yang bisa edit. FIELD_ENGINEER hanya bisa edit saat DRAFT.

**Q39: Apakah ada validasi duplikat budget?**
Belum. OWNER lihat location history saat approval untuk deteksi manual. Validasi otomatis (duplicate location + job_type dalam periode sama) — planned.

**Q40: Bagaimana cara sync tetap idempoten meski network intermittent?**
Idempotency key: `{serverUserId}:{deviceId}:{operationId}`. Server simpan key yang sudah diproses. Kalau retry dengan key sama → server return "already processed" tanpa buat record baru.

## G. Authorization (Q41-Q43)

**Q41: Apakah setiap endpoint punya role check?**
Ya. Semua write endpoint enforce role via `Gate::authorize()`. Read endpoint scope data by role (FE lihat milik sendiri, SUP lihat tim, OWNER/ADMIN lihat semua). Tidak ada endpoint yang mengabaikan authorization.

**Q42: Bagaimana race condition dicegah?**
Optimistic locking: `lock_version` integer column. Sebelum save, cek version di DB sama dengan version di request. Kalau beda → conflict response. Client harus re-fetch data dan submit ulang.

**Q43: Apakah notes bisa dioverwrite?**
Tidak. Notes bersifat append-only via stage transition. Setiap transisi punya field notes sendiri. History bisa dibaca, tapi tidak diedit.

## H. Database & Config (Q44-Q46)

**Q44: Berapa total tabel di database?**
31: 22 existing (users, projects, categories, transactions, devices, sync_outboxes, audit_events, RBAC tables, cache/jobs/sessions) + 10 new (task_expenses, expense_items, task_expense_histories, budget_item_templates, pagu_job_type_amounts, master_locations, laporan_pekerjaan, perangkat_terpasang, perangkat_rusak, laporan_pekerjaan_foto) + 1 pivot (pagu_job_type_amounts).

**Q45: Apa isi config/budget.php?**
Semua hardcoded parameter dalam satu file: max_drafts, pagination, history_limit, rejectable_stages, job_types. Bisa overridden via .env. Tidak ada magic number tersebar di kode.

**Q46: Test apa yang sudah ada?**
139 tests, 437 assertions. Mencakup: CRUD, 7 stage transition, rejection flow, role authorization (6 role × 22 endpoint), pagu enforcement, sync idempotency, validation edge cases. Run: `php artisan test --parallel`.

## I. Development (Q47-Q50)

**Q47: Gimana cara build APK?**
`./gradlew assembleRelease` dari root project. Output: `app/build/outputs/apk/release/`. `scripts/bump-version.sh` auto-increment versionCode + versionName sebelum build.

**Q48: Gimana cara deploy APK ke device?**
Wireless ADB via Termux: `adb connect 192.168.x.x:5555` → `adb install app-release.apk`. Tidak perlu kabel USB.

**Q49: Skill Hermes apa saja yang sering dipakai?**
`plan` (task breakdown), `test-driven-development`, `requesting-code-review`, `simplify-code`, `project-documentation`, `android-apk-build`, `android-debugging`, `obsidian`.

**Q50: Di mana dokumentasi lengkap?**
- Blog: `https://mldmustamin.github.io/Project-Budgeting/`
- Repo: `https://github.com/mldmustamin/Project-Budgeting`
- Obsidian vault: `docs/obsidian/` (11 folders)
- ACTION_LOG: sesi development lengkap
- PRD: product requirements
- SOUL.md: Hermes operating system

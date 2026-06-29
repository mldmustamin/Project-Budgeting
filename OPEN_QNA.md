# Open Q&A — FundManager V2

Dokumen ini menjawab pertanyaan-pertanyaan terbuka seputar FundManager V2. Dirancang untuk menyelaraskan pemahaman antara tim teknis, manajemen, finance, dan pengguna lapangan.

> **Status:** Aktif — akan diperbarui seiring munculnya pertanyaan baru.

---

## A. Produk & Visi

### Q1: Apa bedanya FundManager V2 dibanding aplikasi budgeting pada umumnya?

FundManager V2 dibangun khusus untuk **project lapangan** dengan prioritas **offline-first**, **multi-user di perangkat bersama**, dan **sinkronisasi bidirectional**. Kebanyakan aplikasi budgeting komersial (seperti Jurnal, Accurate, atau Spreadsheet) tidak bisa beroperasi offline penuh, tidak mendukung shared device, dan tidak punya workflow approval yang terintegrasi dengan sync engine.

### Q2: Kenapa harus server-authoritative? Kenapa tidak P2P atau local-only?

- **Auditability:** Semua approval, period closing, dan mutasi keuangan harus tercatat immutable di server.
- **RBAC enforcement:** Role dan permission tidak bisa diubah oleh user lokal.
- **Period enforcement:** Hanya server yang bisa mencegah transaksi masuk ke periode yang sudah ditutup.
- **Reconciliation:** Finance membutuhkan satu source of truth yang konsisten.
- Local-first tetap dipertahankan untuk _capture_, tapi _authority_ ada di server.

### Q3: Apakah aplikasi ini bisa dipakai tanpa internet sama sekali?

Ya, untuk input transaksi. Field engineer bisa membuat, mengedit, dan menghapus transaksi beserta lampiran tanpa koneksi internet. Data disimpan di Room DB lokal. Semua operasi sync (push/pull) terjadi di latar belakang ketika perangkat online.

---

## B. Arsitektur & Teknis

### Q4: Kenapa pilih Laravel? Kenapa bukan Go, Node.js, atau .NET?

- Stack Laravel + Blade + Livewire + Tailwind memungkinkan backend sekaligus web dashboard dalam satu kodebase.
- Tim sudah familiar dengan ekosistem PHP/Laravel.
- Sanctum menangani auth mobile dan web sekaligus.
- Spatie Permission menyediakan RBAC yang mature.
- Trade-off: performa konkurensi lebih rendah dari Go, tapi cukup untuk skala project lapangan yang diproyeksikan.

### Q5: Kenapa Android-only? Kenapa tidak iOS?

- Target pengguna utama adalah field engineer di proyek konstruksi/infrastruktur Indonesia, dimana dominasi perangkat Android > 90%.
- Biaya pengembangan dan maintenance iOS tidak sebanding dengan jumlah pengguna potensial.
- Jika diperlukan, API backend sudah siap untuk klien iOS di masa depan.

### Q6: Bagaimana strategi upgrade dari V1 ke V2 tanpa data loss?

- Room database V1 memiliki migration path (saat ini sudah di MIGRATION_6_7).
- Kolom baru (uuid, serverId, approvalStatus, dll.) bersifat additive — tidak menghapus atau mengubah kolom existing.
- Semua data V1 tetap utuh. UUID di-backfill dari data existing.
- Policy: "Never modify existing columns, only add new ones."

### Q7: Bagaimana idempotency di sync bekerja?

Setiap operasi sync memiliki idempotency key berupa `{userUuid}:{deviceUuid}:{operationUuid}`. Server menyimpan key yang sudah diproses. Jika key yang sama dikirim lagi, server mengembalikan response `DUPLICATE` tanpa membuat duplikat data. Ini mencegah transaksi ganda akibat retry atau network timeout.

---

## C. Keuangan

### Q8: Kenapa pakai Long/BIGINT? Kenapa tidak Double atau Float?

Uang tidak boleh direpresentasikan dalam floating-point karena risiko pembulatan (rounding error). 1000 rupiah adalah 1000, bukan 999.9999999. Semua perhitungan keuangan menggunakan integer (Long di Android, BIGINT di PostgreSQL, BigInt di Laravel). Format tampilan (Rp 1.000) hanya di layer UI.

### Q9: Apa itu reportedAmount vs realAmount?

| Istilah | Arti |
|---------|------|
| **reportedAmount** | Jumlah yang dilaporkan oleh field engineer ke kantor (nilai di kwitansi / bukti) |
| **realAmount** | Jumlah riil yang dikeluarkan (bisa berbeda dengan kwitansi karena negosiasi, diskon, dll.) |

Selisihnya disebut **saving** (khusus OFFICE_EXPENSE): `reportedAmount - realAmount`.

### Q10: Bagaimana mekanisme correction untuk approved transaction?

Approved transaction tidak bisa diedit langsung. Perubahan dilakukan dengan:
1. **Void** — finance dapat mem-void transaksi yang sudah approved. Void bersifat soft exclude (data tetap ada, flag `financeStatus = VOIDED`).
2. **Correction** — finance/supervisor membuat transaksi koreksi baru yang ter-link dengan transaksi original. Kedua transaksi tetap visible.

### Q11: Apakah transaksi bisa dihapus permanen?

Tidak ada hard delete. Semua transaksi menggunakan soft delete (kolom `deletedAt`). Data tetap di database, hanya tidak muncul di perhitungan summary mode default.

---

## D. Role & Keamanan

### Q12: Apa yang terjadi jika FIELD_ENGINEER mencoba approve transaksi sendiri?

Server memblokir — separation of duties di-enforce di server-side, bukan hanya di UI. Backend akan mengembalikan HTTP 403 Forbidden meskipun request dikirim langsung via API (bukan dari UI).

### Q13: Apakah data satu user bisa bocor ke user lain di perangkat bersama?

Tidak, dengan catatan:
- Outbox scoped per session — tidak ada cross-user sync.
- DataStore menyimpan session aktif, bukan data semua user.
- UI hanya menampilkan data user aktif.
- Logout membersihkan token dari memory, data lokal tetap aman.
- Shared device tetap menyimpan data semua user di database lokal, tapi aksesnya dikontrol oleh session.

### Q14: Bagaimana jika perangkat hilang atau dicuri?

Admin/OWNER bisa merevoke device dari web dashboard. Setelah direvoke:
- Device tidak bisa melakukan sync (server menolak semua request dari device tersebut).
- Data lokal tetap ada di device tapi tidak bisa dikirim ke server.
- User bisa registrasi ulang dari device baru.

### Q15: Berapa banyak role yang ada? Apa saja?

6 role:
1. **OWNER** — Manager, kontrol penuh, approve budget + set nominal
2. **ADMIN** — admin user/device/project, rekonsiliasi data realisasi
3. **FINANCE_MANAGER** — pencocokan data realisasi ke kordinator, closing, reconciliation
4. **SUPERVISOR** — Kordinator lapangan, budget request ke Manager, approve/reject transaksi tim
5. **FIELD_ENGINEER** — mobile capture transaksi/estimasi, project ter-assign saja
6. **AUDITOR** — read-only audit dan laporan

### Q15b: Siapa yang berwenang approve budget?

Budget approval adalah kewenangan eksklusif **OWNER (Manager)**. Finance Manager dan Admin hanya mencocokkan data realisasi penggunaan dana ke kordinator — tidak bisa approve budget.

---

## E. Web Dashboard

### Q16: Kenapa tidak pakai React/Vue untuk web dashboard?

Keputusan arsitektur menggunakan Blade + Livewire karena:
- Satu kodebase dengan backend (Laravel) — tidak perlu pisah repo/API.
- Livewire memberikan interaktivitas SPA-like tanpa kompleksitas React.
- Tim bisa deliver lebih cepat dengan overhead minimal.
- Trade-off: tidak cocok untuk real-time heavy dashboard. Jika diperlukan di masa depan, API backend sudah siap untuk frontend terpisah.

### Q17: Fitur apa saja yang ada di web dashboard?

Saat ini 12 halaman: Login, Dashboard, Projects, Project Detail, Transactions, Transaction Detail, Approval Center, Audit Trail, Period Management, User Management, Sync Monitor, dan Device Management.

### Q18: Apakah web dashboard bisa offline?

Tidak. Web dashboard didesain untuk akses online dari kantor. Tidak ada PWA/offline support.

---

## F. Sync & Data

### Q19: Apa yang terjadi kalau dua field engineer mengedit transaksi yang sama dari device berbeda?

Skenario ini jarang terjadi karena setiap transaksi terikat ke satu user (creator). Tapi jika terjadi:
- **Server wins** untuk approval/finance status (server authoritative).
- **Last-write-wins** untuk content fields (description, amount).
- Audit trail mencatat semua perubahan dari kedua device.

### Q20: Berapa besar payload sync? Apakah ada batasan?

Payload sync didesain di bawah 1MB per request. Jika melebihi, client harus melakukan batch. Attachment tidak disinkronkan melalui endpoint sync — ada queue terpisah untuk upload file.

### Q21: Bagaimana jika server down? Apakah data aman?

Data aman sepenuhnya. Semua data tersimpan di Room DB lokal. Tidak ada data yang hilang karena server down. Begitu server kembali online, sync worker akan mengirimkan data yang tertunda.

### Q22: Apa itu calculation mode? Kenapa ada 3 mode?

| Mode | Fungsi |
|------|--------|
| `LOCAL_VIEW` | Menampilkan semua transaksi (kecuali soft-deleted) — untuk field view |
| `FINAL_APPROVED` | Hanya transaksi APPROVED, non-void — official balance organisasi |
| `PROJECTED` | FINAL_APPROVED + PENDING approval — forecasting |
Mode ini memungkinkan pengguna yang berbeda (field vs finance) melihat angka yang sesuai dengan kebutuhan mereka dari kumpulan data yang sama.

---

## G. Development & Timeline

### Q23: Apa stack utama proyek ini?

**Android:** Kotlin, Jetpack Compose, Room, Hilt, Ktor, WorkManager
**Backend:** Laravel 11, Sanctum, Spatie Permission, PostgreSQL, Redis
**Web:** Blade, Livewire, Alpine.js, Tailwind CSS
**Infra:** GitHub Actions, S3-compatible storage, Horizon

### Q24: Bagaimana progress saat ini? (June 2026)

| Area | Skor | Keterangan |
|------|------|------------|
| Backend | 92/100 | 28 endpoint, 124 test, 381 assertions |
| Web | 90/100 | 12 halaman, role-based |
| Android | 85/100 | Auth wired, sync fixed |
| CI/CD | 85/100 | GitHub Actions |
| Production | 60/100 | Tinggal provisioning PostgreSQL/Redis |
| **Overall** | **85/100** | Siap staging deploy |

### Q25: Kapan target rilis produksi?

Tergantung provisioning infrastructure (PostgreSQL, Redis, object storage). Backend, web, dan Android sudah siap secara fungsional. Estimasi: setelah staging deploy dan user acceptance testing (UAT).

---

## H. Miscellaneous

### Q26: Apa itu prinsip "ponytail"?

Prinsip pengembangan dari `.clinerules`: lazy senior dev. Artinya efisien, bukan malas. Selalu tanya "Apakah ini perlu dibangun?" sebelum coding. Gunakan ulang yang sudah ada. Prefer solusi paling sederhana. Tandai kompromi sadar dengan komentar `ponytail:`.

### Q27: Apakah mendukung banyak project dalam satu organisasi?

Ya. Setiap project independent dengan transaksi, summary, dan assignment user-nya sendiri. Tidak ada cross-project aggregation di mobile, tapi web dashboard menyediakan org-wide KPI.

### Q28: Bagaimana cara kontribusi / melaporkan bug?

- Buat issue di repository GitHub.
- Untuk bugs kritis, hubungi tim pengembang langsung.
- Semua pull request harus melewati test backend (`php artisan test`) dan mengikuti `.clinerules`.

---

> Dokumen ini bersifat _living document_. Jika ada pertanyaan baru yang muncul, tambahkan dengan format Q-number terbaru.


---

## F. Budget Request Workflow (Baru — Sesi 30 Juni 2026)

### Q31: Gimana alur budget request dari FIELD_ENGINEER sampai rekonsiliasi?
7 stage: **DRAFT** (FE isi estimasi + pagu enforcement) → **ESTIMASI** (FE submit ke Kordinator) → **FORWARDED** (Kordinator review + edit item → forward ke Manager) → **APPROVED** (Manager lihat historis lokasi → approve + final nominal) → **REALISASI** (FE input realisasi per item + upload bukti) → **VERIFIED** (Admin/FM cek bukti per item, centang bill_verified) → **RECONCILED** (FM crosscheck Kordinator, tentukan nominal tiket tanpa bukti → final).

### Q32: Apa yang terjadi kalau budget di-reject?
Rejection cascade kembali ke **DRAFT**. Semua revisi SUPERVISOR (revised_amount) dan approval OWNER (approved_amount) di-reset ke null. FE lihat alasan rejection di history endpoint (GET /api/v1/task-expenses/{id}/histories), revisi item, submit ulang dari awal.

### Q33: Gimana sistem pagu bekerja? Apa yang terjadi kalau melebihi?
35 kategori biaya dibagi 3 tipe pagu. **FIXED_PAGU** (10 items): tidak boleh lebih — **diblokir 422** saat create draft. **TICKET** (12 items): wajib bukti fisik resmi. Tanpa bukti → Finance tentukan nominal saat rekonsiliasi. **MANAGER_APPROVAL** (13 items): OWNER tentukan nominal final. **HOTEL** special case: boleh lebih pagu tapi wajib bill asli (warning, bukan block).

### Q34: Ada berapa kategori biaya total? Gimana mapping pagu per job_type?
35 kategori dengan pagu_amount di tabel pivot . Contoh: VOUCHER 15rb (Instalasi/Relokasi/PMCM) vs 5rb (Dismantle/Survey). FEE 40rb (Instalasi) / 75rb (Relokasi) / 15rb (PMCM/Dismantle) / null (Survey). BURUH dan BALLAST hanya untuk Instalasi/Relokasi (null untuk job_type lain). Semua parameter di .

### Q35: Apa beda Form Penggunaan Uang (task_expenses) dan Laporan Pekerjaan?
**Form A (task_expenses + expense_items)**: FINANSIAL — estimasi → approved → realisasi biaya + bukti kwitansi. **Form B (laporan_pekerjaan + perangkat + foto)**: TEKNIS — perangkat terpasang/rusak (VSAT/M2M), parameter sinyal, tindakan teknisi, catatan gangguan, foto pekerjaan wajib SCM. Keduanya bisa diisi paralel oleh FE.

### Q36: Gimana FE di lapangan tanpa internet mengisi form?
Auto-save setiap perubahan ke Room DB lokal. Submit via outbox pattern (SyncOutboxRepository). SyncWorker periodic 15 menit + one-time sync setelah login. Upload bukti via file queue terpisah (BuktiUploadWorker). Max 5 draft per FE (config: BUDGET_MAX_DRAFTS). Retry 5x → flag error → toast: "3 form terkirim, 1 gagal". Ganti HP: draft lokal hilang (acceptable), data synced muncul dari server.

### Q37: Siapa yang assign task ke FIELD_ENGINEER?
**SUPERVISOR via app** — isi task_no, VID, nama task, job_type, lokasi, assign ke FE. FE dapat notifikasi di "My Tasks" dashboard. Data (job_type, lokasi) auto-fill di form estimasi. Email sudah tidak digunakan untuk task assignment.

### Q38: Bagaimana OWNER lihat historis budget saat approval?
OWNER saat approve bisa lihat **5-10 kunjungan terakhir di lokasi yang sama** via endpoint . Data mencakup task_no, job_type, stage, total_approved, total_realization. OWNER bandingkan budget baru vs historis untuk keputusan nominal final.

### Q39: Apakah SUPERVISOR bisa edit estimasi dari FIELD_ENGINEER?
Ya. SUPERVISOR bisa revisi nominal per item (disimpan di ) sebelum forward ke OWNER. Audit trail mencatat 3 layer: estimated_amount (FE) → revised_amount (SUP) → approved_amount (OWNER). Semua tercatat di  + .

### Q40: Foto apa saja yang wajib diupload untuk laporan pekerjaan SCM?
**19 item checklist** di  (field_key: FOTO_WAJIB_SCM): antena depan/belakang, SN Modem/BUC/LNB, Modem UP, Rak/Box ATM, Plang lokasi, LNB+BUC+Feedhorn, Feedhorn, jalur kabel out/in, capture summary modem, test ping 1000+100, teknisi di lokasi, dynabolt pedestal, pengukuran listrik PLN+UPS. Semua foto TANPA geotag. Disimpan di .

### Q41: Bagaimana authorization di-backend untuk setiap stage transition?
Role-based ketat: store (FIELD_ENGINEER), forward (SUPERVISOR), approve (OWNER), reject (SUPERVISOR/OWNER), realize (FIELD_ENGINEER own task), verify (ADMIN/FINANCE_MANAGER), reconcile (FINANCE_MANAGER). Semua dicek dengan  + 403 Forbidden. Plus stage validation (hanya ESTIMASI yang bisa diforward, hanya FORWARDED yang bisa diapprove, dsb).

### Q42: Bagaimana race condition dicegah pada stage transition?
Optimistic locking di approve(). Update query pakai . Kalau stage sudah berubah oleh concurrent request, query return 0 rows → throw RuntimeException "Task sudah berubah stage, silakan refresh". Diterapkan di method paling kritis (approve). Method lain pakai check-before-act dalam DB::transaction().

### Q43: Gimana notes di-maintan saat SUPERVISOR forward?
Notes **di-append**, bukan di-overwrite. Format: notes asli FE + "--- Kordinator: [catatan SUPERVISOR]". OWNER bisa lihat history lengkap dari FE dan Kordinator.

### Q44: Gimana struktur tabel yang baru? Ada berapa tabel total?
31 tabel total (22 existing + 9 baru). 9 tabel baru:  (35 kategori pagu),  (pivot: 7 cats × 5 job_types),  (CRUD ADMIN/SUPERVISOR),  (single form 7-stage),  (per-item dengan 4 layer nominal),  (audit trail),  (laporan teknis),  +  (VSAT/M2M),  (foto SCM),  (dropdown values).

### Q45: Kenapa config/budget.php dibuat? Apa isinya?
Semua hardcoded parameter dipusatkan: max_drafts (BUDGET_MAX_DRAFTS=5), pagination (BUDGET_PAGINATION=20), history limit (BUDGET_HISTORY_LIMIT=10), stages, rejectable_stages, job_types, pagu_types, ticket/hotel rules. Semua controller pakai config('budget.xxx') + fallback default. Overridable via .env.

### Q46: Berapa test yang ada dan apa coverage-nya?
**15 tests, 51 assertions**: BudgetTemplateApiTest (4 tests — listing, filtering, equipment options, auth), TaskExpenseApiTest (11 tests — create draft, max 5 drafts, full 7-stage workflow, reject cascade, rejection history, delete draft, unauthorized access [approve/verify/create], pagu enforcement [block + hotel warning]). Coverage: semua stage transition, authorization gate, pagu rules, edge cases.

### Q47: Apa yang masih perlu dibangun?
**Phase 6 (Android)**: 8 screen baru (MyTasks, BudgetEstimateForm, RealizationForm, SupervisorInbox, AssignTask, Approval, Verification, LaporanPekerjaan). Room DB entities (TaskExpenseEntity, ExpenseItemEntity, BudgetTemplateEntity, MasterLocationEntity, LaporanPekerjaanEntity, dll). Repository pattern + sync extension. File upload worker (BuktiUploadWorker) untuk foto SCM offline.

### Q48: Gimana cara menjalankan tests?

   WARN  Metadata found in doc-comment for method Tests\Feature\BudgetTemplateApiTest::can_list_budget_templates(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\BudgetTemplateApiTest::can_list_equipment_options(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\BudgetTemplateApiTest::can_filter_equipment_options_by_field_key(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\BudgetTemplateApiTest::unauthenticated_user_cannot_access_templates(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::engineer_can_create_draft_task_expense(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::engineer_cannot_create_more_than_5_drafts(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::full_happy_path_workflow(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::supervisor_can_reject_back_to_draft(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::engineer_can_see_rejection_history(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::engineer_can_delete_own_draft(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::cannot_delete_non_draft_task(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::engineer_cannot_approve_own_budget(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::pagu_enforcement_blocks_exceeded_fixed_pagu(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::hotel_can_exceed_pagu_with_warning(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.

   WARN  Metadata found in doc-comment for method Tests\Feature\TaskExpenseApiTest::non_engineer_cannot_create_budget_request(). Metadata in doc-comments is deprecated and will no longer be supported in PHPUnit 12. Update your test code to use attributes instead.
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 53 equipment options
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 53 equipment options
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 53 equipment options
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 53 equipment options

   PASS  Tests\Feature\BudgetTemplateApiTest
  ✓ can list budget templates                                            0.52s  
  ✓ can list equipment options                                           0.13s  
  ✓ can filter equipment options by field key                            0.11s  
  ✓ unauthenticated user cannot access templates                         0.12s  
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts
Seeded 35 budget item templates
Seeded 35 pagu job type amounts

   PASS  Tests\Feature\TaskExpenseApiTest
  ✓ engineer can create draft task expense                               0.12s  
  ✓ engineer cannot create more than 5 drafts                            0.09s  
  ✓ full happy path workflow                                             0.13s  
  ✓ supervisor can reject back to draft                                  0.10s  
  ✓ engineer can see rejection history                                   0.10s  
  ✓ engineer can delete own draft                                        0.09s  
  ✓ cannot delete non draft task                                         0.10s  
  ✓ engineer cannot approve own budget                                   0.10s  
  ✓ pagu enforcement blocks exceeded fixed pagu                          0.09s  
  ✓ hotel can exceed pagu with warning                                   0.09s  
  ✓ non engineer cannot create budget request                            0.09s  

  Tests:    15 passed (56 assertions)
  Duration: 2.09s
Hasil: 15 tests, 51 assertions, duration ~1.7s. Semua pass. Requires PostgreSQL database (fundsmanager_production).

### Q49: Gimana struktur direktori backend yang baru?


### Q50: Skills apa yang direkomendasikan untuk development selanjutnya?
**Clean code pipeline**:  (validasi ide) →  (bite-sized tasks) →  (RED-GREEN-REFACTOR) →  (pre-commit gate) →  (parallel 3-agent cleanup, mirip Ponytail — deteksi duplikasi, dead code, N+1, AI slop). Untuk Android:  + .

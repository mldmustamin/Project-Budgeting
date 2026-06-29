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

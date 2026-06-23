# Funds Manager — Android Project Plan

**Nama aplikasi:** Funds Manager  
**Nama file:** `Funds-Manafer.md`  
**Platform:** Android  
**Basis konsep:** Android version dari `tracker-duit` dengan improvement logic.  
**Fokus utama:** pencatatan dana project/perjalanan dinas, bukan e-wallet, payment gateway, investasi, atau aplikasi akuntansi penuh.

---

## 1. Ringkasan Produk

Funds Manager adalah aplikasi Android untuk mencatat dan mengontrol penggunaan dana operasional per project/perjalanan. Aplikasi ini mengambil pola utama dari `tracker-duit`: project-based budget tracking, dana masuk, pengeluaran kantor, pengeluaran pribadi, nominal real vs nominal dilaporkan, multi-user/admin, backup/restore, dan export laporan.

Versi Android harus lebih kuat di logic, lebih cepat untuk input lapangan, dan tetap bisa dipakai offline.

---

## 2. Masalah yang Diselesaikan

1. Catatan pengeluaran lapangan tercecer di chat, nota, foto, dan ingatan.
2. Sulit membedakan pengeluaran kantor dan pengeluaran pribadi.
3. Sulit melihat sisa dana real.
4. Sulit membuat laporan reimbursement atau SPD yang rapi.
5. Sulit membandingkan nominal aktual dengan nominal yang dilaporkan.
6. Bukti transaksi sering tidak terhubung ke item pengeluaran.
7. Tidak ada audit trail saat data diedit/dihapus.
8. Kategori dan aturan biaya berbeda-beda per project.

---

## 3. Baseline dari `tracker-duit`

`tracker-duit` saat ini adalah aplikasi web Budget Tracking berbasis Node.js, Express, SQLite WASM, Vanilla JS, dan Bootstrap.

Fitur baseline yang perlu dipertahankan:

1. Multi-user dan role management.
2. Admin dashboard.
3. Project/perjalanan dinas.
4. Pemasukan dana/top-up/reimburse.
5. Office expenses.
6. Personal expenses.
7. Backup dan restore JSON.
8. Export CSV.
9. Registration lock.
10. Dynamic expense categories.
11. Project archive.
12. User hanya mengakses project miliknya.

Model data baseline:

```text
users
settings
projects
fund_entries
office_expenses
personal_expenses
```

Logic penting yang wajib dipertahankan:

```text
office_expense.amount      = nominal dilaporkan
office_expense.real_amount = nominal aktual/real
selisih                    = amount - real_amount
```

---

## 4. Arah Improvement Logic

### 4.1 Source Menjadi Account

Di versi web, `source` masih berupa teks. Di Android, source sebaiknya dinaikkan menjadi entity `Account`.

Contoh account:

- Cash in Hand.
- Bank.
- Dana Kantor.
- Dana Pribadi.
- Reimburse.
- E-Wallet.
- Lainnya.

Manfaat:

- Bisa hitung saldo per sumber dana.
- Mengurangi typo.
- Filter transaksi lebih rapi.
- Siap untuk sync backend.

### 4.2 Unified Transaction Layer

Walaupun data bisa tetap dipisah menjadi fund, office expense, dan personal expense, domain logic sebaiknya memakai satu view transaksi.

Tipe transaksi:

```text
FUND_IN
OFFICE_EXPENSE
PERSONAL_EXPENSE
REIMBURSEMENT
TRANSFER
ADJUSTMENT
```

Manfaat:

- Summary lebih konsisten.
- Export lebih mudah.
- Filter/search lebih mudah.
- Audit lebih mudah.

### 4.3 Nominal Real vs Nominal Dilaporkan

Untuk `OFFICE_EXPENSE`, field wajib:

```text
reported_amount
real_amount
saving_amount = reported_amount - real_amount
```

Interpretasi:

- `saving_amount > 0` = ada selisih hemat.
- `saving_amount = 0` = real sama dengan laporan.
- `saving_amount < 0` = real lebih besar dari nominal laporan.

### 4.4 Configurable Allowance

Di versi web, allowance masih berupa konstanta. Di Android, pindahkan ke setting/project rule.

Contoh:

```text
daily_allowance = 120000
lodging_allowance = 200000
return_ticket_estimate = manual
lodging_nights = manual/auto
```

Manfaat:

- Beda project bisa beda aturan.
- Tidak perlu edit kode saat kebijakan berubah.
- Lebih aman untuk laporan SPD/perjalanan.

### 4.5 Project Date Logic

Project punya:

```text
start_date
end_date optional
return_date optional
status: active / archived / closed
```

Validasi:

1. Tanggal transaksi wajib.
2. Transaksi sebelum start date diberi warning.
3. Transaksi setelah close date diberi warning.
4. Penginapan dihitung berdasarkan jumlah malam, bukan sekadar jumlah hari.
5. Tiket pulang tidak otomatis berarti tambah penginapan. User harus input penginapan secara eksplisit.

### 4.6 Duplicate Detection

Rule awal:

- Project sama.
- Tanggal sama.
- Nominal sama.
- Deskripsi mirip.
- Kategori sama.

Output:

- Sistem memberi warning.
- Tidak langsung block.
- User bisa tetap simpan.
- Override dicatat di audit log.

### 4.7 Receipt Attachment

Setiap expense dapat punya bukti transaksi.

Field:

```text
receipt_id
local_path
thumbnail_path
mime_type
file_size
sha256
ocr_text optional
created_at
```

Improvement:

- Ambil foto dari kamera.
- Pilih dari galeri.
- Compress otomatis.
- Simpan thumbnail.
- Deteksi bukti yang sama.
- Optional OCR di fase berikutnya.

### 4.8 Safe Restore

Restore JSON tidak boleh langsung menimpa data.

Mode restore:

1. Preview import.
2. Deteksi duplicate project.
3. Deteksi duplicate transaction.
4. Pilihan: skip, merge, replace.
5. Hasil import menampilkan imported, skipped, failed, warnings.

### 4.9 Audit Log dan Soft Delete

Data penting tidak langsung hard delete.

Setiap entity penting punya:

```text
created_at
updated_at
deleted_at
created_by
updated_by
version
```

Audit log menyimpan:

```text
entity_type
entity_id
action
old_value_json
new_value_json
timestamp
user_id
```

---

## 5. Scope MVP

### 5.1 Masuk MVP

1. Setup user pertama / login lokal.
2. Buat, edit, arsipkan project.
3. Tambah dana masuk.
4. Tambah office expense.
5. Tambah personal expense.
6. Nominal dilaporkan vs nominal real.
7. Account/source dana.
8. Category management sederhana.
9. Dashboard project.
10. Rekap semua project.
11. Search dan filter transaksi.
12. Foto bukti transaksi.
13. Export CSV.
14. Backup JSON.
15. Restore JSON dengan preview.
16. Duplicate warning.
17. Missing receipt warning.
18. Soft delete.
19. Audit log dasar.

### 5.2 Tidak Masuk MVP

1. Payment gateway.
2. Integrasi bank otomatis.
3. E-wallet real-time.
4. OCR wajib.
5. Multi-company accounting.
6. Pajak lengkap.
7. Double-entry accounting penuh.
8. Cloud sync wajib.
9. AI assistant.
10. Approval workflow kompleks.

---

## 6. Core Calculation

### 6.1 Per Project

```text
total_fund_in = sum(FUND_IN.amount)

total_office_reported = sum(OFFICE_EXPENSE.reported_amount)

total_office_real = sum(OFFICE_EXPENSE.real_amount)

total_personal = sum(PERSONAL_EXPENSE.amount)

saving_amount = total_office_reported - total_office_real

remaining_reported = total_fund_in - total_office_reported

remaining_real = total_fund_in - total_office_real
```

### 6.2 Allowance

```text
project_days = date_diff(today_or_end_date, start_date) + 1

daily_allowance_budget = daily_allowance * project_days

daily_allowance_used = sum(expense where category = Expense Perjalanan)

daily_allowance_remaining = daily_allowance_budget - daily_allowance_used

lodging_budget = lodging_allowance * lodging_nights

lodging_used = sum(expense where category = Akomodasi/Penginapan)

lodging_remaining = lodging_budget - lodging_used
```

### 6.3 Project Status

```text
if archived = true:
    status = ARCHIVED
else if remaining_real < 0:
    status = OVERSPENT
else if missing_receipt_count > 0:
    status = NEED_RECEIPT
else:
    status = ACTIVE
```

---

## 7. Android Technical Stack

Rekomendasi stack:

- Kotlin.
- Jetpack Compose.
- Material 3.
- Room Database.
- DataStore.
- WorkManager.
- Navigation Compose.
- ViewModel + StateFlow.
- Coroutines.
- Hilt atau Koin.
- CameraX.
- Coil.
- Kotlinx Serialization.
- SQLCipher optional untuk database terenkripsi.

Prinsip arsitektur:

```text
UI Layer          = Compose Screen + ViewModel
Domain Layer      = Use Case + Business Logic
Data Layer        = Repository + DAO + Local Database
Core Layer        = Backup, Export, Security, Utility
Sync Layer        = optional, fase lanjutan
```

---

## 8. Data Model Android

### 8.1 UserEntity

```kotlin
UserEntity(
    id: String,
    username: String,
    passwordHash: String?,
    isAdmin: Boolean,
    createdAt: Instant,
    updatedAt: Instant
)
```

### 8.2 ProjectEntity

```kotlin
ProjectEntity(
    id: String,
    userId: String,
    name: String,
    startDate: LocalDate,
    endDate: LocalDate?,
    returnDate: LocalDate?,
    returnTicketEstimate: Long,
    status: ProjectStatus,
    isArchived: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?
)
```

### 8.3 AccountEntity

```kotlin
AccountEntity(
    id: String,
    userId: String,
    name: String,
    type: AccountType,
    openingBalance: Long,
    isDefault: Boolean,
    createdAt: Instant,
    updatedAt: Instant
)
```

### 8.4 CategoryEntity

```kotlin
CategoryEntity(
    id: String,
    userId: String,
    name: String,
    type: CategoryType,
    isSystem: Boolean,
    sortOrder: Int,
    createdAt: Instant,
    updatedAt: Instant
)
```

### 8.5 TransactionEntity

```kotlin
TransactionEntity(
    id: String,
    projectId: String,
    userId: String,
    accountId: String,
    type: TransactionType,
    date: LocalDate,
    description: String,
    categoryId: String?,
    sourceText: String?,
    reportedAmount: Long,
    realAmount: Long,
    note: String?,
    receiptId: String?,
    duplicateWarningOverridden: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?
)
```

### 8.6 ReceiptEntity

```kotlin
ReceiptEntity(
    id: String,
    transactionId: String,
    localPath: String,
    thumbnailPath: String?,
    mimeType: String,
    fileSize: Long,
    sha256: String,
    ocrText: String?,
    createdAt: Instant
)
```

### 8.7 AuditLogEntity

```kotlin
AuditLogEntity(
    id: String,
    userId: String,
    entityType: String,
    entityId: String,
    action: AuditAction,
    oldValueJson: String?,
    newValueJson: String?,
    createdAt: Instant
)
```

---

## 9. Screen Plan

### 9.1 Login / Setup

- Setup user pertama.
- Login lokal.
- Optional PIN/biometric.
- Lock app setelah idle.

### 9.2 Project List

- List active project.
- Toggle show archived.
- Total dana masuk.
- Sisa dana real.
- Status warning.
- Tombol tambah project.

### 9.3 Project Dashboard

Widget:

- Total dana masuk.
- Office reported.
- Office real.
- Saving/selisih.
- Sisa reported.
- Sisa real.
- Personal expense.
- Missing receipt.
- Allowance harian.
- Penginapan.

### 9.4 Add Fund

Field:

- Tanggal.
- Account/source.
- Nominal.
- Note.

### 9.5 Add Office Expense

Field:

- Tanggal.
- Keterangan.
- Kategori.
- Account/source.
- Nominal dilaporkan.
- Nominal real.
- Bukti transaksi.
- Note.

### 9.6 Add Personal Expense

Field:

- Tanggal.
- Keterangan.
- Account/source.
- Nominal.
- Bukti transaksi optional.
- Note.

### 9.7 Transaction List

Fitur:

- Search.
- Filter tipe.
- Filter kategori.
- Filter tanggal.
- Filter account.
- Filter missing receipt.
- Edit.
- Soft delete.

### 9.8 Report & Export

Output:

- Summary project.
- CSV.
- JSON backup.
- PDF fase 2.
- Excel fase 2.

---

## 10. Backup Compatibility dengan `tracker-duit`

Funds Manager harus bisa import backup JSON dari `tracker-duit`.

Mapping:

```text
fundEntries       -> TransactionEntity(type = FUND_IN)
officeExpenses    -> TransactionEntity(type = OFFICE_EXPENSE)
personalExpenses  -> TransactionEntity(type = PERSONAL_EXPENSE)
amount            -> reportedAmount
realAmount         -> realAmount
source            -> AccountEntity or sourceText
category          -> CategoryEntity
returnTicketEstimate -> ProjectEntity.returnTicketEstimate
```

Restore wajib memakai preview agar data tidak double.

---

## 11. Export Format

### 11.1 CSV MVP

Kolom:

```text
Project
Tanggal
Tipe
Kategori
Keterangan
Account/Sumber
Nominal Dilaporkan
Nominal Real
Selisih
Bukti
Catatan
```

### 11.2 PDF Fase 2

Isi:

- Header project.
- Summary dana.
- Tabel transaksi.
- Warning data belum lengkap.
- Lampiran bukti transaksi.

### 11.3 Excel Fase 2

Sheet:

1. Summary.
2. Dana Masuk.
3. Expense Kantor.
4. Expense Pribadi.
5. Selisih Real vs Dilaporkan.
6. Missing Receipt.
7. Audit Log optional.

---

## 12. Backend Strategy

### 12.1 MVP Tanpa Backend

Rekomendasi awal: Android offline-first tanpa backend.

Kelebihan:

- Cepat dibangun.
- Bisa dipakai di lapangan.
- Tidak tergantung VPS.
- Tidak ada sync conflict.
- Backup JSON cukup untuk fase awal.

### 12.2 Fase Lanjutan dengan Backend

Jika perlu multi-device/multi-user lintas perangkat:

Opsi A: reuse backend `tracker-duit`.

- Tambahkan API mobile.
- Ubah session auth menjadi token/JWT.
- Tambahkan endpoint sync.
- Tambahkan conflict handling.

Opsi B: backend baru.

- Node.js/FastAPI/Ktor.
- PostgreSQL atau SQLite server.
- JWT auth.
- Sync queue.
- Audit log server-side.

Rekomendasi: mulai offline-first, backend belakangan.

---

## 13. Development Phases

### Phase 0 — Scope Lock

Output:

- Confirm domain project/perjalanan dinas.
- Confirm app name Funds Manager.
- Confirm compatibility dengan backup `tracker-duit`.
- Confirm no payment/bank integration in MVP.

### Phase 1 — Android Foundation

Output:

- Kotlin project.
- Compose theme.
- Navigation.
- Room database.
- DataStore.
- Dependency injection.
- Local setup/login.

### Phase 2 — Core Transaction

Output:

- CRUD project.
- CRUD account.
- CRUD category.
- Add fund.
- Add office expense.
- Add personal expense.
- Summary calculation.
- Archive project.
- Soft delete.

### Phase 3 — Receipt & Report

Output:

- Camera/gallery receipt.
- Receipt compression.
- CSV export.
- JSON backup.
- JSON restore preview.
- Share report.

### Phase 4 — Logic Hardening

Output:

- Duplicate detection.
- Missing receipt detection.
- Date validation.
- Allowance rule.
- Audit log.
- Unit test formula.
- Import compatibility test.

### Phase 5 — Optional Sync

Output:

- Sync queue.
- Backend API.
- Conflict handling.
- Admin dashboard compatibility.

---

## 14. Acceptance Criteria MVP

MVP dianggap selesai jika:

1. User bisa setup dan login.
2. User bisa membuat project.
3. User bisa mencatat dana masuk.
4. User bisa mencatat office expense.
5. User bisa mencatat personal expense.
6. User bisa membedakan nominal dilaporkan dan nominal real.
7. Summary project akurat.
8. User bisa attach bukti transaksi.
9. User bisa export CSV.
10. User bisa backup JSON.
11. User bisa restore JSON dengan preview.
12. Duplicate warning berjalan.
13. Missing receipt warning berjalan.
14. Project bisa diarsipkan.
15. Data penting memakai soft delete.
16. Ada unit test untuk formula utama.

---

## 15. QNA Logic Self-Check

### 15.1 Core Calculation

1. Jika dana masuk Rp1.000.000 dan office reported Rp700.000, apakah remaining reported Rp300.000?
2. Jika office real Rp600.000, apakah remaining real Rp400.000?
3. Jika reported Rp700.000 dan real Rp600.000, apakah saving Rp100.000?
4. Apakah personal expense tidak masuk total office expense?
5. Apakah transaksi soft-deleted tidak masuk summary?
6. Apakah archived project tetap bisa dilihat jika toggle archived aktif?

### 15.2 Data Integrity

1. Apakah amount 0 ditolak?
2. Apakah tanggal kosong ditolak?
3. Apakah expense tanpa deskripsi ditolak?
4. Apakah office expense tanpa kategori ditolak?
5. Apakah real amount otomatis sama dengan reported amount jika kosong?
6. Apakah duplicate warning muncul untuk transaksi mirip?
7. Apakah user bisa override duplicate warning?
8. Apakah override tercatat di audit log?

### 15.3 Backup/Restore

1. Apakah backup menghasilkan schema version?
2. Apakah restore menampilkan preview sebelum import?
3. Apakah project duplicate tidak masuk dua kali?
4. Apakah transaksi duplicate tidak masuk dua kali?
5. Apakah backup dari `tracker-duit` bisa dimapping?
6. Apakah `realAmount` dari `tracker-duit` tetap terbaca?
7. Apakah `source` lama tetap disimpan walaupun belum cocok ke account?

### 15.4 Report

1. Apakah jumlah item CSV sama dengan transaksi aktif?
2. Apakah missing receipt muncul di report?
3. Apakah reported amount dan real amount tampil terpisah?
4. Apakah selisih real vs reported dihitung benar?
5. Apakah report tidak memasukkan transaksi deleted?
6. Apakah report bisa dishare dari Android?

---

## 16. Codex Implementation Prompt

```text
Build an Android app named Funds Manager based on the tracker-duit domain model.

The app must be offline-first and use Kotlin, Jetpack Compose, Room, DataStore, ViewModel, StateFlow, Coroutines, and Clean Architecture.

Core domain:
- Project
- Fund entries
- Office expenses
- Personal expenses
- Accounts
- Categories
- Receipts
- Audit logs
- Backup/restore

Important logic:
- Office expense has reportedAmount and realAmount.
- savingAmount = reportedAmount - realAmount.
- remainingReported = totalFundIn - totalOfficeReported.
- remainingReal = totalFundIn - totalOfficeReal.
- Personal expense must not be mixed with office expense.
- Soft delete must be used for important entities.
- Export must not include soft-deleted rows.
- JSON restore must preview and avoid duplicates.
- App must be able to import tracker-duit style backup JSON.

Do not build payment gateway, banking integration, investment features, or full accounting system in MVP.
```

---

## 17. Technical Risks

| Risk | Dampak | Mitigasi |
|---|---:|---|
| Summary dana salah | Sangat tinggi | Unit test formula |
| Restore menimpa data | Tinggi | Preview + merge strategy |
| Foto bukti memenuhi storage | Sedang | Compress + thumbnail |
| Duplicate transaksi | Sedang | Duplicate detection |
| Kategori berubah | Sedang | Category table dinamis |
| Sync conflict | Tinggi | Tunda sync ke fase lanjutan |
| Hard delete tidak sengaja | Tinggi | Soft delete + audit log |
| Scope terlalu melebar | Tinggi | MVP dibatasi ke project fund tracker |

---

## 18. Roadmap Setelah MVP

1. PDF report resmi.
2. Excel export.
3. OCR nota.
4. Cloud sync.
5. Admin web dashboard.
6. Approval flow.
7. Multi-device.
8. Role user/admin lebih lengkap.
9. Budget template per jenis pekerjaan.
10. Auto suggestion kategori berdasarkan deskripsi.
11. Peringatan spending pattern.
12. Dashboard bulanan.
13. Backup Google Drive.
14. Import CSV format lama.
15. Widget Android untuk quick input.

---

## 19. Keputusan Arsitektur

Rekomendasi awal:

1. Bangun Android offline-first terlebih dahulu.
2. Jangan langsung membangun cloud sync.
3. Jangan membawa semua fitur web secara mentah.
4. Pertahankan logic terbaik dari `tracker-duit`: project, fund, office expense, personal expense, reported vs real.
5. Upgrade `source` menjadi `account`.
6. Upgrade export/backup agar aman dan tidak duplicate.
7. Tambahkan audit log sejak awal.
8. Gunakan unified transaction view agar report lebih mudah.

---

## 20. Catatan Akhir

Funds Manager sebaiknya tidak diposisikan sebagai aplikasi keuangan umum yang terlalu luas. Kekuatan utamanya adalah **project-based fund tracking** untuk kebutuhan lapangan: dana masuk, expense kantor, expense pribadi, bukti transaksi, nominal real vs nominal dilaporkan, dan laporan yang rapi.

Versi Android harus lebih rapi dari `tracker-duit`, tetapi tetap sederhana. Improvement logic difokuskan pada account/source yang lebih terstruktur, unified transaction layer, configurable allowance, duplicate detection, receipt attachment, safe restore, audit log, dan report consistency check.

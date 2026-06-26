# LOGICFIX.md
## Funds Manager Android — Master Logic Gate Document

> **Cara pakai:** Letakkan file ini di root project. Baca sebelum mengerjakan task apapun.
> Agent wajib membaca file ini sebelum mulai, dan wajib menjalankan semua gate relevan sebelum menyatakan task selesai.
> Task dianggap selesai **hanya jika** semua gate relevan PASS dengan bukti nyata.

---

## 0. PROJECT IDENTITY GUARD

**Funds Manager Android adalah:**
- Aplikasi offline-first pencatatan dana project / perjalanan dinas
- Pencatatan dana masuk, expense kantor, expense pribadi
- Project summary, transaksi, import tracker-duit
- Export laporan PDF/Excel, backup/restore

**Funds Manager Android BUKAN:**
- e-wallet, payment app, banking app
- investment app, cloud accounting app
- aplikasi dengan AI, OCR, server sync, cloud storage

**Larangan mutlak tanpa perintah eksplisit:**
```
DILARANG TAMBAH: cloud sync, OCR, payment gateway, AI feature,
banking API, server sync, push notification, atau fitur di luar daftar di atas.
```

---

## 1. PRIORITY GATE LADDER

Urutan gate wajib dijalankan dari atas ke bawah. Jangan lanjut ke gate berikutnya jika gate sebelumnya FAIL.

```
GATE 1 — Runtime Wiring Gate       ← paling sering diabaikan agent
GATE 2 — Data Integrity Gate        ← soft delete, Long, no Float
GATE 3 — Financial Logic Gate       ← ProjectSummary sebagai SSOT
GATE 4 — UI Usability Gate          ← label Indonesia, no raw enum
GATE 5 — Export/Share Gate          ← PDF/Excel/FileProvider
GATE 6 — Final MVP Gate             ← semua gate PASS
```

---

## GATE 1 — RUNTIME WIRING GATE

**Tujuan:** Memastikan file yang diubah benar-benar dipakai oleh route aktif saat runtime.

**Penyakit umum:**
- Agent membuat file baru tapi tidak mewire ke navigation
- Agent mengubah composable lama tapi navigation masih panggil file berbeda
- Agent mengklaim selesai karena build sukses padahal screen tidak berubah

**Cek wajib sebelum submit:**

```bash
# Temukan route aktif dan composable yang dipanggil
grep -R "NavHost\|composable\|navigate(" app/src/main/java/com/example/fundsmanager/ui/navigation || true

# Pastikan composable yang diubah ada di route aktif
grep -R "<NamaComposable>" app/src/main/java/com/example/fundsmanager/ui || true
```

**Report wajib sertakan:**
```
Active Route      : [route string]
Active Composable : [file + function name]
File Changed      : [path lengkap file yang diubah]
Wired to Nav      : YES / NO
```

**Gate Hasil:** PASS jika composable yang diubah adalah composable yang dipanggil route aktif.
FAIL jika agent mengubah file yang tidak terhubung ke navigation.

---

## GATE 2 — DATA INTEGRITY GATE

### 2A. Long-Only Money Rule

```
WAJIB  : semua nominal uang pakai Long
DILARANG: Double atau Float untuk uang apapun

Contoh benar  : val amount: Long = 20000L
Contoh salah  : val amount: Double = 20000.0
Contoh salah  : val amount: Float = 20000f
```

**Search check:**
```bash
grep -R "Double\|Float" app/src/main/java/com/example/fundsmanager || true
```
PASS jika tidak ada Double/Float untuk field uang. (Double/Float untuk non-uang seperti animasi diperbolehkan tapi harus dijelaskan.)

### 2B. Soft Delete Rule

Data dengan `deletedAt != null` TIDAK BOLEH muncul atau dihitung di:

| Konteks | Aturan |
|---|---|
| Project list | filter `deletedAt == null` |
| Transaction list | filter `deletedAt == null` |
| Dashboard summary | hanya hitung non-deleted |
| Duplicate check | hanya bandingkan non-deleted |
| Import comparison | hanya bandingkan non-deleted |
| JSON export | exclude deleted |
| CSV export | exclude deleted |
| PDF export | exclude deleted |
| Excel export | exclude deleted |

**Aturan delete transaksi:**
```
WAJIB   : soft-delete (set deletedAt = timestamp)
DILARANG: hard-delete (DELETE FROM transactions)
```

**Search check:**
```bash
grep -R "DELETE FROM\|deleteById\|hardDelete" app/src/main/java/com/example/fundsmanager || true
```

### 2C. Transaction Required Fields

Setiap transaksi wajib punya:
```
projectId    — wajib, tidak boleh 0
userId       — wajib
accountId    — wajib, tidak boleh null
type         — FUND_IN | OFFICE_EXPENSE | PERSONAL_EXPENSE
date         — format yyyy-MM-dd
description  — wajib
reportedAmount — Long
realAmount     — Long
```

### 2D. Database Safety

```bash
# Pastikan tidak ada fallbackToDestructiveMigration di production
grep -R "fallbackToDestructiveMigration" app/src/main/java app/build.gradle* || true
```

PASS jika tidak ada, atau jika ada dan sudah didokumentasikan alasannya.

**Gate Hasil:** PASS jika semua 2A + 2B + 2C + 2D lulus.

---

## GATE 3 — FINANCIAL LOGIC GATE

### 3A. ProjectSummary sebagai Single Source of Truth

`CalculateProjectSummaryUseCase` adalah **satu-satunya** tempat formula summary dihitung.

**Formula resmi:**
```kotlin
totalFundIn          = sum(FUND_IN.reportedAmount)
totalOfficeReported  = sum(OFFICE_EXPENSE.reportedAmount)
totalOfficeReal      = sum(OFFICE_EXPENSE.realAmount)
totalPersonalExpense = sum(PERSONAL_EXPENSE.realAmount)
saving               = totalOfficeReported - totalOfficeReal
remainingReported    = totalFundIn - totalOfficeReported
remainingReal        = totalFundIn - totalOfficeReal
totalCashOut         = totalOfficeReal + totalPersonalExpense
netPosition          = totalFundIn - totalCashOut
```

**Formula ini TIDAK BOLEH diulang di:**
- Compose / UI layer
- ViewModel (kecuali meneruskan dari use case)
- PDF/Excel writer
- CSV exporter

**Search check:**
```bash
grep -R "sumOf" app/src/main/java/com/example/fundsmanager/ui || true
grep -R "sumOf" app/src/main/java/com/example/fundsmanager/presentation || true
```

PASS jika tidak ada `sumOf` di UI/presentation layer.

### 3B. Dashboard vs Export Consistency

Angka di dashboard harus identik dengan angka di PDF/Excel export.
Export wajib konsumsi `ProjectSummary` dari use case, bukan hitung ulang.

### 3C. Formatted Money String

```
WAJIB   : simpan Long ke database  (20000L)
DILARANG: simpan "Rp 20.000" ke database

Display  : format Long → "Rp 20.000" hanya di UI layer
Export   : format Long → "Rp 20.000" hanya saat render PDF/Excel
```

**Gate Hasil:** PASS jika ProjectSummary menjadi SSOT dan tidak ada duplikasi formula.

---

## GATE 4 — UI USABILITY GATE

### 4A. Label Indonesia

Semua label user-facing wajib Bahasa Indonesia.

| Raw / Salah | Label Benar |
|---|---|
| `FUND_IN` | Dana Masuk |
| `OFFICE_EXPENSE` | Expense Kantor |
| `PERSONAL_EXPENSE` | Expense Pribadi |
| `Transaction Form` | Form Transaksi |
| `Transactions` | Daftar Transaksi |
| `Save` | Simpan |
| `Cancel` | Batal |
| `Add` | Tambah |
| `Missing receipt` | Belum ada bukti |
| `Reported Amount` | Nominal Dilaporkan |
| `Real Amount` | Nominal Real |
| `Search description` | Cari transaksi... |

**Search check:**
```bash
grep -R "Transaction Form\|Transactions\|Missing receipt\|Reported Amount\|Real Amount\|Save\|Cancel\|Add" \
  app/src/main/java/com/example/fundsmanager/ui || true
```

PASS jika tidak ada raw English label di UI yang terlihat user.

### 4B. No Raw Enum di UI

```bash
grep -R "FUND_IN\|OFFICE_EXPENSE\|PERSONAL_EXPENSE" \
  app/src/main/java/com/example/fundsmanager/ui || true
```

PASS jika tidak ada raw enum tampil langsung ke user.

### 4C. UI State Requirements

| State | Requirement |
|---|---|
| Loading | tampilkan indicator, bukan blank screen |
| Empty | tampilkan pesan empty state yang jelas |
| Error | tampilkan pesan error dan opsi retry |
| Success | tampilkan konfirmasi (snackbar/toast) |

### 4D. Form Quality

- Dropdown tidak boleh menutupi field lain secara buruk
- Tombol proporsional dan tidak terlalu kecil untuk tap
- Input uang harus terformat saat blur (20000 → 20.000)
- Tanggal menggunakan DatePicker, bukan input teks bebas

**Gate Hasil:** PASS jika semua label Indonesia, tidak ada raw enum, dan semua UI state ditangani.

---

## GATE 5 — EXPORT / SHARE GATE

### 5A. Format Export

| Format | Peruntukan |
|---|---|
| PDF | laporan formal, share WhatsApp/email, cetak, reimburse |
| Excel/XLSX | analisis spreadsheet, reconciliation, kantor |
| JSON | backup/restore saja, BUKAN laporan utama |
| CSV | export sederhana/teknis, BUKAN laporan utama |

**Larangan:**
```
DILARANG: menyebut JSON backup sebagai "Laporan"
DILARANG: menjadikan raw JSON sebagai opsi share utama
DILARANG: menampilkan raw JSON di share sheet sebagai format pertama
```

### 5B. Isi Laporan PDF / Excel

**Header wajib:**
- Nama app: Funds Manager
- Nama project
- Tanggal & waktu export (yyyy-MM-dd HH:mm)
- Jenis laporan: Laporan Project

**Summary wajib (dari ProjectSummary):**
```
Total Dana Masuk
Posisi Bersih
Dilaporkan ke Kantor
Pengeluaran Real
Expense Pribadi
Total Keluar Real
Selisih / Hemat
Sisa Berdasarkan Laporan
Sisa Real
```

**Tabel transaksi wajib:**
```
Tanggal | Jenis | Keterangan | Akun | Kategori |
Nominal Dilaporkan | Nominal Real | Selisih | Catatan | Status Bukti
```

**Footer:**
- Generated by Funds Manager
- Offline-first local report

### 5C. PDF Requirements

```
File name  : FundsManager_[ProjectName]_[yyyyMMdd_HHmm].pdf
Font       : readable size, tidak terlalu kecil
Margin     : ada margin halaman
Tabel      : border atau alternating row background
Amount     : right-aligned
Wrap       : deskripsi panjang wrap, tidak terpotong
Multi-page : wajib support jika transaksi banyak
```

### 5D. Excel/XLSX Requirements

```
File name    : FundsManager_[ProjectName]_[yyyyMMdd_HHmm].xlsx
Sheet 1      : Ringkasan
Sheet 2      : Transaksi
Amount cells : numeric (Long value), BUKAN string "Rp 20.000"
Header row   : bold jika library support
Freeze row   : freeze header row jika library support
Column width : auto width jika praktis
```

**Search check:**
```bash
grep -R "file://" app/src/main/java/com/example/fundsmanager || true
```

### 5E. Android Share / FileProvider Rules

```
WAJIB   : FileProvider content:// URI
DILARANG: file:// URI langsung di share intent

MIME types:
- PDF  : application/pdf
- XLSX : application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

Storage: app cache dir atau app external files dir
Permission: tidak perlu storage permission jika pakai app-controlled dir + share intent
```

**FileProvider manifest wajib:**
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### 5F. Export Dependencies

Tambahkan ke `build.gradle` jika belum ada:

```gradle
// PDF
implementation 'com.tom_roush:pdfbox-android:2.0.27.0'
// XLSX
implementation 'org.apache.poi:poi-ooxml:5.2.3'
configurations.all {
    exclude group: 'org.apache.xmlbeans'
    exclude group: 'stax'
}
```

### 5G. Export Validation Rules

| Kondisi | Perilaku |
|---|---|
| Tidak ada transaksi | tetap export, tabel kosong, summary tetap ada |
| Project archived | boleh export |
| Project deleted | BLOKIR export, tampilkan "Project tidak tersedia" |
| Export berhasil | tampilkan snackbar sukses |
| Export gagal | tampilkan "Gagal membuat laporan. Coba lagi." |

**Gate Hasil:** PASS jika PDF + Excel export tersedia, FileProvider content://, MIME benar, soft-delete excluded, dan ProjectSummary dipakai.

---

## GATE 6 — FINAL MVP GATE

Semua gate di atas harus PASS sebelum menyatakan MVP selesai.

**Checklist final:**

```
GATE 1  Runtime Wiring      : PASS / FAIL
GATE 2  Data Integrity       : PASS / FAIL
GATE 3  Financial Logic      : PASS / FAIL
GATE 4  UI Usability         : PASS / FAIL
GATE 5  Export / Share       : PASS / FAIL
Build   assembleDebug        : PASS / FAIL
Test    testDebugUnitTest    : PASS / FAIL
```

MVP hanya dinyatakan selesai jika semua baris di atas PASS.

---

## ARCHITECTURE RULES (Permanent)

### Layer Isolation

```
DOMAIN LAYER
  ✓ domain model
  ✓ repository interface
  ✓ use case
  ✗ DILARANG import: data.local.dao / data.local.entity
  ✗ DILARANG import: android.* / androidx.room.* / androidx.compose.*

DATA LAYER
  ✓ Room, DAO, Entity, mapper
  ✓ repository implementation
  ✓ storage, file writer
  ✗ tidak boleh ambil keputusan UI

UI LAYER
  ✓ Compose hanya tampilkan state
  ✓ ViewModel kelola state dan panggil use case
  ✗ UI tidak boleh akses DAO langsung
  ✗ UI tidak boleh pakai Room Entity langsung
  ✗ UI tidak boleh hitung ProjectSummary
```

**Search check arsitektur:**
```bash
grep -R "data.local\|android\.\|androidx.room\|androidx.compose" \
  app/src/main/java/com/example/fundsmanager/domain || true
```

### Recommended Use Case Split untuk Export

```
DOMAIN:
  ProjectReportData
  PrepareProjectReportUseCase

DATA/EXPORT:
  PdfReportWriter
  ExcelReportWriter
  ReportFileRepository

UI:
  ViewModel.exportPdf(projectId)
  ViewModel.exportExcel(projectId)
  ViewModel.sharePdf(projectId)
  ViewModel.shareExcel(projectId)
```

### Report Data Model

```kotlin
data class ProjectReportData(
    val project: Project,
    val summary: ProjectSummary,
    val transactions: List<TransactionReportRow>,
    val exportedAt: String
)

data class TransactionReportRow(
    val date: String,
    val typeLabel: String,         // "Dana Masuk" bukan "FUND_IN"
    val description: String,
    val accountName: String,
    val categoryName: String?,
    val reportedAmount: Long,
    val realAmount: Long,
    val saving: Long,
    val note: String?,
    val receiptStatus: String      // "Ada bukti" / "Belum ada bukti"
)
```

---

## IMPORT RULES

Import tracker-duit wajib two-stage:

**Stage 1 — Dry-run (TIDAK menulis ke database):**
1. Parse JSON
2. Mapping field
3. Validasi
4. Duplicate detection (bandingkan `legacyHash`)
5. Tampilkan preview hasil

**Stage 2 — Confirm import (atomic):**
1. Insert hanya valid non-duplicate
2. Gunakan database transaction
3. Rollback total jika ada satu insert gagal

**Imported transaction wajib:**
```
legacyHash  : non-null
projectId   : bukan 0
accountId   : bukan null
```

---

## FULL SELF-CHECK COMMAND SUITE

Jalankan semua ini sebelum submit final report:

```bash
# 1. Build dan test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug

# 2. Domain isolation
grep -R "data.local\|android\.\|androidx.room\|androidx.compose" \
  app/src/main/java/com/example/fundsmanager/domain || true

# 3. No Float/Double untuk uang
grep -R "Double\|Float" \
  app/src/main/java/com/example/fundsmanager || true

# 4. No formula di UI
grep -R "sumOf" \
  app/src/main/java/com/example/fundsmanager/ui || true

# 5. No raw enum di UI
grep -R "FUND_IN\|OFFICE_EXPENSE\|PERSONAL_EXPENSE" \
  app/src/main/java/com/example/fundsmanager/ui || true

# 6. No English label di UI
grep -R "Transaction Form\|Transactions\|Missing receipt\|Reported Amount\|Real Amount\|Save\|Cancel\|Add" \
  app/src/main/java/com/example/fundsmanager/ui || true

# 7. No file:// URI
grep -R "file://" \
  app/src/main/java/com/example/fundsmanager || true

# 8. No destructive migration
grep -R "fallbackToDestructiveMigration" \
  app/src/main/java app/build.gradle* || true

# 9. No hard-delete transaksi
grep -R "DELETE FROM\|deleteById\|hardDelete" \
  app/src/main/java/com/example/fundsmanager || true
```

---

## AGENT BEHAVIOR RULES

Agent wajib:
1. Membaca file ini sebelum mengerjakan task apapun
2. Mengidentifikasi `Active Route` dan `Active Composable` sebelum mengubah UI
3. Tidak membuat screen/komponen baru yang tidak terhubung ke navigation
4. Tidak mengklaim selesai hanya karena build sukses
5. Menjalankan full self-check command suite sebelum final report
6. Menyertakan bukti command output / test result / grep result
7. Jika ada FAIL, laporkan error dan jangan klaim task selesai

Agent dilarang:
1. Mengubah `CalculateProjectSummaryUseCase` tanpa alasan eksplisit
2. Menambahkan fitur di luar scope yang diperintahkan
3. Membuat file baru tanpa mewire ke navigation (untuk UI)
4. Menggunakan Double/Float untuk field uang
5. Menyimpan formatted money string ke database
6. Hard-delete transaksi
7. Menampilkan `file://` URI di share intent

---

## DEFINITION OF DONE

Task dianggap **selesai** hanya jika:

```
1. Semua gate relevan PASS (bukan sekedar tidak FAIL)
2. Build sukses: ./gradlew :app:assembleDebug → BUILD SUCCESSFUL
3. Test sukses: ./gradlew :app:testDebugUnitTest → BUILD SUCCESSFUL
4. Semua grep check tidak menemukan violation
5. Final report menyertakan semua bukti di atas
6. Untuk task UI: Active Route dan Active Composable tercatat
7. Untuk task UI: Runtime APK menampilkan perubahan yang diharapkan
```

---

## FINAL REPORT TEMPLATE

Setiap agent wajib submit dalam format ini:

```
TASK COMPLETION REPORT
======================
Task          : [ringkasan task]
Active Route  : [route string — wajib untuk task UI]
Active Screen : [composable file + function — wajib untuk task UI]

Root Cause    : [mengapa kondisi awal salah]

Files Changed :
  - [path file 1]
  - [path file 2]

What Changed  :
  - [perubahan 1]
  - [perubahan 2]

Runtime Verification :
  - [flow 1] : PASS/FAIL
  - [flow 2] : PASS/FAIL

Gate Results :
  GATE 1  Runtime Wiring    : PASS/FAIL/N.A.
  GATE 2  Data Integrity    : PASS/FAIL/N.A.
  GATE 3  Financial Logic   : PASS/FAIL/N.A.
  GATE 4  UI Usability      : PASS/FAIL/N.A.
  GATE 5  Export/Share      : PASS/FAIL/N.A.

Build/Test   :
  - ./gradlew testDebugUnitTest : PASS/FAIL
  - ./gradlew assembleDebug     : PASS/FAIL

Search Checks :
  - domain isolation   : PASS/FAIL
  - no Double/Float    : PASS/FAIL
  - no sumOf in UI     : PASS/FAIL
  - no raw enum in UI  : PASS/FAIL
  - no English label   : PASS/FAIL
  - no file:// URI     : PASS/FAIL
  - no hard-delete     : PASS/FAIL

Result        : PASS / FAIL

Remaining Risk:
  - [risiko tersisa atau "None"]
```

---

> **Versi dokumen:** 1.0
> **Project:** Funds Manager Android — CV Kendari Karya Teknologi
> **Berlaku untuk:** semua agent (Claude Code, Gemini CLI, Codex, dll)
> **Syarat update dokumen ini:** ada perubahan arsitektur fundamental atau penambahan fitur baru yang disetujui.

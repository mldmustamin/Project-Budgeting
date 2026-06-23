# Funds Manager

Funds Manager adalah aplikasi Android offline-first untuk pencatatan dana project dan perjalanan dinas. Aplikasi ini fokus pada alur kerja keuangan lapangan: project, dana masuk, expense kantor, expense pribadi, bukti transaksi, dan export laporan.

## Fitur Utama

- Manajemen project aktif dan project arsip.
- Dashboard per project dengan ringkasan keuangan.
- Pencatatan transaksi:
  - `FUND_IN`
  - `OFFICE_EXPENSE`
  - `PERSONAL_EXPENSE`
- Form transaksi dengan:
  - tanggal,
  - keterangan,
  - nominal dilaporkan,
  - nominal real,
  - akun,
  - kategori,
  - catatan.
- Daftar transaksi dengan pencarian, filter tipe, filter bukti, dan soft delete.
- Lampiran bukti transaksi dari galeri atau kamera.
- Export laporan per project ke PDF, Excel, dan CSV.
- Logging navigasi dan crash lewat file logger lokal.

## Tech Stack

- Kotlin
- Jetpack Compose Material 3
- Room
- Hilt
- Navigation Compose
- DataStore
- Kotlin Serialization
- Coil

## Persyaratan

- Android Studio terbaru yang mendukung AGP `8.10.0`
- JDK 17
- Android SDK dengan compile/target SDK `36`
- Minimum SDK `26`

## Cara Menjalankan

1. Buka project ini di Android Studio.
2. Tunggu Gradle sync selesai.
3. Jalankan aplikasi ke emulator atau device Android.

Atau lewat terminal:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

APK debug akan dihasilkan di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Alur Aplikasi

1. Aplikasi membuka `Project List`.
2. User membuat project baru atau membuka project yang sudah ada.
3. Di `Dashboard`, user melihat ringkasan project dan transaksi terbaru.
4. User menambah transaksi lewat `Form Transaksi`.
5. User bisa membuka `Daftar Transaksi`, mencari transaksi, memberi bukti, atau menghapus secara soft delete.
6. Laporan project dapat diexport ke PDF, Excel, atau CSV dari dashboard.

## Struktur Proyek

```text
app/src/main/java/com/example/fundsmanager
├── data
│   ├── local
│   ├── mapper
│   ├── repository
│   └── service
├── domain
│   ├── model
│   ├── repository
│   ├── service
│   └── usecase
├── ui
│   ├── component
│   ├── navigation
│   └── screen
├── di
└── util
```

## Komponen Domain Penting

- `FundsRepository` sebagai kontrak data utama.
- `CalculateProjectSummaryUseCase` sebagai sumber tunggal perhitungan ringkasan project.
- `ExportCsvUseCase` untuk export CSV ringkasan transaksi dan project.
- `ReportFileRepository` untuk pembuatan file PDF dan Excel.

## Catatan Implementasi

- Aplikasi ini tidak menggunakan backend cloud atau server sync.
- Nominal uang disimpan sebagai `Long`, bukan `Double` atau `Float`.
- Penghapusan transaksi memakai soft delete.
- UI dibuat dengan pendekatan finance app yang compact dan card-based.
- Route awal aplikasi adalah `project_list`.

## Testing

Project ini memiliki unit test dan instrumented test. Perintah yang umum dipakai:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Lisensi

Belum ditentukan.

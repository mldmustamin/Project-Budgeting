# Funds Manager App Brief

## Ringkasan

Funds Manager adalah aplikasi Android offline-first untuk mencatat dana project dan perjalanan dinas. Fokus utamanya adalah pengelolaan project, dana masuk, expense kantor, expense pribadi, bukti transaksi, import backup JSON, dan export laporan.

## Tujuan Produk

- Memudahkan pencatatan keuangan lapangan tanpa backend cloud.
- Menyediakan ringkasan dana project yang cepat dibaca.
- Menjaga data tetap rapi lewat akun, kategori, bukti transaksi, dan soft delete.
- Mendukung alur impor data legacy dari `tracker-duit`.

## Fitur Inti

- Project list dengan project aktif dan arsip.
- Dashboard per project dengan ringkasan keuangan dan transaksi terbaru.
- Form transaksi untuk:
  - dana masuk,
  - expense kantor,
  - expense pribadi.
- Daftar transaksi dengan pencarian, filter tipe, filter bukti, edit, dan soft delete.
- Import preview untuk JSON `tracker-duit`.
- Export laporan per project ke PDF, Excel, dan CSV.
- Lampiran bukti dari kamera atau galeri.

## Arsitektur Singkat

- UI: Jetpack Compose Material 3.
- State management: ViewModel + StateFlow.
- Data layer: Room + repository pattern.
- Dependency injection: Hilt.
- Navigasi: Navigation Compose.
- Storage tambahan: DataStore dan file storage lokal.

## Route Utama

- `project_list`
- `project_dashboard/{projectId}`
- `transaction_form/{projectId}?transactionId={transactionId}`
- `transaction_list/{projectId}`
- `import_preview`
- `transaction_home`
- `report_home`
- `settings`

## Aturan Domain Penting

- Semua nominal uang disimpan sebagai `Long`.
- Ringkasan project dihitung dari `CalculateProjectSummaryUseCase`.
- Transaksi yang dihapus memakai soft delete, bukan hard delete.
- UI tidak menghitung ulang rumus keuangan.
- Aplikasi tidak memakai cloud sync, payment gateway, OCR, AI, atau server sync.

## Stack

- Kotlin
- Jetpack Compose
- Room
- Hilt
- Navigation Compose
- DataStore
- Kotlin Serialization
- Coil

## Build dan Jalankan

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

APK debug:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Struktur Folder Tinggi

- `data/` untuk local database, repository, mapper, dan service.
- `domain/` untuk model, use case, repository contract, dan service contract.
- `ui/` untuk screen, component, navigation, dan theme.
- `di/` untuk modul Hilt.
- `util/` untuk logging dan helper teknis.

## Catatan Implementasi

- Start destination aplikasi adalah `project_list`.
- Dashboard memuat `ProjectSummary` dan transaksi terbaru.
- Import JSON menggunakan preview sebelum commit data.
- Export laporan dibuat dari data project yang sudah dihitung di domain layer.
- `settings` route sudah ada di navigation, tetapi screen-nya belum terhubung.


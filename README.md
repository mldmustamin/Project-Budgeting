# Funds Manager

Funds Manager adalah aplikasi Android offline-first untuk mengelola dana project dengan alur yang sederhana dan fokus pada kebutuhan operasional lapangan. Build saat ini sudah berada pada baseline stabil awal dengan flow utama yang berfungsi: project, transaksi, dashboard, kategori, lampiran bukti, dan pengaturan dasar.

## Status Build

- Status: stable production-ready build
- Branch terakhir dipush: `main`
- Commit stabil terakhir: `211e154`

## Fitur yang Sudah Aktif

- Dashboard utama dengan ringkasan keuangan, quick action, dan transaksi terbaru
- Daftar project aktif dan arsip
- Tambah project dengan `project start` dan `project selesai`
- Edit nama project
- Edit tanggal start dan end project
- Dashboard detail per project
- Form transaksi dengan 2 flow:
  - `Pemasukan` -> `Transfer Dana`
  - `Pengeluaran` -> `Pengeluaran Pekerjaan` dan `Pengeluaran Pribadi`
- Edit transaksi
- Daftar transaksi global dan daftar transaksi per project
- Upload bukti transaksi dari file atau kamera
- Kelola kategori transaksi dari menu setting
- Ikon aplikasi kustom
- Refresh data saat screen utama kembali aktif

## Catatan Produk Saat Ini

- Aplikasi berjalan lokal tanpa backend cloud
- Semua nominal uang disimpan sebagai `Long`
- Soft delete dipakai untuk transaksi dan project
- `Backup & ekspor` masih berstatus `Coming soon`
- `Hubungi developer` di setting mengarah ke WhatsApp

## Stack

- Kotlin
- Jetpack Compose Material 3
- Room
- Hilt
- Navigation Compose
- DataStore
- Coil

## Build APK

Build debug bisa dibuat dengan:

```bash
./gradlew assembleDebug
```

Hasil APK debug ada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Menjalankan Project

1. Buka project ini di Android Studio.
2. Gunakan JDK 17.
3. Pastikan Android SDK untuk `compileSdk 36` tersedia.
4. Jalankan ke emulator atau device Android.

## Struktur Ringkas

```text
app/src/main/java/com/example/fundsmanager
├── data
├── di
├── domain
├── ui
└── util
```

## Dokumentasi Tambahan

Seluruh dokumen pendukung sudah dirapikan di folder [docs](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/docs), termasuk:

- [appbrief.md](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/docs/appbrief.md)
- [deepreview.md](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/docs/deepreview.md)
- [Funds-Manager.md](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/docs/Funds-Manager.md)
- [LOGICFIX.md](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/docs/LOGICFIX.md)
- [UIAdjustment.md](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/docs/UIAdjustment.md)

## Lisensi

Belum ditentukan.

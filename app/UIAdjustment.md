TASK:
Rebuild Funds Manager Android UI to match the attached mockup as closely as possible.

IMPORTANT:
The attached image is the approved UI reference. Do not reinterpret the design freely. Do not create a new style. The goal is visual parity with the mockup.

Current problem:
The existing UI is functional but still too plain and does not match the approved mockup. The previous implementation only followed the rough structure, not the actual visual design.

GOAL:
Make the app UI look as close as possible to the attached mockup:

* same screen structure
* same card-based layout
* same spacing rhythm
* same compact financial dashboard style
* same clean input fields
* same transaction cards
* same button proportions
* same Indonesian finance app feel

Do not stop at “functional”. The result must visually resemble the mockup.

DO NOT:

* Do not change business logic.
* Do not change ProjectSummary formulas.
* Do not move financial calculation into Compose.
* Do not access DAO directly from UI.
* Do not use Room Entity directly in UI.
* Do not add cloud sync, OCR, payment, AI, banking, or server sync.
* Do not create a different design system.
* Do not leave UI as plain stacked TextFields.

REFERENCE DESIGN RULE:
Use the attached mockup as the source of truth. If there is a conflict between existing UI and the mockup, follow the mockup unless it breaks business logic.

==================================================
GLOBAL DESIGN REQUIREMENTS
==========================

1. Visual Style
   Match the mockup:

* clean white / off-white background
* deep navy primary buttons
* subtle blue dashboard card
* soft rounded white cards
* small badges
* compact spacing
* finance-app style typography
* light shadows or subtle borders
* polished but minimal

2. Main Colors
   Use a consistent palette close to the mockup:

* Primary navy: deep blue
* Background: near-white / very light gray
* Card: white
* Dashboard card: soft blue gradient or soft blue surface
* Positive: green
* Expense/danger: red
* Muted text: gray-blue
* Border: very light gray

3. Shape

* Main cards: 16dp to 20dp radius
* Inputs: 10dp to 14dp radius
* Buttons: 14dp to 18dp radius
* Badges: pill shape
* Avoid square hard edges

4. Typography
   Match the mockup hierarchy:

* Screen title: compact, bold
* Section title: bold
* Card title: semi-bold
* Metadata: small, muted
* Amount: bold and right-aligned
* Use smaller, denser typography than current app

5. Spacing
   Use compact, mockup-like spacing:

* screen horizontal padding: 20dp
* section gap: 16dp to 20dp
* card padding: 14dp to 16dp
* input gap: 10dp to 14dp
* avoid huge empty areas

6. Language
   Use Indonesian-first labels:

* Project Aktif
* Lihat Project Arsip
* Import & Backup
* Import dari tracker-duit
* Form Transaksi
* Tanggal
* Keterangan
* Nominal Dana Masuk
* Nominal Dilaporkan
* Nominal Real
* Akun
* Kategori
* Catatan
* Simpan
* Transaksi
* Transaksi Terbaru
* Tambah Transaksi
* Dana Masuk
* Expense Kantor
* Expense Pribadi
* Belum ada bukti
* Ada bukti
* Hapus transaksi?
* Batal
* Hapus

Never show raw enum names:

* FUND_IN
* OFFICE_EXPENSE
* PERSONAL_EXPENSE

==================================================
SCREEN 1 — PROJECT LIST
=======================

Rebuild Project List to match the mockup.

Layout:

* Top bar:

    * Left: Funds Manager
    * Right: settings icon
* Section header:

    * Left: Project Aktif
    * Right: compact primary button “+ Project”
* Project cards:

    * Rounded white card
    * Left icon in soft colored circle/square
    * Project name
    * small transaction count
    * Posisi Bersih amount on right
    * Dana Masuk amount
    * right arrow or archive/action icon
* Archive button:

    * full-width outlined small button
    * label: Lihat Project Arsip
* Import section:

    * title: Import & Backup
    * white action card
    * label: Import dari tracker-duit
    * right chevron

Important:

* The + Project button must be compact like the mockup.
* Do not use oversized pill button.
* Do not use giant empty space.
* Project card must look like a polished card, not a raw row.

==================================================
SCREEN 2 — PROJECT DASHBOARD
============================

Rebuild Dashboard to match the mockup.

Top bar:

* Back button
* Project name
* Optional share/menu icons

Summary card:

* Large soft blue rounded card
* Must display ProjectSummary values only
* Strong emphasis on Posisi Bersih
* Rows:

    * Total Dana Masuk
    * Posisi Bersih
    * Dilaporkan ke Kantor
    * Pengeluaran Real
    * Expense Pribadi
    * Total Keluar Real
    * Selisih / Hemat
    * Sisa Berdasarkan Laporan
    * Sisa Real

Style:

* Amounts right-aligned
* Posisi Bersih larger/bolder
* Positive values green
* Expense values normal or red where appropriate
* Use divider lines like mockup

CTA:

* Full-width navy button:

    * Tambah Transaksi

Recent Transactions:

* Section title: Transaksi Terbaru
* Link: Lihat Semua
* Compact transaction cards
* Show:

    * date
    * description
    * type badge
    * account
    * amount

Do not calculate values in Compose.
Do not use sumOf in UI.

==================================================
SCREEN 3 — FORM TRANSAKSI
=========================

This screen must be rebuilt to look like the mockup form, not the current plain form.

Top bar:

* Back button
* Title: Form Transaksi
* Attachment icon
* Optional check icon only if bottom button is removed
* Prefer one clear save action, not duplicate confusing actions

Form:
Use compact vertical layout similar to mockup.

Fields:

1. Jenis Transaksi

* Dropdown field, full width
* Shows icon + label:

    * Dana Masuk
    * Expense Kantor
    * Expense Pribadi
* Must not show raw enum
* Must not use broken chips

2. Tanggal

* Full-width input
* Label: Tanggal
* Value format: yyyy-MM-dd
* Optional calendar icon

3. Keterangan

* Full-width input
* Label: Keterangan

4. Nominal behavior:
   A. Dana Masuk:

* Show one field: Nominal Dana Masuk
* Internally reportedAmount = value and realAmount = value

B. Expense Kantor:

* Show two fields side-by-side if width allows, otherwise stacked:

    * Nominal Dilaporkan
    * Nominal Real
* Internally reportedAmount and realAmount may differ

C. Expense Pribadi:

* Show one field: Nominal Expense
* Internally reportedAmount = value and realAmount = value

5. Akun

* Full-width dropdown
* Label: Akun
* Default: Cash in Hand
* Must look like mockup selector

6. Kategori

* Full-width dropdown
* Label: Kategori
* Options:

    * Tanpa Kategori
    * Makan & Minum
    * Transportasi
    * Penginapan
    * Alat Tulis Kantor
    * Lain-lain

7. Catatan

* Full-width input
* Label: Catatan
* Optional

8. Save button

* Full-width navy button
* Label: Simpan
* Same proportion as mockup

Money input:

* User types 20000 and sees 20,000.
* User types 2000000 and sees 2,000,000.
* Internal value remains Long.
* No Double/Float.
* Do not store formatted string.

Validation:

* amount > 0
* date yyyy-MM-dd
* account required
* description required for expense
* show errors in Indonesian

==================================================
SCREEN 4 — TRANSACTION LIST
===========================

Rebuild Transaction List to match mockup.

Top bar:

* Back button
* Title: Transaksi
* filter icon/menu icon

Search:

* full-width rounded search field
* placeholder: Cari keterangan, catatan, atau tanggal

Filter chips:

* Semua
* Dana Masuk
* Expense Kantor
* Expense Pribadi
* Must look like mockup pills
* No raw enum labels

Receipt filter:

* Toggle/chip: Belum ada bukti

Transaction card:

* Rounded white card
* Date block or date text
* Description
* Amount right-aligned
* Type badge
* Account
* Category if available
* Receipt badge:

    * Ada bukti
    * Belum ada bukti
* 3-dot menu or edit/delete actions

Delete:

* Must use confirmation dialog
* Must soft-delete only

==================================================
SCREEN 5 — DELETE DIALOG
========================

Match mockup dialog style.

Dialog:

* Dimmed background
* Rounded white card
* Warning icon
* Title: Hapus transaksi?
* Message:
  Transaksi akan disembunyikan dari daftar dan tidak ikut perhitungan.
* Buttons:

    * Batal
    * Hapus
* Hapus button red

==================================================
EMPTY STATES
============

Create mockup-like empty state:

* Icon/illustration if possible
* Title:
  Belum ada transaksi
* Body:
  Yuk, catat transaksi pertama kamu pada project ini.
* Button:

    * Tambah Transaksi

For no project:

* Belum ada project
* * Buat Project

==================================================
REUSABLE COMPONENTS REQUIRED
============================

Create or refactor toward reusable UI components:

* AppTopBar
* PrimaryButton
* ProjectCard
* SummaryCard
* SummaryRow
* TransactionCard
* TransactionTypeDropdown
* MoneyInputField
* AppDropdownField
* ReceiptBadge
* EmptyState
* DeleteConfirmationDialog

Centralize label mapping:

* TransactionType.toUiLabel()
* TransactionType.toBadgeColor()
* Receipt status label
* Money formatter

==================================================
ARCHITECTURE RULES
==================

Must remain true:

* UI does not access DAO.
* UI does not use Room Entity.
* Compose does not calculate ProjectSummary.
* ViewModel does not duplicate financial formulas.
* Dashboard consumes ProjectSummary.
* Domain layer has no data/android/room/compose imports.
* Money remains Long.
* Date remains yyyy-MM-dd.
* Every transaction has accountId.
* Soft delete remains soft delete.

==================================================
SELF-CHECK BEFORE DONE
======================

1. Visual Parity Check

* Does the Project List look close to the mockup?
* Does Dashboard summary card look close to the mockup?
* Does Form Transaksi look close to the mockup?
* Does Transaction List look close to the mockup?
* Are card shapes, spacing, typography, and buttons visually close?

If the answer is “it is only functional but still plain”, the task is not done.

2. Runtime Check

* Create Project works.
* Open Dashboard works.
* Add Dana Masuk works.
* Add Expense Kantor works.
* Money input shows 20,000 formatting.
* Dashboard updates correctly.
* Transaction List opens.
* Delete confirmation works.
* Soft delete works.

3. Label Check

* No raw enum labels visible.
* No English labels from old UI remain:

    * Transaction Form
    * Transactions
    * Search description
    * Missing receipt
    * Reported Amount
    * Real Amount
    * Save

4. Architecture Check

* No DAO access from UI.
* No Room Entity used in UI.
* No financial sum in Compose.
* No ProjectSummary formula duplicated in ViewModel.

5. Test/Build Check
   Run:
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:assembleDebug

Search checks:
grep -R "FUND_IN\|OFFICE_EXPENSE\|PERSONAL_EXPENSE" app/src/main/java/com/example/fundsmanager/ui || true
grep -R "Transaction Form\|Transactions\|Search description\|Missing receipt\|Reported Amount\|Real Amount\|Save" app/src/main/java/com/example/fundsmanager/ui || true
grep -R "sumOf" app/src/main/java/com/example/fundsmanager/ui || true
grep -R "Double\|Float" app/src/main/java/com/example/fundsmanager || true
grep -R "data.local\|android\.\|androidx.room\|androidx.compose" app/src/main/java/com/example/fundsmanager/domain || true
grep -R "fallbackToDestructiveMigration" app/src/main/java app/build.gradle* || true

==================================================
MANUAL VERIFICATION
===================

Install debug APK and verify against the attached mockup.

Required:

1. Project List visually close to mockup.
2. Dashboard visually close to mockup.
3. Form Transaksi visually close to mockup.
4. Transaction List visually close to mockup.
5. Delete dialog visually close to mockup.
6. No raw enum labels.
7. No broken dropdowns.
8. No plain stacked developer-looking form.
9. Money input formatting works.
10. Core data flow still works.

==================================================
FINAL REPORT FORMAT
===================

PIXEL-CLOSE UI ALIGNMENT REPORT

Files Changed:

* [file 1]
* [file 2]

What Was Wrong:

* [issue 1]
* [issue 2]

What Changed:

* [change 1]
* [change 2]

Visual Parity Result:

* Project List close to mockup: PASS/FAIL
* Dashboard close to mockup: PASS/FAIL
* Form Transaksi close to mockup: PASS/FAIL
* Transaction List close to mockup: PASS/FAIL
* Delete Dialog close to mockup: PASS/FAIL

Runtime Flow Verified:

* Create Project: PASS/FAIL
* Add Dana Masuk: PASS/FAIL
* Add Expense Kantor: PASS/FAIL
* Dashboard updates: PASS/FAIL
* Transaction List works: PASS/FAIL
* Soft Delete works: PASS/FAIL

Checks:

* Scope Check: PASS/FAIL
* Visual Parity Check: PASS/FAIL
* Runtime Check: PASS/FAIL
* Architecture Check: PASS/FAIL
* Financial Logic Check: PASS/FAIL
* Clean Code Check: PASS/FAIL
* Test/Build Check: PASS/FAIL

Commands Run:

* ./gradlew :app:testDebugUnitTest
* ./gradlew :app:assembleDebug
* grep/search checks

Result:

* PASS/FAIL

Remaining Risk:

* [list risks or None]

Do not mark complete if the UI is merely functional. It must be visually close to the attached mockup.

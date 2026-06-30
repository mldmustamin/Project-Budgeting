---
created: 2026-06-30
status: complete
tags: [workflow, budget, approval]
---

# Budget Request Workflow — 7 Stages

```
STAGE 0:   SUPERVISOR assign task via app → FE notifikasi
             │
STAGE 1:   DRAFT             FE isi form estimasi (pilih lokasi, job type, item)
             │                  Pagu enforcement: FIXED di-block, HOTEL warning, MANAGER pass
STAGE 2:   ESTIMASI          FE submit → masuk inbox SUPERVISOR
             │
STAGE 3:   FORWARDED         SUPERVISOR review + edit item → forward ke OWNER
             │                  (Audit: estimated + revised_amount tercatat)
STAGE 4:   APPROVED          OWNER lihat historis lokasi → approve + final nominal
             │
           ─── PEKERJAAN DI LAPANGAN ───
             │
STAGE 5:   REALISASI         FE input realisasi per item + upload bukti kwitansi
             │                  + LAPORAN PEKERJAAN (teknis: perangkat, parameter, foto SCM)
STAGE 6:   VERIFIED          ADMIN/FINANCE_MANAGER cek bukti per item, bill_verified
             │
STAGE 7:   RECONCILED        FINANCE_MANAGER crosscheck Kordinator → final
```

## Rejection Flow
```
ESTIMASI ──reject──▶ DRAFT (reset semua revisi/approval)
FORWARDED ──reject──▶ DRAFT
FE lihat history → revisi → submit ulang
```

## Offline Flow (FIELD_ENGINEER)
```
FE create/edit draft (Room DB) → auto-save setiap perubahan
FE klik submit → outbox entry → toast "Menunggu jaringan"
SyncWorker (15 min periodic + one-time after login)
  → push outbox → server
  → pull changes → update local Room DB
Toast: "3 form terkirim, 1 gagal"
Retry: 5x → flag error
```

## Related
- [[03 - Backend/API Routes]] — All endpoints
- [[05 - Database/Schema]] — Table structure
- [[08 - Open QNA/OPEN_QNA]] — Q31-Q43

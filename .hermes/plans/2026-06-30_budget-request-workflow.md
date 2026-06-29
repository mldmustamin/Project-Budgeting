# Budget Request Workflow — Implementation Plan

> **For Hermes:** Execute task-by-task, verify each step, commit after each task.

**Goal:** Build the complete budget request workflow: from FIELD_ENGINEER estimating expenses → SUPERVISOR review/edit → OWNER approval with location history → FIELD_ENGINEER realization → ADMIN/FINANCE verification → FINANCE reconciliation.

**Architecture:** Laravel 11 backend (PostgreSQL) + Android Kotlin/Compose (single APK, role-based UI). Single form multi-stage model: `task_expenses` + `expense_items`. Offline-capable via Room DB + WorkManager outbox pattern (existing). Pagu enforcement at stage 1 with 35 budget categories across 3 pagu types (FIXED_PAGU, TICKET, MANAGER_APPROVAL).

**Tech Stack:** Laravel 11, PostgreSQL 14, Android Kotlin/Compose, Hilt, Room, WorkManager, Ktor HTTP client

---

## Phase 1: Database Migrations (Backend)

### Task 1.1: Create budget_item_templates migration

**Objective:** Store 35 budget categories with pagu rules

**Files:**
- Create: `backend/database/migrations/XXXX_XX_XX_000001_create_budget_item_templates_table.php`

**Step 1: Create migration**

```bash
php artisan make:migration create_budget_item_templates_table
```

**Step 2: Write schema**

```php
Schema::create('budget_item_templates', function (Blueprint $table) {
    $table->id();
    $table->uuid('uuid')->unique();
    $table->string('category_name'); // 35 kategori: "Tiket Pesawat Berangkat", etc
    $table->enum('category_group', [
        'PAKET_LK', 'HOTEL_LK', 'HOTEL_PAPUA', 'VOUCHER_HP',
        'BURUH', 'BALLAST', 'TRANSPORT_HB', 'TRANSPORT_LK',
        'FEE_HB', 'FEE_LK',
        // TICKET group
        'TIKET_PESAWAT_BERANGKAT', 'TIKET_PESAWAT_PULANG',
        'TIKET_FERRY_BERANGKAT', 'TIKET_FERRY_PULANG',
        'TIKET_BIS_BERANGKAT', 'TIKET_BIS_PULANG',
        'TIKET_TRAVEL_BERANGKAT', 'TIKET_TRAVEL_PULANG',
        'TIKET_KERETA_BERANGKAT', 'TIKET_KERETA_PULANG',
        'TIKET_LAINNYA',
        // MANAGER_APPROVAL group
        'TRANSPORT_OJEK_BERANGKAT', 'TRANSPORT_OJEK_PULANG',
        'TRANSPORT_TAKSI_BERANGKAT', 'TRANSPORT_TAKSI_PULANG',
        'TRANSPORT_OJEK_BELI_MATERIAL', 'TRANSPORT_BENSIN',
        'TRANSPORT_LAINNYA', 'AKOMODASI_UANG_MAKAN',
        'AKOMODASI_PAKET', 'AKOMODASI_HOTEL', 'AKOMODASI_LAINNYA',
        'BIAYA_LAIN_VOUCHER', 'BIAYA_LAIN_BURUH', 'BIAYA_LAIN_MATERIAL',
        'BIAYA_LAIN_BALLAST', 'BIAYA_LAIN_LIFTING',
        'BIAYA_LAIN_TARIK_KABEL', 'BIAYA_LAIN_TEBANG_POHON',
        'BIAYA_LAIN_FEE', 'BIAYA_LAINNYA',
        'PENGEMBALIAN_DANA', 'BIAYA_MOUNTING', 'JASA_SUBCONT'
    ]);
    $table->enum('pagu_type', ['FIXED_PAGU', 'TICKET', 'MANAGER_APPROVAL']);
    $table->bigInteger('pagu_amount')->nullable(); // null for TICKET and MANAGER_APPROVAL
    $table->boolean('requires_bill')->default(false);
    $table->text('bill_note')->nullable();
    $table->boolean('is_active')->default(true);
    $table->timestamps();
});
```

**Step 3: Run migration**

```bash
php artisan migrate
```

**Verification:** `php artisan db:show --database=pgsql` — `budget_item_templates` table exists

**Step 4: Commit**

```bash
git add backend/database/migrations/
git commit -m "feat: create budget_item_templates migration"
```

---

### Task 1.2: Create master_locations migration

**Objective:** Store location master data (CRUD by ADMIN + SUPERVISOR)

**Files:**
- Create: `backend/database/migrations/XXXX_XX_XX_000002_create_master_locations_table.php`

**Step 1: Write migration**

```php
Schema::create('master_locations', function (Blueprint $table) {
    $table->id();
    $table->uuid('uuid')->unique();
    $table->foreignId('project_id')->constrained()->cascadeOnDelete();
    $table->string('remote_name');
    $table->text('address');
    $table->decimal('latitude', 10, 7)->nullable();
    $table->decimal('longitude', 10, 7)->nullable();
    $table->foreignId('created_by')->constrained('users');
    $table->foreignId('updated_by')->nullable()->constrained('users');
    $table->timestamps();
    $table->softDeletes();
});
```

**Step 2: Run migration**

```bash
php artisan migrate
```

**Verification:** Table exists in DB

**Step 3: Commit**

---

### Task 1.3: Create task_expenses migration

**Objective:** Single form, 7-stage workflow

**Files:**
- Create: `backend/database/migrations/XXXX_XX_XX_000003_create_task_expenses_table.php`

**Step 1: Write migration**

```php
Schema::create('task_expenses', function (Blueprint $table) {
    $table->id();
    $table->uuid('uuid')->unique();
    $table->foreignId('project_id')->constrained()->cascadeOnDelete();
    $table->foreignId('location_id')->nullable()->constrained('master_locations')->nullOnDelete();
    $table->string('task_no'); // 101757
    $table->string('vid'); // BNM26071
    $table->text('task_name')->nullable();
    $table->string('remote_name')->nullable();
    $table->enum('job_type', ['INSTALASI', 'RELOKASI', 'PMCM', 'DISMANTLE', 'SURVEY']);
    $table->enum('stage', [
        'DRAFT', 'ESTIMASI', 'FORWARDED', 'APPROVED',
        'REALISASI', 'VERIFIED', 'RECONCILED', 'REJECTED'
    ])->default('DRAFT');
    $table->foreignId('submitted_by')->constrained('users');
    $table->foreignId('forwarded_by')->nullable()->constrained('users');
    $table->foreignId('approved_by')->nullable()->constrained('users');
    $table->foreignId('verified_by')->nullable()->constrained('users');
    $table->foreignId('reconciled_by')->nullable()->constrained('users');
    $table->bigInteger('total_estimated')->default(0);
    $table->bigInteger('total_revised')->default(0);
    $table->bigInteger('total_approved')->default(0);
    $table->bigInteger('total_realization')->default(0);
    $table->text('rejection_reason')->nullable();
    $table->text('notes')->nullable();
    $table->timestamp('completed_at')->nullable();
    // Sync fields
    $table->string('server_id')->nullable();
    $table->string('device_id')->nullable();
    $table->string('sync_status')->default('PENDING');
    $table->timestamp('last_synced_at')->nullable();
    $table->timestamps();
    $table->softDeletes();
    
    $table->index('stage');
    $table->index('job_type');
    $table->index(['project_id', 'stage']);
    $table->index(['submitted_by', 'stage']);
});
```

**Step 2: Run + commit**

---

### Task 1.4: Create expense_items migration

**Objective:** Per-item within task_expense

**Files:**
- Create: `backend/database/migrations/XXXX_XX_XX_000004_create_expense_items_table.php`

**Step 1: Write migration**

```php
Schema::create('expense_items', function (Blueprint $table) {
    $table->id();
    $table->uuid('uuid')->unique();
    $table->foreignId('task_expense_id')->constrained()->cascadeOnDelete();
    $table->foreignId('template_id')->nullable()->constrained('budget_item_templates')->nullOnDelete();
    $table->date('tanggal');
    $table->text('note')->nullable();
    $table->bigInteger('estimated_amount')->default(0);   // FE input
    $table->bigInteger('revised_amount')->nullable();      // SUPERVISOR edit
    $table->bigInteger('approved_amount')->nullable();     // OWNER final
    $table->bigInteger('realization_amount')->nullable();  // FE realization
    $table->text('bukti_path')->nullable();
    $table->boolean('requires_bill')->default(false);
    $table->boolean('bill_verified')->default(false);
    $table->enum('item_status', ['DRAFT', 'APPROVED', 'REJECTED'])->default('DRAFT');
    $table->text('rejection_reason')->nullable();
    $table->timestamps();
});
```

**Step 2: Run + commit**

---

### Task 1.5: Create task_expense_histories migration (audit trail)

**Objective:** Record every stage transition

**Files:**
- Create: `backend/database/migrations/XXXX_XX_XX_000005_create_task_expense_histories_table.php`

```php
Schema::create('task_expense_histories', function (Blueprint $table) {
    $table->id();
    $table->foreignId('task_expense_id')->constrained()->cascadeOnDelete();
    $table->foreignId('actor_id')->constrained('users');
    $table->string('action'); // submitted, forwarded, approved, rejected, etc
    $table->string('old_stage')->nullable();
    $table->string('new_stage');
    $table->text('notes')->nullable();
    $table->timestamps();
});
```

**Step 2: Run + commit**

---

## Phase 2: Models & Relationships (Backend)

### Task 2.1: BudgetItemTemplate model

**Files:**
- Create: `backend/app/Models/BudgetItemTemplate.php`

```php
class BudgetItemTemplate extends Model
{
    protected $fillable = [
        'uuid', 'category_name', 'category_group', 'pagu_type',
        'pagu_amount', 'requires_bill', 'bill_note', 'is_active',
    ];
    
    public function getRouteKeyName(): string { return 'uuid'; }
    
    public function expenseItems(): HasMany
    {
        return $this->hasMany(ExpenseItem::class, 'template_id');
    }
    
    // Scope: get active templates filtered by job_type
    public function scopeForJobType($query, string $jobType): void
    {
        // FIXED_PAGU templates filter by job_type (INSTALASI vs RELOKASI have different pagu)
        // MANAGER_APPROVAL and TICKET templates apply to ALL job types
    }
}
```

### Task 2.2: MasterLocation model

**Files:**
- Create: `backend/app/Models/MasterLocation.php`

```php
class MasterLocation extends Model
{
    use SoftDeletes;
    
    protected $fillable = [
        'uuid', 'project_id', 'remote_name', 'address',
        'latitude', 'longitude', 'created_by', 'updated_by',
    ];
    
    public function project(): BelongsTo { return $this->belongsTo(Project::class); }
    public function taskExpenses(): HasMany { return $this->hasMany(TaskExpense::class, 'location_id'); }
    
    // Scope: get location history (last N approved task_expenses at this location)
    public function scopeWithHistory($query, int $limit = 5): void
    {
        // Used by OWNER during approval
    }
}
```

### Task 2.3: TaskExpense model

**Files:**
- Create: `backend/app/Models/TaskExpense.php`

Full model with:
- 7 stage constants
- Relationships: submittedBy, forwardedBy, approvedBy, verifiedBy, reconciledBy, items, histories
- Stage transition methods: submit(), forward(), approve(), reject(), realize(), verify(), reconcile()
- Pagu validation: validateItemsAgainstPagu()
- Total calculations: recalculateTotals()
- Location history: getLocationHistory()

### Task 2.4: ExpenseItem and TaskExpenseHistory models

**Files:**
- Create: `backend/app/Models/ExpenseItem.php`
- Create: `backend/app/Models/TaskExpenseHistory.php`

---

## Phase 3: Seeders (Backend)

### Task 3.1: BudgetItemTemplateSeeder

**Objective:** Seed 35 categories with pagu rules

**Files:**
- Create: `backend/database/seeders/BudgetItemTemplateSeeder.php`

Seed all 35 categories with:
```php
// FIXED_PAGU (10 items — amounts vary by job_type)
['category_group' => 'PAKET_LK', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => 120000],
// etc.

// TICKET (12 items)
['category_group' => 'TIKET_PESAWAT_BERANGKAT', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true],

// MANAGER_APPROVAL (13 items)
['category_group' => 'TRANSPORT_TAKSI_BERANGKAT', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null],
```

### Task 3.2: Update StagingSeeder

- Add SUPERVISOR user (email: supervisor@fundsmanager.test, employee_id: 10004)
- Add AUDITOR user (email: auditor@fundsmanager.test, employee_id: 10005)
- Assign roles
- Seed master_locations sample data
- Create sample project assignments

---

## Phase 4: API Endpoints (Backend)

### Task 4.1: MasterLocation CRUD API

**Files:**
- Create: `backend/app/Http/Controllers/Api/MasterLocationController.php`
- Create: `backend/tests/Feature/MasterLocationApiTest.php`

Endpoints:
```
GET    /api/projects/{project}/locations       — List locations (all authenticated)
POST   /api/projects/{project}/locations       — Create (ADMIN, SUPERVISOR)
GET    /api/locations/{uuid}                   — Detail
PUT    /api/locations/{uuid}                   — Update (ADMIN, SUPERVISOR)
DELETE /api/locations/{uuid}                   — Delete (ADMIN, SUPERVISOR)
```

Authorization: ADMIN, SUPERVISOR for write; all roles for read.

### Task 4.2: BudgetItemTemplate API

**Files:**
- Create: `backend/app/Http/Controllers/Api/BudgetItemTemplateController.php`

```
GET /api/budget-templates  — List all active templates (all roles)
GET /api/budget-templates?job_type=INSTALASI  — Filter by job type
```

No write API (admin-managed via seeder/migration).

### Task 4.3: TaskExpense CRUD + Stage Transitions API

**Files:**
- Create: `backend/app/Http/Controllers/Api/TaskExpenseController.php`
- Create: `backend/app/Http/Requests/TaskExpense/StoreTaskExpenseRequest.php`
- Create: `backend/app/Http/Requests/TaskExpense/SubmitTaskExpenseRequest.php`
- Create: `backend/app/Http/Requests/TaskExpense/ForwardTaskExpenseRequest.php`
- Create: `backend/app/Http/Requests/TaskExpense/ApproveTaskExpenseRequest.php`
- Create: `backend/app/Http/Requests/TaskExpense/RealizeTaskExpenseRequest.php`
- Create: `backend/app/Http/Requests/TaskExpense/VerifyTaskExpenseRequest.php`
- Create: `backend/app/Http/Requests/TaskExpense/ReconcileTaskExpenseRequest.php`

Endpoints:
```
GET    /api/task-expenses                        — List (scoped per role)
POST   /api/task-expenses                        — Create draft (FIELD_ENGINEER)
GET    /api/task-expenses/{uuid}                 — Detail with items + history
PUT    /api/task-expenses/{uuid}                 — Update draft (FE only, stage=DRAFT)
DELETE /api/task-expenses/{uuid}                 — Delete draft (FE only, stage=DRAFT)

POST   /api/task-expenses/{uuid}/submit          — FE submit → ESTIMASI
POST   /api/task-expenses/{uuid}/forward         — SUP forward → FORWARDED
POST   /api/task-expenses/{uuid}/approve         — OWNER approve → APPROVED
POST   /api/task-expenses/{uuid}/reject          — Any reject → REJECTED (back to DRAFT)
POST   /api/task-expenses/{uuid}/realize         — FE realize → REALISASI
POST   /api/task-expenses/{uuid}/verify          — ADMIN/FM verify → VERIFIED
POST   /api/task-expenses/{uuid}/reconcile       — FM reconcile → RECONCILED

GET    /api/task-expenses/{uuid}/histories       — Audit trail
GET    /api/projects/{project}/task-expenses     — Per-project listing
GET    /api/locations/{location}/history         — Location budget history (OWNER)
```

Authorization per stage transition:
- `submit`: FIELD_ENGINEER (own form, stage=DRAFT)
- `forward`: SUPERVISOR (stage=ESTIMASI)
- `approve`: OWNER (stage=FORWARDED)
- `reject`: SUPERVISOR or OWNER (stage=ESTIMASI or FORWARDED)
- `realize`: FIELD_ENGINEER (own form, stage=APPROVED)
- `verify`: ADMIN or FINANCE_MANAGER (stage=REALISASI)
- `reconcile`: FINANCE_MANAGER (stage=VERIFIED)

### Task 4.4: Offline Sync API (extend existing)

**Files:**
- Modify: `backend/app/Http/Controllers/Api/SyncController.php` (if exists)

Outbox operations for task_expenses:
```json
{
  "entity": "task_expense",
  "operation": "CREATE | UPDATE | SUBMIT | REALIZE",
  "payload": {
    "uuid": "...",
    "device_id": "...",
    "task_no": "101757",
    "items": [...]
  }
}
```

Server-side: detect conflicts (stage mismatch → return STAGE_CONFLICT)

---

## Phase 5: Tests (Backend)

### Task 5.1: BudgetItemTemplateSeeder test

- Run seeder → verify 35 templates exist
- Verify FIXED_PAGU templates have amounts
- Verify TICKET templates have requires_bill=true

### Task 5.2: TaskExpense stage transition tests

Test each transition:
- FE create draft ✓
- FE submit → SUPERVISOR forward → OWNER approve (happy path)
- FE submit → SUPERVISOR reject (cascade back to DRAFT)
- Pagu enforcement: FIXED_PAGU > pagu → 422 error
- TICKET: warning but allowed
- OWNER approval with location history
- FE realization with offline outbox simulation

---

## Phase 6: Android Implementation

### Task 6.1: Room DB entities

**Files:**
- Create: `app/src/main/java/.../data/local/entity/TaskExpenseEntity.kt`
- Create: `app/src/main/java/.../data/local/entity/ExpenseItemEntity.kt`
- Create: `app/src/main/java/.../data/local/entity/BudgetItemTemplateEntity.kt`
- Create: `app/src/main/java/.../data/local/entity/MasterLocationEntity.kt`
- Create: `app/src/main/java/.../data/local/dao/TaskExpenseDao.kt`
- Create: `app/src/main/java/.../data/local/dao/BudgetTemplateDao.kt`
- Create: `app/src/main/java/.../data/local/dao/MasterLocationDao.kt`

Room DB update: add new entities, migration for Room schema.

### Task 6.2: Data layer (DTOs + Repository)

**Files:**
- Create: DTO classes for API communication
- Create: `TaskExpenseRepository.kt` — cache-first, sync-aware
- Create: `BudgetTemplateRepository.kt` — cached from server
- Create: `MasterLocationRepository.kt` — cached from server

### Task 6.3: Sync integration

- Extend `SyncWorker.kt` to handle task_expense entities
- Extend `SyncOutboxRepository.kt` for task_expense operations
- File upload worker: `BuktiUploadWorker.kt` — separate worker for file uploads

### Task 6.4: Task assignment (via app)

**Files:**
- Create: `app/src/main/java/.../ui/screen/supervisor/AssignTaskScreen.kt`

SUPERVISOR can:
```
┌────────────────────────────────────┐
│  ASSIGN TASK                       │
│                                    │
│  Project: [Tol Kalimantan S3 ▼]   │
│  Task No: [101758]                 │
│  VID:     [BNM26072]              │
│  Job Type:[SURVEY ▼]              │
│  Lokasi:  [BNI ATM Mandonga ▼]    │
│  Assign to:[Mas Rudi ▼]           │
│                                    │
│  [ASSIGN]                          │
└────────────────────────────────────┘
```

FE receives push notification / appears in "My Tasks"

### Task 6.5: FE UI — Form Estimasi (Stage 1)

**Files:**
- Create: `app/src/main/java/.../ui/screen/engineer/BudgetEstimateFormScreen.kt`
- Create: `app/src/main/java/.../ui/screen/engineer/BudgetEstimateViewModel.kt`

Features:
- Offline form with Room auto-save
- Max 5 drafts validation
- Pagu enforcement UI (FIXED: block, TICKET: warning banner, MANAGER_APPROVAL: info)
- Job type dropdown, location dropdown (cached)
- Add/remove items, per-item date + note
- Upload bukti button (queue for later if offline)
- SIMPAN DRAFT / KIRIM KE KORDINATOR

### Task 6.6: FE UI — My Tasks Dashboard

List of tasks grouped by status (Draft, Waiting Review, Approved, Realization needed)

### Task 6.7: FE UI — Realisasi Form (Stage 5)

Same form, different mode: realization columns enabled, bukti upload required per item

### Task 6.8: SUPERVISOR UI — Inbox + Forward

Review FE estimates, edit items, add notes, forward to OWNER or reject back

### Task 6.9: SUPERVISOR UI — Assign Task

Create new task_expense (stage=DRAFT, assigned to FE)

### Task 6.10: OWNER UI — Approval Dashboard

- Inbox approval with location history panel
- Per-item adjustment, total approved
- Approve/Reject

### Task 6.11: ADMIN/FINANCE UI — Verification Dashboard

- Realisasi vs Approved comparison per item
- Bukti viewer
- Bill verification checkbox
- Flag issue

### Task 6.12: FINANCE UI — Reconciliation

- Crosscheck notes
- Ticket without bukti: adjust nominal
- Final close

---

## Offline Handling Summary

| Scenario | Handling |
|----------|----------|
| Create draft offline | Room DB, auto-save setiap perubahan |
| Submit offline | Outbox entry, toast "Menunggu sync" |
| Sync success | Toast "3 terkirim, 0 gagal" |
| Sync failure | Retry 5x, flag error, toast "1 gagal" |
| Conflict (stage mismatch) | Server returns STAGE_CONFLICT, app notifies |
| Upload bukti offline | File queue terpisah, upload worker |
| App crash mid-form | Auto-save → restore on reopen |
| Ganti HP | Server data appears; draft lokal hilang (acceptable) |
| Max 5 drafts | Validation before creating new draft |

---

## Verification Checklist

- [ ] Migration: all 5 new tables created
- [ ] Seeder: 35 budget templates seeded correctly
- [ ] API: CRUD task_expenses with stage transitions
- [ ] API: Authorization per role per stage
- [ ] API: Pagu enforcement (FIXED blocked, TICKET warning, MANAGER_APPROVAL pass)
- [ ] API: Location history for OWNER approval
- [ ] Tests: all stage transitions tested
- [ ] Android: offline form with auto-save
- [ ] Android: sync outbox for task_expenses
- [ ] Android: 5x retry with error flag
- [ ] Android: file upload worker
- [ ] APK build success

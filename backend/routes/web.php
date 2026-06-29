<?php

use App\Http\Controllers\Web\ApprovalController;
use App\Http\Controllers\Web\AuditWebController;
use App\Http\Controllers\Web\AuthWebController;
use App\Http\Controllers\Web\DashboardController;
use App\Http\Controllers\Web\PeriodWebController;
use App\Http\Controllers\Web\ProjectWebController;
use App\Http\Controllers\Web\SyncMonitorController;
use App\Http\Controllers\Web\TransactionWebController;
use App\Http\Controllers\Web\BudgetWebController;
use App\Http\Controllers\Web\SearchController;
use App\Http\Controllers\Web\UserWebController;
use App\Http\Controllers\Web\EquipmentWebController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes — FundManager V2 Dashboard
|--------------------------------------------------------------------------
|
| Session-based auth (Blade + Livewire). Sanctum guards the API.
|
*/

// Guest routes
Route::middleware('guest')->group(function () {
    Route::get('/login', [AuthWebController::class, 'showLogin'])->name('login');
    Route::post('/login', [AuthWebController::class, 'login'])->name('login.post');
});

// Password change (auth required, no other middleware)
Route::middleware('auth')->group(function () {
    Route::get('/password/change', [AuthWebController::class, 'showChangePassword'])->name('password.change');
    Route::post('/password/change', [AuthWebController::class, 'changePassword'])->name('password.update');
});

// Authenticated routes
Route::middleware('auth')->group(function () {
    Route::post('/logout', [AuthWebController::class, 'logout'])->name('logout');

    // Search (all authenticated)
    Route::get('/search', [SearchController::class, 'search'])->name('web.search');

    // Dashboard
    Route::get('/', [DashboardController::class, 'index'])->name('web.dashboard');
    Route::get('/dashboard', [DashboardController::class, 'index']);

    // Projects
    Route::get('/projects', [ProjectWebController::class, 'index'])->name('web.projects.index');
    Route::post('/projects', [ProjectWebController::class, 'store'])->name('web.projects.store');
    Route::patch('/projects/{project}', [ProjectWebController::class, 'update'])->name('web.projects.update');
    Route::patch('/projects/{project}/archive', [ProjectWebController::class, 'update'])->name('web.projects.archive');
    Route::get('/projects/{project}', [ProjectWebController::class, 'show'])->name('web.projects.show');

    // Transactions (read + write)
    Route::get('/transactions', [TransactionWebController::class, 'index'])->name('web.transactions.index');
    Route::get('/transactions/create', [TransactionWebController::class, 'create'])->name('web.transactions.create');
    Route::post('/transactions', [TransactionWebController::class, 'store'])->name('web.transactions.store');
    Route::get('/transactions/{transaction}/edit', [TransactionWebController::class, 'edit'])->name('web.transactions.edit');
    Route::put('/transactions/{transaction}', [TransactionWebController::class, 'update'])->name('web.transactions.update');
    Route::delete('/transactions/{transaction}', [TransactionWebController::class, 'destroy'])->name('web.transactions.destroy');
    Route::get('/transactions/{transaction}', [TransactionWebController::class, 'show'])->name('web.transactions.show');

    // Approval (FINANCE_MANAGER, ADMIN, OWNER)
    Route::middleware('role:OWNER|ADMIN|FINANCE_MANAGER')->group(function () {
        Route::get('/approval', [ApprovalController::class, 'index'])->name('web.approval.index');
        Route::post('/transactions/{transaction}/approve', [ApprovalController::class, 'approve'])->name('web.approval.approve');
        Route::post('/transactions/{transaction}/reject', [ApprovalController::class, 'reject'])->name('web.approval.reject');
        Route::post('/transactions/{transaction}/dispute', [ApprovalController::class, 'dispute'])->name('web.approval.dispute');
        Route::post('/transactions/{transaction}/resolve-dispute', [ApprovalController::class, 'resolveDispute'])->name('web.approval.resolve');
    });

    // Audit Trail (OWNER, ADMIN, AUDITOR, FINANCE_MANAGER)
    Route::middleware('role:OWNER|ADMIN|AUDITOR|FINANCE_MANAGER')->group(function () {
        Route::get('/audit', [AuditWebController::class, 'index'])->name('web.audit.index');
    });

    // Period Management (OWNER, FINANCE_MANAGER)
    Route::middleware('role:OWNER|FINANCE_MANAGER')->group(function () {
        Route::get('/periods', [PeriodWebController::class, 'index'])->name('web.periods.index');
        Route::post('/periods', [PeriodWebController::class, 'store'])->name('web.periods.store');
        Route::post('/periods/{period}/close', [PeriodWebController::class, 'close'])->name('web.periods.close');
        Route::post('/periods/{period}/reopen', [PeriodWebController::class, 'reopen'])->name('web.periods.reopen');
    });

    // User Management (ADMIN, OWNER)
    Route::middleware('role:OWNER|ADMIN')->group(function () {
        Route::get('/users', [UserWebController::class, 'index'])->name('web.users.index');
        Route::post('/users', [UserWebController::class, 'store'])->name('web.users.store');
        Route::patch('/users/{user}', [UserWebController::class, 'update'])->name('web.users.update');
        Route::post('/users/{user}/reset-password', [UserWebController::class, 'resetPassword'])->name('web.users.reset-password');
    });

    // Master Locations (ADMIN, SUPERVISOR)
    Route::middleware('role:OWNER|ADMIN|SUPERVISOR')->group(function () {
        Route::get('/locations', [App\Http\Controllers\Web\LocationWebController::class, 'index'])->name('web.locations.index');
        Route::post('/locations', [App\Http\Controllers\Web\LocationWebController::class, 'store'])->name('web.locations.store');
        Route::patch('/locations/{location}', [App\Http\Controllers\Web\LocationWebController::class, 'update'])->name('web.locations.update');
        Route::delete('/locations/{location}', [App\Http\Controllers\Web\LocationWebController::class, 'destroy'])->name('web.locations.destroy');
    });

    // Budget Request Workflow (Web)
    Route::middleware('role:SUPERVISOR')->group(function () {
        Route::get('/budget/inbox', [App\Http\Controllers\Web\BudgetWebController::class, 'inbox'])->name('web.budget.inbox');
        Route::post('/budget/{task}/forward', [App\Http\Controllers\Web\BudgetWebController::class, 'forward'])->name('web.budget.forward');
    });
    Route::middleware('role:SUPERVISOR|OWNER')->group(function () {
        Route::post('/budget/{task}/reject', [App\Http\Controllers\Web\BudgetWebController::class, 'reject'])->name('web.budget.reject');
    });
    Route::middleware('role:OWNER')->group(function () {
        Route::get('/budget/approval', [App\Http\Controllers\Web\BudgetWebController::class, 'approvalList'])->name('web.budget.approval');
        Route::post('/budget/{task}/approve', [App\Http\Controllers\Web\BudgetWebController::class, 'approve'])->name('web.budget.approve');
    });
    Route::middleware('role:ADMIN|FINANCE_MANAGER')->group(function () {
        Route::get('/budget/verification', [App\Http\Controllers\Web\BudgetWebController::class, 'verificationList'])->name('web.budget.verification');
        Route::post('/budget/{task}/verify', [App\Http\Controllers\Web\BudgetWebController::class, 'verify'])->name('web.budget.verify');
    });
    Route::middleware('role:FINANCE_MANAGER')->group(function () {
        Route::post('/budget/{task}/reconcile', [App\Http\Controllers\Web\BudgetWebController::class, 'reconcile'])->name('web.budget.reconcile');
    });

    // Laporan Pekerjaan (FIELD_ENGINEER)
    Route::middleware('role:FIELD_ENGINEER')->group(function () {
        Route::get('/laporan', [App\Http\Controllers\Web\BudgetWebController::class, 'laporanForm'])->name('web.laporan.form');
        Route::post('/laporan', [App\Http\Controllers\Web\BudgetWebController::class, 'storeLaporan'])->name('web.laporan.store');
    });

    // Budget CRUD (FIELD_ENGINEER)
    Route::middleware('role:FIELD_ENGINEER')->group(function () {
        Route::get('/budget', [App\Http\Controllers\Web\BudgetWebController::class, 'index'])->name('web.budget.index');
        Route::get('/budget/create', [App\Http\Controllers\Web\BudgetWebController::class, 'create'])->name('web.budget.create');
        Route::post('/budget', [App\Http\Controllers\Web\BudgetWebController::class, 'store'])->name('web.budget.store');
        Route::get('/budget/{task}/edit', [App\Http\Controllers\Web\BudgetWebController::class, 'edit'])->name('web.budget.edit');
        Route::put('/budget/{task}', [App\Http\Controllers\Web\BudgetWebController::class, 'update'])->name('web.budget.update');
        Route::delete('/budget/{task}', [App\Http\Controllers\Web\BudgetWebController::class, 'destroy'])->name('web.budget.destroy');
        Route::get('/budget/{task}/realize', [App\Http\Controllers\Web\BudgetWebController::class, 'realize'])->name('web.budget.realize');
        Route::post('/budget/{task}/realize', [App\Http\Controllers\Web\BudgetWebController::class, 'storeRealization'])->name('web.budget.realize-store');
    });

    // Equipment Options (ADMIN, SUPERVISOR)
    Route::middleware('role:ADMIN|SUPERVISOR')->group(function () {
        Route::get('/equipment', [App\Http\Controllers\Web\EquipmentWebController::class, 'index'])->name('web.equipment.index');
        Route::post('/equipment', [App\Http\Controllers\Web\EquipmentWebController::class, 'store'])->name('web.equipment.store');
        Route::delete('/equipment/{option}', [App\Http\Controllers\Web\EquipmentWebController::class, 'destroy'])->name('web.equipment.destroy');
    });

    // Sync Monitor (ADMIN, OWNER)
    Route::middleware('role:OWNER|ADMIN')->group(function () {
        Route::get('/sync', [SyncMonitorController::class, 'index'])->name('web.sync.monitor');
    });

    // ──────────────────────────────────────────
    // Budget Workflow Routes
    // ──────────────────────────────────────────

    // Supervisor Inbox (SUPERVISOR only)
    Route::middleware('role:SUPERVISOR')->group(function () {
        Route::get('/budget/inbox', [BudgetWebController::class, 'inbox'])->name('web.budget.inbox');
        Route::post('/budget/{task}/forward', [BudgetWebController::class, 'forward'])->name('web.budget.forward');
    });

    // Reject (SUPERVISOR or OWNER)
    Route::middleware('role:SUPERVISOR|OWNER')->group(function () {
        Route::post('/budget/{task}/reject', [BudgetWebController::class, 'reject'])->name('web.budget.reject');
    });

    // Owner Approval (OWNER only)
    Route::middleware('role:OWNER')->group(function () {
        Route::get('/budget/approval', [BudgetWebController::class, 'approvalList'])->name('web.budget.approval');
        Route::post('/budget/{task}/approve', [BudgetWebController::class, 'approve'])->name('web.budget.approve');
    });

    // Finance Verification (ADMIN or FINANCE_MANAGER)
    Route::middleware('role:ADMIN|FINANCE_MANAGER')->group(function () {
        Route::get('/budget/verification', [BudgetWebController::class, 'verificationList'])->name('web.budget.verification');
        Route::post('/budget/{task}/verify', [BudgetWebController::class, 'verify'])->name('web.budget.verify');
    });

    // Reconcile (FINANCE_MANAGER only)
    Route::middleware('role:FINANCE_MANAGER')->group(function () {
        Route::post('/budget/{task}/reconcile', [BudgetWebController::class, 'reconcile'])->name('web.budget.reconcile');
    });
});

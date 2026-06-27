<?php

use App\Http\Controllers\Web\ApprovalController;
use App\Http\Controllers\Web\AuditWebController;
use App\Http\Controllers\Web\AuthWebController;
use App\Http\Controllers\Web\DashboardController;
use App\Http\Controllers\Web\PeriodWebController;
use App\Http\Controllers\Web\ProjectWebController;
use App\Http\Controllers\Web\SyncMonitorController;
use App\Http\Controllers\Web\TransactionWebController;
use App\Http\Controllers\Web\SearchController;
use App\Http\Controllers\Web\UserWebController;
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

    // Transactions (read)
    Route::get('/transactions', [TransactionWebController::class, 'index'])->name('web.transactions.index');
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

    // Sync Monitor (ADMIN, OWNER)
    Route::middleware('role:OWNER|ADMIN')->group(function () {
        Route::get('/sync', [SyncMonitorController::class, 'index'])->name('web.sync.monitor');
    });
});

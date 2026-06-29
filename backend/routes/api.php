<?php

use App\Http\Controllers\Api\AttachmentController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\BudgetItemTemplateController;
use App\Http\Controllers\Api\CrashReportController;
use App\Http\Controllers\Api\DeviceController;
use App\Http\Controllers\Api\MasterEquipmentOptionController;
use App\Http\Controllers\Api\MasterLocationController;
use App\Http\Controllers\Api\PeriodController;
use App\Http\Controllers\Api\ProjectController;
use App\Http\Controllers\Api\SyncChangesController;
use App\Http\Controllers\Api\SyncPushController;
use App\Http\Controllers\Api\SyncStatusController;
use App\Http\Controllers\Api\TaskExpenseController;
use App\Http\Controllers\Api\TransactionController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes — FundManager V2
|--------------------------------------------------------------------------
|
| Sanctum token-based authentication for mobile clients.
| Session-based auth for web (Blade + Livewire) routes lives in web.php.
|
*/

// Auth — no middleware
Route::prefix('v1/auth')->group(function () {
    Route::post('/login', [AuthController::class, 'login']);
});

// Auth — Sanctum protected
Route::prefix('v1/auth')->middleware('auth:sanctum')->group(function () {
    Route::get('/me', [AuthController::class, 'me']);
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::post('/change-password', [AuthController::class, 'changePassword']);
});

// Projects — Sanctum protected
Route::prefix('v1/projects')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [ProjectController::class, 'index']);
    Route::post('/', [ProjectController::class, 'store']);
    Route::patch('/{project}', [ProjectController::class, 'update']);
    Route::post('/{project}/assignments', [ProjectController::class, 'assign']);
    Route::get('/{project}/summary', [ProjectController::class, 'summary']);
    Route::get('/{project}/export', [ProjectController::class, 'export']);
});

// Transactions — Sanctum protected
Route::prefix('v1/transactions')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [TransactionController::class, 'index']);
    Route::post('/', [TransactionController::class, 'store']);
    Route::get('/{transaction}', [TransactionController::class, 'show']);
    Route::post('/{transaction}/submit', [TransactionController::class, 'submit']);
    Route::post('/{transaction}/approve', [TransactionController::class, 'approve']);
    Route::post('/{transaction}/reject', [TransactionController::class, 'reject']);
    Route::post('/{transaction}/void', [TransactionController::class, 'void']);
    Route::post('/{transaction}/correction', [TransactionController::class, 'correction']);
    Route::post('/{transaction}/dispute', [TransactionController::class, 'dispute']);
    Route::post('/{transaction}/resolve-dispute', [TransactionController::class, 'resolveDispute']);
    Route::post('/{transaction}/attachments', [AttachmentController::class, 'store']);
});

// Attachments — Sanctum protected
Route::prefix('v1/attachments')->middleware('auth:sanctum')->group(function () {
    Route::get('/{attachment}', [AttachmentController::class, 'show']);
});

// Devices — Sanctum protected
Route::prefix('v1/devices')->middleware('auth:sanctum')->group(function () {
    Route::post('/register', [DeviceController::class, 'register']);
});

// Periods — Sanctum protected
Route::prefix('v1/periods')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [PeriodController::class, 'index']);
    Route::post('/{period}/close', [PeriodController::class, 'close']);
    Route::post('/{period}/reopen', [PeriodController::class, 'reopen']);
});

// Sync — Sanctum protected
Route::prefix('v1/sync')->middleware('auth:sanctum')->group(function () {
    Route::post('/push', [SyncPushController::class, 'push']);
    Route::get('/changes', [SyncChangesController::class, 'changes']);
    Route::get('/status', [SyncStatusController::class, 'status']);
});

// Health check (public)
Route::get('/health', fn () => response()->json(['status' => 'ok']));

Route::post('/v1/crash-reports', [CrashReportController::class, 'store']);

// Budget Item Templates — Sanctum protected
Route::prefix('v1/budget-templates')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [BudgetItemTemplateController::class, 'index']);
});

// Equipment Options — Sanctum protected
Route::prefix('v1/equipment-options')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [MasterEquipmentOptionController::class, 'index']);
});

// Master Locations — Sanctum protected
Route::prefix('v1')->middleware('auth:sanctum')->group(function () {
    Route::get('/projects/{project}/locations', [MasterLocationController::class, 'index']);
    Route::post('/projects/{project}/locations', [MasterLocationController::class, 'store']);
    Route::get('/locations/{location}', [MasterLocationController::class, 'show']);
    Route::put('/locations/{location}', [MasterLocationController::class, 'update']);
    Route::delete('/locations/{location}', [MasterLocationController::class, 'destroy']);
    Route::get('/locations/{location}/history', [MasterLocationController::class, 'history']);
});

// Task Expenses — Sanctum protected
Route::prefix('v1/task-expenses')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [TaskExpenseController::class, 'index']);
    Route::post('/', [TaskExpenseController::class, 'store']);
    Route::get('/{taskExpense}', [TaskExpenseController::class, 'show']);
    Route::put('/{taskExpense}', [TaskExpenseController::class, 'update']);
    Route::delete('/{taskExpense}', [TaskExpenseController::class, 'destroy']);
    // Stage transitions
    Route::post('/{taskExpense}/submit', [TaskExpenseController::class, 'submit']);
    Route::post('/{taskExpense}/forward', [TaskExpenseController::class, 'forward']);
    Route::post('/{taskExpense}/approve', [TaskExpenseController::class, 'approve']);
    Route::post('/{taskExpense}/reject', [TaskExpenseController::class, 'reject']);
    Route::post('/{taskExpense}/realize', [TaskExpenseController::class, 'realize']);
    Route::post('/{taskExpense}/verify', [TaskExpenseController::class, 'verify']);
    Route::post('/{taskExpense}/reconcile', [TaskExpenseController::class, 'reconcile']);
    Route::get('/{taskExpense}/histories', [TaskExpenseController::class, 'histories']);
});

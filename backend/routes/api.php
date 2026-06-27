<?php

use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\DeviceController;
use App\Http\Controllers\Api\ProjectController;
use App\Http\Controllers\Api\SyncPushController;
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
});

// Projects — Sanctum protected
Route::prefix('v1/projects')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [ProjectController::class, 'index']);
    Route::post('/', [ProjectController::class, 'store']);
    Route::patch('/{project}', [ProjectController::class, 'update']);
    Route::post('/{project}/assignments', [ProjectController::class, 'assign']);
});

// Transactions — Sanctum protected
Route::prefix('v1/transactions')->middleware('auth:sanctum')->group(function () {
    Route::get('/', [TransactionController::class, 'index']);
    Route::post('/', [TransactionController::class, 'store']);
    Route::get('/{transaction}', [TransactionController::class, 'show']);
});

// Devices — Sanctum protected
Route::prefix('v1/devices')->middleware('auth:sanctum')->group(function () {
    Route::post('/register', [DeviceController::class, 'register']);
});

// Sync push — Sanctum protected
Route::prefix('v1/sync')->middleware('auth:sanctum')->group(function () {
    Route::post('/push', [SyncPushController::class, 'push']);
});

// Health check (public)
Route::get('/health', fn () => response()->json(['status' => 'ok']));

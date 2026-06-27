<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AccountingPeriod;
use App\Models\AuditEvent;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;

class PeriodController extends Controller
{
    public function index(): JsonResponse
    {
        $periods = AccountingPeriod::orderByDesc('period_start')->get();

        return response()->json([
            'periods' => $periods,
        ]);
    }

    public function close(AccountingPeriod $period, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'FINANCE_MANAGER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER or FINANCE_MANAGER can close periods.'],
            ]);
        }

        if ($period->status === 'CLOSED') {
            throw ValidationException::withMessages([
                'status' => ['Period is already closed.'],
            ]);
        }

        $period->update([
            'status' => 'CLOSED',
            'closed_by' => $user->id,
            'closed_at' => now(),
            'reason' => $request->input('reason'),
        ]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'accounting_period',
            'entity_uuid' => $period->uuid,
            'action' => 'close_period',
            'old_value' => ['status' => 'OPEN'],
            'new_value' => ['status' => 'CLOSED'],
            'reason' => $request->input('reason'),
        ]);

        return response()->json([
            'period' => $period,
            'message' => 'Period closed.',
        ]);
    }

    public function reopen(AccountingPeriod $period, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'FINANCE_MANAGER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER or FINANCE_MANAGER can reopen periods.'],
            ]);
        }

        if ($period->status !== 'CLOSED') {
            throw ValidationException::withMessages([
                'status' => ['Only closed periods can be reopened.'],
            ]);
        }

        $period->update([
            'status' => 'OPEN',
            'reopened_by' => $user->id,
            'reopened_at' => now(),
        ]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'accounting_period',
            'entity_uuid' => $period->uuid,
            'action' => 'reopen_period',
            'old_value' => ['status' => 'CLOSED'],
            'new_value' => ['status' => 'OPEN'],
            'reason' => $request->input('reason'),
        ]);

        return response()->json([
            'period' => $period,
            'message' => 'Period reopened.',
        ]);
    }
}

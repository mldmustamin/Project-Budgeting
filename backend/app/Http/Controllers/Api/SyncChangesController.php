<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\ProjectAssignment;
use App\Models\Transaction;
use Carbon\Carbon;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class SyncChangesController extends Controller
{
    use ValidatesSyncDevice;

    public function changes(Request $request): JsonResponse
    {
        $device = $this->validateDevice($request);
        if ($device instanceof JsonResponse) {
            return $device;
        }

        $user = $request->user();
        $since = $request->query('since');

        $query = Transaction::with('project:id,uuid,name');

        // Scoping: OWNER/ADMIN see all, others see assigned projects only
        if (! $user->hasRole(['OWNER', 'ADMIN'])) {
            $assignedProjectIds = ProjectAssignment::where('user_id', $user->id)
                ->pluck('project_id');
            $query->whereIn('project_id', $assignedProjectIds);
        }

        // Since cursor — parse ISO string for proper SQL comparison
        if ($since) {
            $sinceDate = Carbon::parse($since);
            $query->where('updated_at', '>', $sinceDate);
        }

        $transactions = $query->orderBy('updated_at')->get();

        $changes = $transactions->map(fn (Transaction $tx) => [
            'uuid' => $tx->uuid,
            'project_uuid' => $tx->project_uuid,
            'user_uuid' => $tx->user_uuid,
            'type' => $tx->type,
            'date' => $tx->date?->format('Y-m-d'),
            'description' => $tx->description,
            'reported_amount' => $tx->reported_amount,
            'real_amount' => $tx->real_amount,
            'note' => $tx->note,
            'source_text' => $tx->source_text,
            'approval_status' => $tx->approval_status,
            'finance_status' => $tx->finance_status,
            'sync_status' => $tx->sync_status,
            'updated_at' => $tx->updated_at?->toIso8601String(),
            'deleted_at' => $tx->deleted_at?->toIso8601String(),
        ]);

        $lastChange = $changes->last();
        $nextCursor = $lastChange
            ? ($lastChange['updated_at'] ?? $since ?? now()->toIso8601String())
            : ($since ?? now()->toIso8601String());

        return response()->json([
            'server_time' => now()->toIso8601String(),
            'next_cursor' => $nextCursor,
            'changes' => [
                'transactions' => $changes->values()->toArray(),
            ],
        ]);
    }
}
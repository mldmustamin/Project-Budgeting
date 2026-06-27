<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\SyncOutbox;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class SyncStatusController extends Controller
{
    use ValidatesSyncDevice;

    public function status(Request $request): JsonResponse
    {
        $device = $this->validateDevice($request);
        if ($device instanceof JsonResponse) {
            return $device;
        }

        $user = $request->user();

        $lastSyncedRaw = SyncOutbox::where('user_id', $user->id)
            ->where('device_id', $device->uuid)
            ->max('last_synced_at');

        return response()->json([
            'device_uuid' => $device->uuid,
            'is_revoked' => $device->is_revoked,
            'last_active_at' => $device->last_active_at?->toIso8601String(),
            'pending_outbox_count' => SyncOutbox::where('user_id', $user->id)
                ->where('device_id', $device->uuid)
                ->where('status', 'PENDING')
                ->count(),
            'rejected_outbox_count' => SyncOutbox::where('user_id', $user->id)
                ->where('device_id', $device->uuid)
                ->where('status', 'REJECTED')
                ->count(),
            'last_synced_at' => $lastSyncedRaw
                ? \Illuminate\Support\Carbon::parse($lastSyncedRaw)->toIso8601String()
                : null,
        ]);
    }
}
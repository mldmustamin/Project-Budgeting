<?php

namespace App\Http\Controllers\Api;

use App\Models\Device;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

trait ValidatesSyncDevice
{
    /**
     * Validate device_uuid for sync endpoints.
     * Returns Device model on success, or JsonResponse error on failure.
     */
    protected function validateDevice(Request $request): Device|JsonResponse
    {
        $deviceUuid = $request->input('device_uuid');

        if (! $deviceUuid) {
            return response()->json([
                'error' => 'MISSING_DEVICE_UUID',
                'message' => 'device_uuid query parameter is required.',
            ], 422);
        }

        $device = Device::where('uuid', $deviceUuid)
            ->where('user_id', $request->user()->id)
            ->first();

        if (! $device) {
            return response()->json([
                'error' => 'DEVICE_NOT_REGISTERED',
                'message' => 'Device not registered or does not belong to this user.',
            ], 401);
        }

        if ($device->is_revoked) {
            return response()->json([
                'error' => 'DEVICE_REVOKED',
                'message' => 'Device has been revoked.',
            ], 403);
        }

        return $device;
    }
}
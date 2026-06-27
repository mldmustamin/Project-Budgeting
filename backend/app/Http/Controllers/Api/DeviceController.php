<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Device;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class DeviceController extends Controller
{
    public function register(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'device_name' => ['required', 'string', 'max:255'],
            'device_platform' => ['required', 'string', 'max:100'],
            'device_version' => ['nullable', 'string', 'max:50'],
            'device_uuid' => ['nullable', 'uuid'],
        ]);

        $deviceUuid = $validated['device_uuid'] ?? (string) Str::uuid();

        $device = Device::updateOrCreate(
            ['uuid' => $deviceUuid],
            [
                'user_id' => $request->user()->id,
                'device_name' => $validated['device_name'],
                'device_platform' => $validated['device_platform'],
                'device_version' => $validated['device_version'] ?? null,
                'last_active_at' => now(),
                'is_revoked' => false,
            ]
        );

        return response()->json([
            'device' => [
                'uuid' => $device->uuid,
                'user_uuid' => $request->user()->uuid,
                'device_name' => $device->device_name,
                'device_platform' => $device->device_platform,
                'is_revoked' => $device->is_revoked,
                'last_active_at' => $device->last_active_at,
            ],
        ], $device->wasRecentlyCreated ? 201 : 200);
    }
}
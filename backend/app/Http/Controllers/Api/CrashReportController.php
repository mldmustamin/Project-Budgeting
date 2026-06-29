<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class CrashReportController extends Controller
{
    /**
     * Receive crash report from Android app.
     * POST /api/v1/crash-reports
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'report' => ['required', 'string', 'max:50000'],
            'device_model' => ['nullable', 'string', 'max:100'],
            'android_version' => ['nullable', 'string', 'max:20'],
            'app_version' => ['nullable', 'string', 'max:50'],
        ]);

        Log::channel('crash')->error('Android Crash Report', [
            'device_model' => $validated['device_model'] ?? 'unknown',
            'android_version' => $validated['android_version'] ?? 'unknown',
            'app_version' => $validated['app_version'] ?? 'unknown',
            'report' => $validated['report'],
        ]);

        return response()->json(['message' => 'Crash report received'], 201);
    }
}

<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\Device;
use App\Models\SyncOutbox;
use Illuminate\View\View;

class SyncMonitorController extends Controller
{
    public function index(): View
    {
        $devices = Device::with('user:id,name,email')
            ->orderByDesc('last_active_at')
            ->get()
            ->map(function ($device) {
                $device->pending_count = SyncOutbox::where('device_id', $device->uuid)
                    ->where('status', 'PENDING')
                    ->count();
                $device->rejected_count = SyncOutbox::where('device_id', $device->uuid)
                    ->where('status', 'REJECTED')
                    ->count();
                return $device;
            });

        return view('web.sync.monitor', compact('devices'));
    }
}

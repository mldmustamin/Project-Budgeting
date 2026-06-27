@extends('layouts.app')

@section('title', 'Sync Monitor')

@section('content')
<div class="bg-white rounded-xl border border-gray-200">
    <div class="px-5 py-4 border-b border-gray-100">
        <h3 class="text-sm font-semibold text-gray-800">Device Sync Status ({{ $devices->count() }})</h3>
    </div>
    <div class="overflow-x-auto">
        <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left">
                <tr>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Device</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">User</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Platform</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Status</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Last Active</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Pending</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Rejected</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($devices as $device)
                <tr class="hover:bg-gray-50">
                    <td class="px-5 py-3 text-gray-900 font-medium">{{ $device->device_name }}</td>
                    <td class="px-5 py-3 text-gray-600">{{ $device->user?->name ?? '-' }}</td>
                    <td class="px-5 py-3 text-gray-600">{{ $device->device_platform }}</td>
                    <td class="px-5 py-3">
                        @if($device->is_revoked)
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">Revoked</span>
                        @else
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Active</span>
                        @endif
                    </td>
                    <td class="px-5 py-3 text-gray-500 whitespace-nowrap">{{ $device->last_active_at?->diffForHumans() ?? '-' }}</td>
                    <td class="px-5 py-3">
                        @if($device->pending_count > 0)
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800">{{ $device->pending_count }}</span>
                        @else
                            <span class="text-gray-400">0</span>
                        @endif
                    </td>
                    <td class="px-5 py-3">
                        @if($device->rejected_count > 0)
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">{{ $device->rejected_count }}</span>
                        @else
                            <span class="text-gray-400">0</span>
                        @endif
                    </td>
                </tr>
                @empty
                <tr>
                    <td colspan="7" class="px-5 py-8 text-center text-gray-400">Belum ada device terdaftar.</td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>
</div>
@endsection

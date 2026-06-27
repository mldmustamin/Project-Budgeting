@extends('layouts.app')

@section('title', 'Dashboard')

@section('content')
<div class="space-y-6">
    {{-- Welcome + Status --}}
    <div class="flex items-center justify-between mb-2">
        <div>
            <h2 class="text-xl font-bold text-gray-900">Selamat datang, {{ auth()->user()?->name }}</h2>
            <p class="text-sm text-gray-500 mt-0.5">Ringkasan keuangan semua project</p>
        </div>
        <div class="flex items-center gap-2 px-3 py-1.5 bg-{{ $netPosition >= 0 ? 'brand' : 'red' }}-50 rounded-full border border-{{ $netPosition >= 0 ? 'brand' : 'red' }}-100">
            <div class="w-1.5 h-1.5 rounded-full bg-{{ $netPosition >= 0 ? 'brand' : 'red' }}-500"></div>
            <span class="text-xs font-semibold text-{{ $netPosition >= 0 ? 'brand' : 'red' }}-700">{{ $netPosition >= 0 ? 'Surplus' : 'Defisit' }}</span>
        </div>
    </div>

    {{-- Primary Stat Cards --}}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <x-web-stat-card label="Dana Masuk" :value="$totalFundIn" color="green">
            <x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></x-slot:icon>
        </x-web-stat-card>
        <x-web-stat-card label="Kas Keluar" :value="$totalCashOut" color="red">
            <x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2z"/></x-slot:icon>
        </x-web-stat-card>
        <x-web-stat-card label="Posisi Bersih" :value="$netPosition" color="indigo">
            <x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/></x-slot:icon>
        </x-web-stat-card>
        <x-web-stat-card label="Pending" :value="$pendingApprovalCount" color="amber" :isCount="true">
            <x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></x-slot:icon>
        </x-web-stat-card>
    </div>

    {{-- Secondary Stats --}}
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm">
            <div class="flex items-center gap-2 mb-3">
                <div class="w-8 h-8 bg-blue-50 rounded-lg flex items-center justify-center">
                    <svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
                </div>
                <span class="text-xs font-bold text-gray-400 uppercase tracking-wider">Office (Real)</span>
            </div>
            <p class="text-2xl font-extrabold text-gray-900 tracking-tight">Rp{{ number_format($totalOfficeReal, 0, ',', '.') }}</p>
        </div>
        <div class="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm">
            <div class="flex items-center gap-2 mb-3">
                <div class="w-8 h-8 bg-orange-50 rounded-lg flex items-center justify-center">
                    <svg class="w-4 h-4 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
                </div>
                <span class="text-xs font-bold text-gray-400 uppercase tracking-wider">Personal</span>
            </div>
            <p class="text-2xl font-extrabold text-gray-900 tracking-tight">Rp{{ number_format($totalPersonal, 0, ',', '.') }}</p>
        </div>
    </div>

    {{-- Quick Links --}}
    <div class="flex gap-2">
        @if(auth()->user()?->hasRole(['OWNER','ADMIN','FINANCE_MANAGER']))
        <a href="{{ route('web.approval.index') }}" class="text-xs px-3 py-2 bg-amber-50 text-amber-700 rounded-xl hover:bg-amber-100 font-medium border border-amber-200 transition-colors">
            ⏳ Approval Queue
        </a>
        @endif
        <a href="{{ route('web.transactions.index') }}" class="text-xs px-3 py-2 bg-brand-50 text-brand-700 rounded-xl hover:bg-brand-100 font-medium border border-brand-200 transition-colors">
            📋 Transaksi
        </a>
        <a href="{{ route('web.projects.index') }}" class="text-xs px-3 py-2 bg-gray-50 text-gray-600 rounded-xl hover:bg-gray-100 font-medium border border-gray-200 transition-colors">
            📁 Project
        </a>
    </div>

    {{-- Recent Audit --}}
    <div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div class="px-5 py-3 border-b border-gray-100 flex items-center justify-between">
            <h3 class="text-sm font-semibold text-gray-700">Aktivitas Terbaru</h3>
            <span class="text-[11px] text-gray-400 font-medium">{{ $recentAuditEvents->count() }} event</span>
        </div>
        <div class="overflow-x-auto">
            <table class="w-full text-sm">
                <thead class="bg-gray-50 text-left">
                    <tr>
                        <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Waktu</th>
                        <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">User</th>
                        <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Action</th>
                        <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Entity</th>
                        <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">UUID</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                    @forelse($recentAuditEvents as $event)
                    <tr class="hover:bg-gray-50">
                        <td class="px-5 py-3 text-gray-600 whitespace-nowrap">{{ $event->created_at->diffForHumans() }}</td>
                        <td class="px-5 py-3 text-gray-900">{{ $event->user?->name ?? '-' }}</td>
                        <td class="px-5 py-3">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium
                                @if($event->action === 'approve') bg-green-100 text-green-800
                                @elseif($event->action === 'reject') bg-red-100 text-red-800
                                @elseif($event->action === 'submit') bg-blue-100 text-blue-800
                                @else bg-gray-100 text-gray-700 @endif">
                                {{ $event->action }}
                            </span>
                        </td>
                        <td class="px-5 py-3 text-gray-600">{{ $event->entity_type }}</td>
                        <td class="px-5 py-3 text-gray-500 font-mono text-xs">{{ \Illuminate\Support\Str::limit($event->entity_uuid, 12, '') }}</td>
                    </tr>
                    @empty
                    <tr>
                        <td colspan="5" class="px-5 py-8 text-center text-gray-400">Belum ada aktivitas audit.</td>
                    </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>
</div>
@endsection

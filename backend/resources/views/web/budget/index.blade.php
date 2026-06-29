@extends('layouts.app')

@section('title', 'Budget Saya')

@section('content')
<div class="flex items-center justify-between mb-4">
    <div class="flex items-center gap-3">
        <h2 class="text-lg font-semibold text-gray-800">Budget Saya</h2>
        @if(($draftCount ?? 0) > 0)
            <span class="px-2 py-0.5 bg-amber-100 text-amber-700 text-xs rounded-full font-medium">{{ $draftCount }} Draft</span>
        @endif
    </div>
    <a href="{{ route('web.budget.create') }}" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">+ Buat Budget</a>
</div>

<div class="space-y-4">
    @forelse($tasks as $task)
    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <div class="flex items-start justify-between mb-3">
            <div>
                <span class="text-sm font-bold text-gray-900">#{{ $task->task_no }}</span>
                <span class="ml-2 px-2 py-0.5 rounded-full text-xs font-medium
                    @if($task->stage === 'DRAFT') bg-gray-100 text-gray-600
                    @elseif($task->stage === 'ESTIMASI') bg-blue-100 text-blue-700
                    @elseif($task->stage === 'FORWARDED') bg-amber-100 text-amber-700
                    @elseif($task->stage === 'APPROVED') bg-green-100 text-green-700
                    @elseif($task->stage === 'REALISASI') bg-purple-100 text-purple-700
                    @elseif($task->stage === 'VERIFIED') bg-indigo-100 text-indigo-700
                    @elseif($task->stage === 'RECONCILED') bg-teal-100 text-teal-700
                    @else bg-gray-100 text-gray-600
                    @endif">{{ $task->stage }}</span>
            </div>
            <span class="text-xs text-gray-400">{{ $task->created_at->diffForHumans() }}</span>
        </div>

        <div class="grid grid-cols-3 gap-2 text-sm mb-3">
            <div><span class="text-gray-500">VID:</span> {{ $task->vid }}</div>
            <div><span class="text-gray-500">Jenis:</span> {{ $task->job_type }}</div>
            <div><span class="text-gray-500">Lokasi:</span> {{ $task->location?->remote_name ?? '-' }}</div>
            <div class="col-span-3"><span class="text-gray-500">Total Estimasi:</span> <strong>Rp {{ number_format($task->total_estimated, 0, ',', '.') }}</strong></div>
        </div>

        @if($task->items->isNotEmpty())
        <div class="mb-3">
            <table class="w-full text-xs border border-gray-100 rounded-lg overflow-hidden">
                <thead class="bg-gray-50"><tr><th class="px-2 py-1 text-left">Kategori</th><th class="px-2 py-1 text-right">Estimasi</th><th class="px-2 py-1 text-right">Realisasi</th></tr></thead>
                <tbody class="divide-y divide-gray-100">
                    @foreach($task->items as $item)
                    <tr>
                        <td class="px-2 py-1">{{ $item->template?->category_name ?? '-' }}</td>
                        <td class="px-2 py-1 text-right">Rp{{ number_format($item->estimated_amount, 0, ',', '.') }}</td>
                        <td class="px-2 py-1 text-right">Rp{{ number_format($item->realization_amount ?? 0, 0, ',', '.') }}</td>
                    </tr>
                    @endforeach
                </tbody>
            </table>
        </div>
        @endif

        <div class="flex gap-2">
            @if(in_array($task->stage, ['DRAFT', 'ESTIMASI']))
                <a href="{{ route('web.budget.edit', $task) }}" class="px-3 py-1.5 bg-gray-100 text-gray-700 text-xs rounded-lg hover:bg-gray-200 font-medium">✏️ Edit</a>
            @endif
            @if($task->stage === 'APPROVED')
                <a href="{{ route('web.budget.realize', $task) }}" class="px-3 py-1.5 bg-brand-600 text-white text-xs rounded-lg hover:bg-brand-700 font-medium">💰 Realisasi</a>
            @endif
            @if($task->stage === 'REALISASI')
                <span class="px-3 py-1.5 bg-purple-50 text-purple-600 text-xs rounded-lg font-medium">⏳ Menunggu Verifikasi</span>
            @endif
            @if($task->stage === 'VERIFIED')
                <span class="px-3 py-1.5 bg-indigo-50 text-indigo-600 text-xs rounded-lg font-medium">✅ Terverifikasi</span>
            @endif
            @if($task->stage === 'RECONCILED')
                <span class="px-3 py-1.5 bg-teal-50 text-teal-600 text-xs rounded-lg font-medium">🔒 Terekonsiliasi</span>
            @endif
            @if($task->stage === 'REJECTED')
                <span class="px-3 py-1.5 bg-red-50 text-red-600 text-xs rounded-lg font-medium">❌ Ditolak: {{ $task->rejection_reason }}</span>
            @endif
        </div>
    </div>
    @empty
    <div class="bg-white rounded-xl border border-gray-200 p-8 text-center">
        <p class="text-gray-400 mb-4">Belum ada budget. Mulai buat estimasi baru.</p>
        <a href="{{ route('web.budget.create') }}" class="inline-block px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">+ Buat Budget</a>
    </div>
    @endforelse
</div>
<div class="mt-4">{{ $tasks->links() }}</div>
@endsection

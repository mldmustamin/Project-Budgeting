@extends('layouts.app')
@section('title', 'Budget Inbox — Kordinator')
@section('content')
<h2 class="text-lg font-semibold text-gray-800 mb-4">Budget Inbox — Review Estimasi</h2>

<div class="space-y-4">
    @forelse($tasks as $task)
    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <div class="flex items-start justify-between mb-3">
            <div>
                <span class="text-sm font-bold text-gray-900">#{{ $task->task_no }}</span>
                <span class="ml-2 px-2 py-0.5 rounded-full text-xs font-medium
                    @if($task->stage === 'ESTIMASI') bg-blue-100 text-blue-700
                    @elseif($task->stage === 'FORWARDED') bg-amber-100 text-amber-700
                    @endif">{{ $task->stage }}</span>
            </div>
            <span class="text-xs text-gray-400">{{ $task->created_at->diffForHumans() }}</span>
        </div>

        <div class="grid grid-cols-2 gap-2 text-sm mb-3">
            <div><span class="text-gray-500">VID:</span> {{ $task->vid }}</div>
            <div><span class="text-gray-500">Jenis:</span> {{ $task->job_type }}</div>
            <div><span class="text-gray-500">FE:</span> {{ $task->submittedBy?->name ?? '-' }}</div>
            <div><span class="text-gray-500">Lokasi:</span> {{ $task->location?->remote_name ?? '-' }}</div>
            <div class="col-span-2"><span class="text-gray-500">Total:</span> <strong>Rp {{ number_format($task->total_estimated, 0, ',', '.') }}</strong></div>
        </div>

        @if($task->items->isNotEmpty())
        <div class="mb-3">
            <table class="w-full text-xs border border-gray-100 rounded-lg overflow-hidden">
                <thead class="bg-gray-50"><tr><th class="px-2 py-1 text-left">Kategori</th><th class="px-2 py-1 text-right">Estimasi</th><th class="px-2 py-1 text-right">Revisi</th></tr></thead>
                <tbody class="divide-y divide-gray-100">
                    @foreach($task->items as $item)
                    <tr><td class="px-2 py-1">{{ $item->template?->category_name ?? '-' }}</td><td class="px-2 py-1 text-right">Rp{{ number_format($item->estimated_amount,0,',','.') }}</td><td class="px-2 py-1 text-right text-brand-600">Rp{{ number_format($item->revised_amount ?? $item->estimated_amount,0,',','.') }}</td></tr>
                    @endforeach
                </tbody>
            </table>
        </div>
        @endif

        @if($task->stage === 'ESTIMASI')
        <div class="flex gap-2">
            <form method="POST" action="{{ route('web.budget.forward', $task) }}" class="flex-1 flex gap-2">
                @csrf
                <input type="text" name="notes" placeholder="Catatan (opsional)" class="flex-1 border border-gray-300 rounded-lg px-3 py-1.5 text-xs">
                <button class="px-4 py-1.5 bg-brand-600 text-white text-xs rounded-lg hover:bg-brand-700 font-medium">Forward</button>
            </form>
            <form method="POST" action="{{ route('web.budget.reject', $task) }}" class="flex gap-2">
                @csrf
                <input type="text" name="reason" placeholder="Alasan reject" class="border border-gray-300 rounded-lg px-3 py-1.5 text-xs" required>
                <button class="px-4 py-1.5 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium">Reject</button>
            </form>
        </div>
        @else
        <p class="text-xs text-amber-600">⏳ Menunggu approval OWNER</p>
        @endif
    </div>
    @empty
    <p class="text-gray-400 text-center py-8">Tidak ada task menunggu review.</p>
    @endforelse
</div>
<div class="mt-4">{{ $tasks->links() }}</div>
@endsection

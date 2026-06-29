@extends('layouts.app')
@section('title', 'Approval Budget — OWNER')
@section('content')
<h2 class="text-lg font-semibold text-gray-800 mb-4">Approval Budget — Final</h2>

<div class="space-y-4">
    @forelse($tasks as $task)
    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <div class="flex items-start justify-between mb-3">
            <div>
                <span class="text-sm font-bold text-gray-900">#{{ $task->task_no }}</span>
                <span class="ml-2 px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-700">FORWARDED</span>
            </div>
            <span class="text-xs text-gray-400">{{ $task->created_at->diffForHumans() }}</span>
        </div>

        <div class="grid grid-cols-2 gap-2 text-sm mb-3">
            <div><span class="text-gray-500">VID:</span> {{ $task->vid }}</div>
            <div><span class="text-gray-500">Jenis:</span> {{ $task->job_type }}</div>
            <div><span class="text-gray-500">FE:</span> {{ $task->submittedBy?->name ?? '-' }}</div>
            <div><span class="text-gray-500">Kordinator:</span> {{ $task->forwardedBy?->name ?? '-' }}</div>
            <div><span class="text-gray-500">Lokasi:</span> {{ $task->location?->remote_name ?? '-' }}</div>
            <div><span class="text-gray-500">Total:</span> <strong>Rp {{ number_format($task->total_revised, 0, ',', '.') }}</strong></div>
        </div>

        @if($task->notes)
        <div class="bg-gray-50 rounded-lg p-2 text-xs text-gray-600 mb-3">📝 {{ $task->notes }}</div>
        @endif

        <form method="POST" action="{{ route('web.budget.approve', $task) }}">
            @csrf
            <table class="w-full text-xs border border-gray-100 rounded-lg overflow-hidden mb-3">
                <thead class="bg-gray-50"><tr><th class="px-2 py-1 text-left">Item</th><th class="px-2 py-1 text-right">Estimasi</th><th class="px-2 py-1 text-right">Revisi</th><th class="px-2 py-1 text-right">Approve *</th></tr></thead>
                <tbody class="divide-y divide-gray-100">
                    @foreach($task->items as $item)
                    <tr>
                        <td class="px-2 py-1">{{ $item->template?->category_name ?? '-' }}</td>
                        <td class="px-2 py-1 text-right">Rp{{ number_format($item->estimated_amount,0,',','.') }}</td>
                        <td class="px-2 py-1 text-right">Rp{{ number_format($item->revised_amount ?? $item->estimated_amount,0,',','.') }}</td>
                        <td class="px-2 py-1">
                            <input type="hidden" name="items[{{ $loop->index }}][id]" value="{{ $item->id }}">
                            <input type="number" name="items[{{ $loop->index }}][approved_amount]" value="{{ $item->revised_amount ?? $item->estimated_amount }}" class="w-24 border border-gray-300 rounded px-2 py-0.5 text-xs text-right" required>
                        </td>
                    </tr>
                    @endforeach
                </tbody>
            </table>

            <div class="flex gap-2">
                <button type="submit" class="flex-1 px-4 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 font-medium">✅ Approve</button>
            </div>
        </form>

        <form method="POST" action="{{ route('web.budget.reject', $task) }}" class="flex gap-2 mt-2">
            @csrf
            <input type="text" name="reason" placeholder="Alasan reject" class="flex-1 border border-gray-300 rounded-lg px-3 py-1.5 text-xs" required>
            <button class="px-4 py-1.5 bg-red-50 text-red-600 text-xs rounded-lg hover:bg-red-100 font-medium">Reject</button>
        </form>
    </div>
    @empty
    <p class="text-gray-400 text-center py-8">Tidak ada budget menunggu approval.</p>
    @endforelse
</div>
<div class="mt-4">{{ $tasks->links() }}</div>
@endsection

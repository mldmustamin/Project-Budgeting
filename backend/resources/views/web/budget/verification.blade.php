@extends('layouts.app')
@section('title', 'Verifikasi Budget')
@section('content')
<h2 class="text-lg font-semibold text-gray-800 mb-4">Verifikasi Realisasi Budget</h2>

<div class="space-y-4">
    @forelse($tasks as $task)
    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <div class="flex items-start justify-between mb-3">
            <div>
                <span class="text-sm font-bold text-gray-900">#{{ $task->task_no }}</span>
                <span class="ml-2 px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-700">REALISASI</span>
            </div>
            <span class="text-xs text-gray-400">{{ $task->created_at->diffForHumans() }}</span>
        </div>

        <div class="grid grid-cols-3 gap-2 text-sm mb-3">
            <div><span class="text-gray-500">VID:</span> {{ $task->vid }}</div>
            <div><span class="text-gray-500">Jenis:</span> {{ $task->job_type }}</div>
            <div><span class="text-gray-500">FE:</span> {{ $task->submittedBy?->name ?? '-' }}</div>
            <div class="col-span-3 flex gap-4">
                <span class="text-gray-500">Approved: <strong>Rp {{ number_format($task->total_approved, 0, ',', '.') }}</strong></span>
                <span class="text-gray-500">Realisasi: <strong>Rp {{ number_format($task->total_realization, 0, ',', '.') }}</strong></span>
                @php $diff = $task->total_realization - $task->total_approved @endphp
                <span class="text-{{ $diff < 0 ? 'green' : ($diff > 0 ? 'red' : 'gray') }}-600 font-medium">Selisih: Rp {{ number_format($diff, 0, ',', '.') }}</span>
            </div>
        </div>

        <form method="POST" action="{{ route('web.budget.verify', $task) }}">
            @csrf
            <table class="w-full text-xs border border-gray-100 rounded-lg overflow-hidden mb-3">
                <thead class="bg-gray-50"><tr><th class="px-2 py-1 text-left">Item</th><th class="px-2 py-1 text-right">Approved</th><th class="px-2 py-1 text-right">Realisasi</th><th class="px-2 py-1 text-center">Bill OK</th></tr></thead>
                <tbody class="divide-y divide-gray-100">
                    @foreach($task->items as $item)
                    <tr>
                        <td class="px-2 py-1">{{ $item->template?->category_name ?? '-' }}</td>
                        <td class="px-2 py-1 text-right">Rp{{ number_format($item->approved_amount ?? 0,0,',','.') }}</td>
                        <td class="px-2 py-1 text-right">Rp{{ number_format($item->realization_amount ?? 0,0,',','.') }}</td>
                        <td class="px-2 py-1 text-center">
                            <input type="hidden" name="items[{{ $loop->index }}][id]" value="{{ $item->id }}">
                            <input type="checkbox" name="items[{{ $loop->index }}][bill_verified]" value="1" @checked($item->bill_verified) class="rounded border-gray-300">
                        </td>
                    </tr>
                    @endforeach
                </tbody>
            </table>

            <div class="flex gap-2">
                <button type="submit" class="flex-1 px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">✅ Verifikasi</button>
            </div>
        </form>

        @if($task->reconciled_by === null)
        <form method="POST" action="{{ route('web.budget.reconcile', $task) }}" class="mt-2">
            @csrf
            <button class="w-full px-4 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 font-medium">🔒 Rekonsiliasi</button>
        </form>
        @endif
    </div>
    @empty
    <p class="text-gray-400 text-center py-8">Tidak ada task menunggu verifikasi.</p>
    @endforelse
</div>
<div class="mt-4">{{ $tasks->links() }}</div>
@endsection

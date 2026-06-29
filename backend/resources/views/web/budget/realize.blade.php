@extends('layouts.app')

@section('title', 'Realisasi — ' . $task->task_no)

@section('content')
<div class="max-w-4xl">

    <div class="flex items-center gap-3 mb-6">
        <a href="{{ route('web.budget.index') }}" class="text-gray-500 hover:text-gray-700">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        </a>
        <h2 class="text-lg font-semibold text-gray-800">Realisasi Budget — {{ $task->task_no }}</h2>
        <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">APPROVED</span>
    </div>

    {{-- Task Header --}}
    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm mb-6">
        <div class="grid grid-cols-4 gap-4 text-sm">
            <div>
                <span class="text-gray-500 text-xs block">Task No</span>
                <span class="font-semibold text-gray-900">{{ $task->task_no }}</span>
            </div>
            <div>
                <span class="text-gray-500 text-xs block">VID</span>
                <span class="font-semibold text-gray-900">{{ $task->vid }}</span>
            </div>
            <div>
                <span class="text-gray-500 text-xs block">Jenis Pekerjaan</span>
                <span class="font-semibold text-gray-900">{{ $task->job_type }}</span>
            </div>
            <div>
                <span class="text-gray-500 text-xs block">Lokasi</span>
                <span class="font-semibold text-gray-900">{{ $task->location?->remote_name ?? '-' }}</span>
            </div>
            <div class="col-span-4 pt-2 border-t border-gray-100 mt-1">
                <span class="text-gray-500 text-xs">Total Approved:</span>
                <span class="font-bold text-lg text-brand-600 ml-2">Rp {{ number_format($task->total_approved, 0, ',', '.') }}</span>
            </div>
        </div>
    </div>

    {{-- Realization Form --}}
    <form method="POST" action="{{ route('web.budget.realize-store', $task) }}" class="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
        @csrf

        <table class="w-full text-sm border border-gray-100 rounded-lg overflow-hidden mb-4">
            <thead class="bg-gray-50">
                <tr>
                    <th class="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase">Item</th>
                    <th class="px-3 py-2 text-right text-xs font-semibold text-gray-500 uppercase w-36">Approved (Rp)</th>
                    <th class="px-3 py-2 text-right text-xs font-semibold text-gray-500 uppercase w-36">Realisasi (Rp) *</th>
                    <th class="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase">Catatan</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @foreach($task->items as $i => $item)
                <tr class="hover:bg-gray-50">
                    <td class="px-3 py-2 text-gray-900">
                        {{ $item->template?->category_name ?? 'Item #' . ($i + 1) }}
                        @if($item->tanggal)
                            <span class="block text-xs text-gray-400">{{ $item->tanggal->format('d/m/Y') }}</span>
                        @endif
                    </td>
                    <td class="px-3 py-2 text-right text-gray-600">
                        Rp{{ number_format($item->approved_amount ?? 0, 0, ',', '.') }}
                    </td>
                    <td class="px-3 py-2">
                        <input type="hidden" name="items[{{ $i }}][id]" value="{{ $item->id }}">
                        <input type="number" name="items[{{ $i }}][realization_amount]"
                               value="{{ old("items.$i.realization_amount", $item->realization_amount ?? $item->approved_amount ?? 0) }}"
                               class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-xs text-right focus:ring-2 focus:ring-brand-500"
                               min="0" required>
                    </td>
                    <td class="px-3 py-2">
                        <input type="text" name="items[{{ $i }}][note]"
                               value="{{ old("items.$i.note", $item->note ?? '') }}"
                               class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-xs focus:ring-2 focus:ring-brand-500"
                               placeholder="Catatan (opsional)" maxlength="500">
                    </td>
                </tr>
                @endforeach
            </tbody>
        </table>

        {{-- Diff: total_realization - total_approved --}}
        <div class="bg-gray-50 rounded-lg p-4 mb-4 flex flex-wrap gap-4 items-center">
            <div>
                <span class="text-xs text-gray-500">Total Approved</span>
                <span class="text-sm font-semibold text-gray-800 ml-2">Rp {{ number_format($task->total_approved, 0, ',', '.') }}</span>
            </div>
            <div>
                <span class="text-xs text-gray-500">Total Realisasi</span>
                <span id="totalRealisasiDisplay" class="text-sm font-semibold text-brand-600 ml-2">Rp {{ number_format(old('total_realization', $task->total_realization ?? $task->total_approved ?? 0), 0, ',', '.') }}</span>
            </div>
            @php
                $diff = ($task->total_realization ?? $task->total_approved ?? 0) - ($task->total_approved ?? 0);
            @endphp
            <div>
                <span class="text-xs text-gray-500">Selisih</span>
                <span id="selisihDisplay" class="text-sm font-bold ml-2 {{ $diff < 0 ? 'text-green-600' : ($diff > 0 ? 'text-red-600' : 'text-gray-600') }}">
                    Rp {{ number_format($diff, 0, ',', '.') }}
                </span>
            </div>
        </div>

        <div class="flex justify-between pt-2">
            <a href="{{ route('web.budget.index') }}" class="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 font-medium">Batal</a>
            <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">
                💰 Simpan Realisasi
            </button>
        </div>
    </form>
</div>

<script>
(function() {
    const approved = {{ $task->total_approved ?? 0 }};
    const inputs = document.querySelectorAll('input[name$="[realization_amount]"]');

    function calc() {
        let total = 0;
        inputs.forEach(inp => { total += parseInt(inp.value) || 0; });
        const selisih = total - approved;
        const selisihEl = document.getElementById('selisihDisplay');
        const totalEl = document.getElementById('totalRealisasiDisplay');

        if (totalEl) {
            totalEl.textContent = 'Rp ' + new Intl.NumberFormat('id-ID').format(total);
        }
        if (selisihEl) {
            selisihEl.textContent = 'Rp ' + new Intl.NumberFormat('id-ID').format(selisih);
            selisihEl.className = 'text-sm font-bold ml-2 ' + (selisih < 0 ? 'text-green-600' : (selisih > 0 ? 'text-red-600' : 'text-gray-600'));
        }
    }

    inputs.forEach(inp => inp.addEventListener('input', calc));
    calc();
})();
</script>
@endsection

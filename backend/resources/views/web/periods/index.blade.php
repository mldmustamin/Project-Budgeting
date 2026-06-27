@extends('layouts.app')
@section('title', 'Period Management')
@section('content')

<div class="mb-4">
    <button onclick="document.getElementById('createModal').classList.remove('hidden')" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">+ Tambah Periode</button>
</div>

<div id="createModal" class="hidden fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
    <div class="bg-white rounded-2xl p-6 w-full max-w-sm shadow-xl" onclick="event.stopPropagation()">
        <h4 class="text-sm font-semibold text-gray-800 mb-4">Buat Periode Akuntansi</h4>
        <form method="POST" action="{{ route('web.periods.store') }}">
            @csrf
            <div class="space-y-3">
                <div><label class="text-xs text-gray-500">Mulai</label><input type="date" name="period_start" required class="w-full border rounded-lg px-3 py-2 text-sm mt-1"></div>
                <div><label class="text-xs text-gray-500">Selesai</label><input type="date" name="period_end" required class="w-full border rounded-lg px-3 py-2 text-sm mt-1"></div>
            </div>
            <div class="flex gap-2 mt-4 justify-end">
                <button type="button" onclick="document.getElementById('createModal').classList.add('hidden')" class="px-4 py-2 bg-gray-100 text-sm rounded-lg">Batal</button>
                <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg">Simpan</button>
            </div>
        </form>
    </div>
</div>

<div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
    <div class="px-5 py-3 border-b"><h3 class="text-sm font-semibold text-gray-700">Periode Akuntansi ({{$periods->total()}})</h3></div>
    <table class="w-full text-sm">
        <thead class="bg-gray-50"><tr>
            <th class="px-5 py-3 text-xs text-gray-400 uppercase">Mulai</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">Selesai</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">Status</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">Ditutup</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">Aksi</th>
        </tr></thead>
        <tbody class="divide-y">
            @forelse($periods as $p)
            <tr class="hover:bg-gray-50">
                <td class="px-5 py-3 text-gray-800 text-xs">{{ $p->period_start->format('d M Y') }}</td>
                <td class="px-5 py-3 text-gray-800 text-xs">{{ $p->period_end->format('d M Y') }}</td>
                <td class="px-5 py-3">
                    <span class="px-2 py-0.5 rounded-full text-xs font-semibold @if($p->status==='CLOSED')bg-red-50 text-red-700 ring-1 ring-red-200 @else bg-brand-50 text-brand-700 ring-1 ring-brand-200 @endif">{{ $p->status }}</span>
                </td>
                <td class="px-5 py-3 text-gray-500 text-xs">{{ $p->closed_at?->format('d M Y H:i') ?: '-' }}</td>
                <td class="px-5 py-3">
                    @if($p->status === 'OPEN')
                    <form method="POST" action="{{ route('web.periods.close', $p) }}" class="inline">@csrf<button class="text-xs text-red-600 hover:text-red-800 font-medium">Tutup</button></form>
                    @else
                    <form method="POST" action="{{ route('web.periods.reopen', $p) }}" class="inline">@csrf<button class="text-xs text-brand-600 hover:text-brand-800 font-medium">Buka</button></form>
                    @endif
                </td>
            </tr>
            @empty
            <tr><td colspan="5" class="px-5 py-8 text-center text-gray-400">Belum ada periode.</td></tr>
            @endforelse
        </tbody>
    </table>
    @if($periods->hasPages())<div class="px-5 py-3 border-t">{{$periods->links()}}</div>@endif
</div>
@endsection

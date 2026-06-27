@extends('layouts.app')
@section('title', $project->name)
@section('content')

<div class="mb-4"><a href="{{ route('web.projects.index') }}" class="text-sm text-brand-600 hover:text-brand-800">← Kembali ke Project</a></div>

<h2 class="text-xl font-bold text-gray-900 mb-1">{{ $project->name }}</h2>
<p class="text-sm text-gray-500 mb-6">{{ $project->description ?: 'Tidak ada deskripsi' }}</p>

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
    <x-web-stat-card label="Dana Masuk" :value="$summary['total_fund_in']" color="green"><x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1"/></x-slot:icon></x-web-stat-card>
    <x-web-stat-card label="Kas Keluar" :value="$summary['total_cash_out']" color="red"><x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2z"/></x-slot:icon></x-web-stat-card>
    <x-web-stat-card label="Saving" :value="$summary['saving']" color="indigo"><x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/></x-slot:icon></x-web-stat-card>
    <x-web-stat-card label="Posisi Bersih" :value="$summary['net_position']" color="indigo"><x-slot:icon><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2"/></x-slot:icon></x-web-stat-card>
</div>

<div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6 text-sm">
    <div class="bg-white rounded-xl border p-4"><span class="text-xs text-gray-400 uppercase font-bold">Office Reported</span><p class="text-lg font-extrabold">Rp{{ number_format($summary['total_office_reported'],0,',','.') }}</p></div>
    <div class="bg-white rounded-xl border p-4"><span class="text-xs text-gray-400 uppercase font-bold">Office Real</span><p class="text-lg font-extrabold">Rp{{ number_format($summary['total_office_real'],0,',','.') }}</p></div>
    <div class="bg-white rounded-xl border p-4"><span class="text-xs text-gray-400 uppercase font-bold">Personal</span><p class="text-lg font-extrabold">Rp{{ number_format($summary['total_personal_expense'],0,',','.') }}</p></div>
    <div class="bg-white rounded-xl border p-4"><span class="text-xs text-gray-400 uppercase font-bold">Transaksi</span><p class="text-lg font-extrabold">{{ $summary['transaction_count'] }}</p></div>
</div>

<div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
    <div class="px-5 py-3 border-b"><h3 class="text-sm font-semibold text-gray-700">Transaksi ({{ $txs->count() }})</h3></div>
    <table class="w-full text-sm">
        <thead class="bg-gray-50"><tr>
            <th class="px-5 py-3 text-xs text-gray-500 uppercase text-left">Tanggal</th>
            <th class="px-5 py-3 text-xs text-gray-500 uppercase text-left">Tipe</th>
            <th class="px-5 py-3 text-xs text-gray-500 uppercase text-right">Reported</th>
            <th class="px-5 py-3 text-xs text-gray-500 uppercase text-right">Real</th>
            <th class="px-5 py-3 text-xs text-gray-500 uppercase text-right">Saving</th>
            <th class="px-5 py-3 text-xs text-gray-500 uppercase">Status</th>
        </tr></thead>
        <tbody class="divide-y">
            @forelse($txs as $tx)
            @php $saving = $tx->type === 'OFFICE_EXPENSE' ? $tx->reported_amount - $tx->real_amount : null; @endphp
            <tr class="hover:bg-gray-50">
                <td class="px-5 py-3 text-gray-600 text-xs">{{ $tx->date->format('d M Y') }}</td>
                <td class="px-5 py-3"><span class="px-2 py-0.5 rounded-full text-xs font-medium @if($tx->type==='FUND_IN')bg-brand-50 text-brand-700 @elseif($tx->type==='OFFICE_EXPENSE')bg-blue-50 text-blue-700 @else bg-orange-50 text-orange-700 @endif">{{ str_replace('_',' ',$tx->type) }}</span></td>
                <td class="px-5 py-3 text-right text-xs">Rp{{ number_format($tx->reported_amount,0,',','.') }}</td>
                <td class="px-5 py-3 text-right text-xs">Rp{{ number_format($tx->real_amount,0,',','.') }}</td>
                <td class="px-5 py-3 text-right text-xs @if($saving !== null && $saving > 0) text-brand-600 font-bold @elseif($saving !== null && $saving < 0) text-red-600 @else text-gray-400 @endif">
                    {{ $saving !== null ? 'Rp'.number_format($saving,0,',','.') : '-' }}
                </td>
                <td class="px-5 py-3"><span class="px-2 py-0.5 rounded-full text-xs font-medium @if($tx->approval_status==='APPROVED')bg-green-50 text-green-700 @elseif($tx->approval_status==='PENDING')bg-amber-50 text-amber-700 @elseif($tx->approval_status==='DISPUTED')bg-red-50 text-red-700 @else bg-gray-50 text-gray-600 @endif">{{ $tx->approval_status }}</span></td>
            </tr>
            @empty
            <tr><td colspan="6" class="px-5 py-8 text-center text-gray-400">Belum ada transaksi.</td></tr>
            @endforelse
        </tbody>
    </table>
</div>
@endsection

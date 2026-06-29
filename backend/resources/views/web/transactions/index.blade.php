@extends('layouts.app')

@section('title', 'Transaksi')

@section('content')
<div class="flex items-center justify-between mb-4">
    <h2 class="text-lg font-semibold text-gray-800">Daftar Transaksi</h2>
    <a href="{{ route('web.transactions.create') }}" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">+ Buat Transaksi</a>
</div>
<div class="bg-white rounded-xl border border-gray-200 p-4 mb-6">
    <form method="GET" action="{{ route('web.transactions.index') }}" class="flex flex-wrap gap-3 items-end">
        <div>
            <label class="block text-xs font-semibold text-gray-500 mb-1">Project</label>
            <select name="project_uuid" class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                <option value="">Semua Project</option>
                @foreach($projects as $p)
                    <option value="{{ $p->uuid }}" @selected(request('project_uuid') === $p->uuid)>{{ $p->name }}</option>
                @endforeach
            </select>
        </div>
        <div>
            <label class="block text-xs font-semibold text-gray-500 mb-1">Tipe</label>
            <select name="type" class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                <option value="">Semua</option>
                <option value="FUND_IN" @selected(request('type') === 'FUND_IN')>Dana Masuk</option>
                <option value="OFFICE_EXPENSE" @selected(request('type') === 'OFFICE_EXPENSE')>Office Expense</option>
                <option value="PERSONAL_EXPENSE" @selected(request('type') === 'PERSONAL_EXPENSE')>Personal Expense</option>
            </select>
        </div>
        <div>
            <label class="block text-xs font-semibold text-gray-500 mb-1">Status</label>
            <select name="approval_status" class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                <option value="">Semua</option>
                <option value="DRAFT" @selected(request('approval_status') === 'DRAFT')>Draft</option>
                <option value="PENDING" @selected(request('approval_status') === 'PENDING')>Pending</option>
                <option value="APPROVED" @selected(request('approval_status') === 'APPROVED')>Approved</option>
                <option value="REJECTED" @selected(request('approval_status') === 'REJECTED')>Rejected</option>
            </select>
        </div>
        <div>
            <label class="block text-xs font-semibold text-gray-500 mb-1">Dari</label>
            <input type="date" name="date_from" value="{{ request('date_from') }}" class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
        </div>
        <div>
            <label class="block text-xs font-semibold text-gray-500 mb-1">Sampai</label>
            <input type="date" name="date_to" value="{{ request('date_to') }}" class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
        </div>
        <div class="flex gap-2">
            <button type="submit" class="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700">Filter</button>
            <a href="{{ route('web.transactions.index') }}" class="px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200">Reset</a>
        </div>
    </form>
</div>

<div class="bg-white rounded-xl border border-gray-200">
    <div class="overflow-x-auto">
        <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left">
                <tr>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Tanggal</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Project</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Tipe</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Deskripsi</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase text-right">Reported</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase text-right">Real</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Approval</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase"></th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($transactions as $tx)
                <tr class="hover:bg-gray-50">
                    <td class="px-5 py-3 text-gray-600 whitespace-nowrap">{{ $tx->date?->format('d M Y') }}</td>
                    <td class="px-5 py-3 text-gray-900">{{ $tx->project?->name ?? '-' }}</td>
                    <td class="px-5 py-3">
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium
                            @if($tx->type === 'FUND_IN') bg-green-100 text-green-800
                            @elseif($tx->type === 'OFFICE_EXPENSE') bg-blue-100 text-blue-800
                            @else bg-orange-100 text-orange-800 @endif">
                            {{ str_replace('_', ' ', $tx->type) }}
                        </span>
                    </td>
                    <td class="px-5 py-3 text-gray-600 max-w-xs truncate">{{ $tx->description }}</td>
                    <td class="px-5 py-3 text-gray-900 text-right">Rp {{ number_format($tx->reported_amount, 0, ',', '.') }}</td>
                    <td class="px-5 py-3 text-gray-900 text-right">Rp {{ number_format($tx->real_amount, 0, ',', '.') }}</td>
                    <td class="px-5 py-3">
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium
                            @if($tx->approval_status === 'APPROVED') bg-green-100 text-green-800
                            @elseif($tx->approval_status === 'PENDING') bg-amber-100 text-amber-800
                            @elseif($tx->approval_status === 'REJECTED') bg-red-100 text-red-800
                            @else bg-gray-100 text-gray-600 @endif">
                            {{ $tx->approval_status }}
                        </span>
                    </td>
                    <td class="px-5 py-3">
                        <a href="{{ route('web.transactions.show', $tx) }}" class="text-indigo-600 hover:text-indigo-800 text-xs font-medium">Detail</a>
                    </td>
                </tr>
                @empty
                <tr>
                    <td colspan="8" class="px-5 py-8 text-center text-gray-400">Tidak ada transaksi ditemukan.</td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>
    @if($transactions->hasPages())
    <div class="px-5 py-3 border-t border-gray-100">
        {{ $transactions->links() }}
    </div>
    @endif
</div>
@endsection

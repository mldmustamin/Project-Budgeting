@extends('layouts.app')

@section('title', 'Detail Transaksi')

@section('content')
<div class="max-w-4xl">
    <div class="flex items-center gap-3 mb-6">
        <a href="{{ route('web.transactions.index') }}" class="text-gray-500 hover:text-gray-700">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        </a>
        <h2 class="text-lg font-semibold text-gray-800">Detail Transaksi</h2>
    </div>

    <div class="bg-white rounded-xl border border-gray-200">
        <div class="p-6 space-y-4">
            <div class="grid grid-cols-2 gap-6">
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">UUID</p>
                    <p class="text-sm text-gray-900 font-mono mt-1">{{ $transaction->uuid }}</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Project</p>
                    <p class="text-sm text-gray-900 mt-1">{{ $transaction->project?->name ?? '-' }}</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Tipe</p>
                    <p class="text-sm mt-1">
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium
                            @if($transaction->type === 'FUND_IN') bg-green-100 text-green-800
                            @elseif($transaction->type === 'OFFICE_EXPENSE') bg-blue-100 text-blue-800
                            @else bg-orange-100 text-orange-800 @endif">
                            {{ str_replace('_', ' ', $transaction->type) }}
                        </span>
                    </p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Tanggal</p>
                    <p class="text-sm text-gray-900 mt-1">{{ $transaction->date?->format('d M Y') }}</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Reported Amount</p>
                    <p class="text-sm text-gray-900 font-semibold mt-1">Rp {{ number_format($transaction->reported_amount, 0, ',', '.') }}</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Real Amount</p>
                    <p class="text-sm text-gray-900 font-semibold mt-1">Rp {{ number_format($transaction->real_amount, 0, ',', '.') }}</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Approval Status</p>
                    <p class="text-sm mt-1">
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium
                            @if($transaction->approval_status === 'APPROVED') bg-green-100 text-green-800
                            @elseif($transaction->approval_status === 'PENDING') bg-amber-100 text-amber-800
                            @elseif($transaction->approval_status === 'REJECTED') bg-red-100 text-red-800
                            @else bg-gray-100 text-gray-600 @endif">
                            {{ $transaction->approval_status }}
                        </span>
                    </p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Finance Status</p>
                    <p class="text-sm text-gray-900 mt-1">{{ $transaction->finance_status }}</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">User</p>
                    <p class="text-sm text-gray-900 mt-1">{{ $transaction->user?->name ?? '-' }}</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Dibuat</p>
                    <p class="text-sm text-gray-900 mt-1">{{ $transaction->created_at->format('d M Y H:i') }}</p>
                </div>
            </div>

            @if($transaction->description)
            <div class="pt-4 border-t border-gray-100">
                <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Deskripsi</p>
                <p class="text-sm text-gray-900 mt-1">{{ $transaction->description }}</p>
            </div>
            @endif

            @if($transaction->note)
            <div class="pt-4 border-t border-gray-100">
                <p class="text-xs text-gray-500 uppercase tracking-wider font-semibold">Catatan</p>
                <p class="text-sm text-gray-900 mt-1">{{ $transaction->note }}</p>
            </div>
            @endif
        </div>
    </div>
</div>
@endsection

@extends('layouts.app')

@section('title', 'Approval Queue')

@section('content')
<div class="bg-white rounded-xl border border-gray-200 mb-4 p-4">
    <form method="GET" action="{{ route('web.approval.index') }}" class="flex gap-3 items-end">
        <input type="hidden" name="tab" value="{{ $tab }}">
        <div>
            <label class="block text-xs font-semibold text-gray-500 mb-1">Filter Project</label>
            <select name="project_uuid" class="border border-gray-300 rounded-lg px-3 py-2 text-sm">
                <option value="">Semua</option>
                @foreach($projects as $p)
                    <option value="{{ $p->uuid }}" @selected(request('project_uuid') === $p->uuid)>{{ $p->name }}</option>
                @endforeach
            </select>
        </div>
        <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700">Filter</button>
    </form>
</div>

{{-- Tabs --}}
<div class="flex gap-1 mb-4">
    <a href="{{ route('web.approval.index', ['tab' => 'pending']) }}"
       class="px-4 py-2 text-sm rounded-lg font-medium transition-colors {{ $tab === 'pending' ? 'bg-brand-600 text-white' : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50' }}">
        Pending ({{ $pendingTransactions->total() }})
    </a>
    <a href="{{ route('web.approval.index', ['tab' => 'disputed']) }}"
       class="px-4 py-2 text-sm rounded-lg font-medium transition-colors {{ $tab === 'disputed' ? 'bg-red-600 text-white' : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50' }}">
        Disputed ({{ $disputedTransactions->total() }})
    </a>
</div>

@if($tab === 'pending')
<div class="bg-white rounded-xl border border-gray-200">
    <div class="overflow-x-auto">
        <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left">
                <tr>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Tanggal</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Project</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Engineer</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Tipe</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Deskripsi</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase text-right">Amount</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Aksi</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($pendingTransactions as $tx)
                <tr class="hover:bg-gray-50" x-data="{ showReject: false }">
                    <td class="px-5 py-3 text-gray-600 whitespace-nowrap">{{ $tx->date?->format('d M Y') }}</td>
                    <td class="px-5 py-3 text-gray-900">{{ $tx->project?->name ?? '-' }}</td>
                    <td class="px-5 py-3 text-gray-600">{{ $tx->user?->name ?? '-' }}</td>
                    <td class="px-5 py-3">
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium
                            @if($tx->type === 'FUND_IN') bg-green-100 text-green-800
                            @elseif($tx->type === 'OFFICE_EXPENSE') bg-blue-100 text-blue-800
                            @else bg-orange-100 text-orange-800 @endif">
                            {{ str_replace('_', ' ', $tx->type) }}
                        </span>
                    </td>
                    <td class="px-5 py-3 text-gray-600 max-w-xs truncate">{{ $tx->description }}</td>
                    <td class="px-5 py-3 text-gray-900 text-right font-medium">Rp {{ number_format($tx->reported_amount, 0, ',', '.') }}</td>
                    <td class="px-5 py-3">
                        <div class="flex gap-2">
                            <form method="POST" action="{{ route('web.approval.approve', $tx) }}">
                                @csrf
                                <button class="px-3 py-1.5 bg-green-600 text-white text-xs rounded-lg hover:bg-green-700 font-medium">Approve</button>
                            </form>
                            <button @click="showReject = true" class="px-3 py-1.5 bg-red-600 text-white text-xs rounded-lg hover:bg-red-700 font-medium">Reject</button>
                        </div>

                        {{-- Reject Modal --}}
                        <div x-show="showReject" x-cloak class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
                            <div @click.away="showReject = false" class="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
                                <h4 class="text-sm font-semibold text-gray-800 mb-3">Tolak Transaksi</h4>
                                <p class="text-xs text-gray-500 mb-3">{{ $tx->description }} - Rp {{ number_format($tx->reported_amount, 0, ',', '.') }}</p>
                                <form method="POST" action="{{ route('web.approval.reject', $tx) }}">
                                    @csrf
                                    <textarea name="reason" rows="3" required class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-red-500" placeholder="Alasan penolakan..."></textarea>
                                    <div class="flex gap-2 mt-3 justify-end">
                                        <button type="button" @click="showReject = false" class="px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200">Batal</button>
                                        <button type="submit" class="px-4 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700">Tolak</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </td>
                </tr>
                @empty
                <tr>
                    <td colspan="7" class="px-5 py-12 text-center">
                        <p class="text-gray-400 text-sm">Tidak ada transaksi pending approval.</p>
                        <p class="text-gray-300 text-xs mt-1">Semua transaksi sudah diproses.</p>
                    </td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>
    @if($pendingTransactions->hasPages())
    <div class="px-5 py-3 border-t border-gray-100">
        {{ $pendingTransactions->links() }}
    </div>
    @endif
</div>
@else
{{-- Disputed Tab --}}
<div class="bg-white rounded-xl border border-gray-200">
    <div class="overflow-x-auto">
        <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left">
                <tr>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Tanggal</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Project</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Engineer</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase text-right">Original</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase text-right">Sanggahan</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Alasan</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Aksi</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($disputedTransactions as $tx)
                <tr class="hover:bg-gray-50" x-data="{ showResolve: false }">
                    <td class="px-5 py-3 text-gray-600 whitespace-nowrap">{{ $tx->date?->format('d M Y') }}</td>
                    <td class="px-5 py-3 text-gray-900">{{ $tx->project?->name ?? '-' }}</td>
                    <td class="px-5 py-3 text-gray-600">{{ $tx->user?->name ?? '-' }}</td>
                    <td class="px-5 py-3 text-gray-500 text-right line-through">Rp{{ number_format($tx->real_amount, 0, ',', '.') }}</td>
                    <td class="px-5 py-3 text-red-600 text-right font-bold">Rp{{ number_format($tx->disputed_amount, 0, ',', '.') }}</td>
                    <td class="px-5 py-3 text-gray-600 max-w-xs truncate text-xs">{{ $tx->dispute_reason }}</td>
                    <td class="px-5 py-3">
                        <button @click="showResolve = true" class="px-3 py-1.5 bg-brand-600 text-white text-xs rounded-lg hover:bg-brand-700 font-medium">Resolve</button>
                        {{-- Resolve Modal --}}
                        <div x-show="showResolve" x-cloak class="fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
                            <div @click.away="showResolve = false" class="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
                                <h4 class="text-sm font-semibold text-gray-800 mb-3">Resolve Sanggahan</h4>
                                <p class="text-xs text-gray-500 mb-1">Original: Rp{{ number_format($tx->real_amount, 0, ',', '.') }}</p>
                                <p class="text-xs text-red-600 mb-3 font-medium">Sanggahan: Rp{{ number_format($tx->disputed_amount, 0, ',', '.') }}</p>
                                <p class="text-xs text-gray-400 mb-3 italic">"{{ $tx->dispute_reason }}"</p>
                                <form method="POST" action="{{ route('web.approval.resolve', $tx) }}">
                                    @csrf
                                    <input type="hidden" name="action" value="accept_dispute" id="resolve_action_{{$tx->id}}">
                                    <textarea name="resolution_note" rows="2" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm mb-3" placeholder="Catatan resolusi..."></textarea>
                                    <div class="flex gap-2 justify-end">
                                        <button type="button" @click="showResolve = false" class="px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200">Batal</button>
                                        <button type="submit" onclick="document.getElementById('resolve_action_{{$tx->id}}').value='reject_dispute'" class="px-4 py-2 bg-red-100 text-red-700 text-sm rounded-lg hover:bg-red-200 font-medium">Tolak Sanggahan</button>
                                        <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">Terima Sanggahan</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </td>
                </tr>
                @empty
                <tr><td colspan="7" class="px-5 py-12 text-center text-gray-400 text-sm">Tidak ada transaksi dalam sanggahan.</td></tr>
                @endforelse
            </tbody>
        </table>
    </div>
    @if($disputedTransactions->hasPages())
    <div class="px-5 py-3 border-t border-gray-100">{{ $disputedTransactions->links() }}</div>
    @endif
</div>
@endif
@endsection

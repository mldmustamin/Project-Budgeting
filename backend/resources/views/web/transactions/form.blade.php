@extends('layouts.app')

@section('title', $transaction ? 'Edit Transaksi' : 'Buat Transaksi')

@section('content')
<div class="max-w-3xl">
    <div class="flex items-center gap-3 mb-6">
        <a href="{{ route('web.transactions.index') }}" class="text-gray-500 hover:text-gray-700">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        </a>
        <h2 class="text-lg font-semibold text-gray-800">{{ $transaction ? 'Edit' : 'Buat' }} Transaksi</h2>
    </div>

    <form method="POST" action="{{ $transaction ? route('web.transactions.update', $transaction) : route('web.transactions.store') }}" class="bg-white rounded-xl border border-gray-200 p-6 space-y-5">
        @csrf
        @if($transaction) @method('PUT') @endif

        <div class="grid grid-cols-2 gap-4">
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Project *</label>
                <select name="project_id" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 @error('project_id') border-red-400 @enderror">
                    <option value="">Pilih Project</option>
                    @foreach($projects as $p)
                        <option value="{{ $p->id }}" @selected(old('project_id', $transaction?->project_id) == $p->id)>{{ $p->name }}</option>
                    @endforeach
                </select>
                @error('project_id') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
            </div>

            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Tipe *</label>
                <select name="type" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 @error('type') border-red-400 @enderror">
                    <option value="">Pilih Tipe</option>
                    <option value="FUND_IN" @selected(old('type', $transaction?->type) === 'FUND_IN')>Dana Masuk</option>
                    <option value="OFFICE_EXPENSE" @selected(old('type', $transaction?->type) === 'OFFICE_EXPENSE')>Office Expense</option>
                    <option value="PERSONAL_EXPENSE" @selected(old('type', $transaction?->type) === 'PERSONAL_EXPENSE')>Personal Expense</option>
                </select>
                @error('type') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
            </div>

            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Kategori</label>
                <select name="category_id" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                    <option value="">Pilih Kategori</option>
                    @foreach($categories as $c)
                        <option value="{{ $c->id }}" @selected(old('category_id', $transaction?->category_id) == $c->id)>{{ $c->name }}</option>
                    @endforeach
                </select>
            </div>

            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Akun</label>
                <select name="account_id" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500">
                    <option value="">Pilih Akun</option>
                    @foreach($accounts as $a)
                        <option value="{{ $a->id }}" @selected(old('account_id', $transaction?->account_id) == $a->id)>{{ $a->name }}</option>
                    @endforeach
                </select>
            </div>

            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Tanggal *</label>
                <input type="date" name="date" value="{{ old('date', $transaction?->date?->format('Y-m-d')) }}" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 @error('date') border-red-400 @enderror">
                @error('date') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
            </div>

            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Nominal (Rp) *</label>
                <input type="number" name="reported_amount" value="{{ old('reported_amount', $transaction?->reported_amount) }}" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 @error('reported_amount') border-red-400 @enderror" placeholder="0">
                @error('reported_amount') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
            </div>

            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Real Amount (Rp)</label>
                <input type="number" name="real_amount" value="{{ old('real_amount', $transaction?->real_amount) }}" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500" placeholder="0">
            </div>
        </div>

        <div>
            <label class="block text-xs font-semibold text-gray-500 mb-1">Deskripsi</label>
            <textarea name="description" rows="3" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500" placeholder="Catatan...">{{ old('description', $transaction?->description) }}</textarea>
        </div>

        <div class="flex justify-between pt-2">
            <div>
                @if($transaction)
                <button type="button" onclick="if(confirm('Hapus transaksi ini?')){document.getElementById('deleteForm').submit()}" class="px-4 py-2 bg-red-50 text-red-600 text-sm rounded-lg hover:bg-red-100 font-medium">Hapus</button>
                @endif
            </div>
            <div class="flex gap-2">
                <a href="{{ route('web.transactions.index') }}" class="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 font-medium">Batal</a>
                <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">Simpan</button>
            </div>
        </div>
    </form>

    @if($transaction)
    <form id="deleteForm" method="POST" action="{{ route('web.transactions.destroy', $transaction) }}" class="hidden">
        @csrf @method('DELETE')
    </form>
    @endif
</div>
@endsection

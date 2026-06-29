@extends('layouts.app')
@section('title', 'Estimasi Budget')
@section('content')
<div class="flex items-center justify-between mb-4">
    <h2 class="text-lg font-semibold text-gray-800">Estimasi Budget</h2>
</div>

<form method="POST" action="{{ route('web.budget.store') }}" class="space-y-4" x-data="{ items: [{}] }">
    @csrf
    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">Data Task</h3>
        <div class="grid grid-cols-2 gap-3">
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Task No *</label><input type="text" name="task_no" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" required></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">VID *</label><input type="text" name="vid" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" required></div>
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Jenis Pekerjaan</label>
                <select name="job_type" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                    <option value="INSTALASI">Instalasi</option><option value="RELOKASI">Relokasi</option>
                    <option value="PMCM">PMCM</option><option value="DISMANTLE">Dismantle</option><option value="SURVEY">Survey</option>
                </select>
            </div>
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Lokasi</label>
                <select name="location_id" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                    <option value="">Pilih Lokasi</option>
                    @foreach(\App\Models\MasterLocation::orderBy('remote_name')->get() as $loc)
                        <option value="{{ $loc->id }}">{{ $loc->remote_name }}</option>
                    @endforeach
                </select>
            </div>
            <div class="col-span-2"><label class="block text-xs font-semibold text-gray-500 mb-1">Catatan</label><textarea name="notes" rows="2" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></textarea></div>
        </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-semibold text-gray-700">Item Budget</h3>
            <button type="button" @click="items.push({})" class="px-3 py-1 bg-brand-600 text-white text-xs rounded-lg">+ Tambah</button>
        </div>
        <template x-for="(item, index) in items" :key="index">
            <div class="border border-gray-100 rounded-lg p-3 mb-3">
                <div class="grid grid-cols-3 gap-3">
                    <div>
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Kategori</label>
                        <select :name="'items['+index+'][template_id]'" class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-xs">
                            <option value="">Pilih</option>
                            @foreach(\App\Models\BudgetItemTemplate::orderBy('category_name')->get() as $tpl)
                                <option value="{{ $tpl->id }}">{{ $tpl->category_name }} ({{ $tpl->pagu_type }})</option>
                            @endforeach
                        </select>
                    </div>
                    <div>
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Estimasi (Rp)</label>
                        <input type="number" :name="'items['+index+'][estimated_amount]'" class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-xs" min="0">
                    </div>
                    <div>
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Tanggal</label>
                        <input type="date" :name="'items['+index+'][tanggal]'" class="w-full border border-gray-300 rounded-lg px-2 py-1.5 text-xs">
                    </div>
                </div>
                <button type="button" @click="items.splice(index,1)" x-show="items.length > 1" class="mt-2 text-red-500 text-xs">Hapus</button>
            </div>
        </template>
    </div>

    <div class="flex justify-between">
        <a href="{{ route('web.dashboard') }}" class="px-6 py-2.5 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 font-medium">Batal</a>
        <div class="flex gap-2">
            <button type="submit" name="action" value="draft" class="px-6 py-2.5 bg-gray-600 text-white text-sm rounded-lg hover:bg-gray-700 font-medium">Simpan Draft</button>
            <button type="submit" name="action" value="submit" class="px-6 py-2.5 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">Submit</button>
        </div>
    </div>
</form>
<script src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
@endsection

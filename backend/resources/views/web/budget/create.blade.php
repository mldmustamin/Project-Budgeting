@extends('layouts.app')
@section('title', 'Estimasi Budget')
@section('content')
<div class="flex items-center justify-between mb-4">
    <h2 class="text-lg font-semibold text-gray-800">Estimasi Budget</h2>
</div>

<form method="POST" action="{{ route('web.budget.store') }}" class="space-y-4"
      x-data="{
        items: [{
            template_id: null,
            estimated_amount: '0',
            tanggal: '',
            note: '',
            templateSearch: '',
            templateOpen: false
        }],
        jobType: 'INSTALASI',
        selectedTaskLabel: '',

        allTemplates: {{ Js::from($templates->map(fn($t) => [
            'id' => $t->id,
            'name' => $t->category_name,
            'pagu_type' => $t->pagu_type,
            'pagu_amount' => $t->pagu_amount,
            'pagu_note' => $t->pagu_note,
        ])) }},

        filteredTemplates(index) {
            let q = (this.items[index].templateSearch || '').toLowerCase();
            return this.allTemplates.filter(t => t.name.toLowerCase().includes(q));
        },

        selectTemplate(index, tpl) {
            this.items[index].template_id = tpl.id;
            this.items[index].templateSearch = tpl.name + ' (' + tpl.pagu_type + ')';
            this.items[index].templateOpen = false;
        },

        addItem() {
            this.items.push({
                template_id: null,
                estimated_amount: '0',
                tanggal: '',
                note: '',
                templateSearch: '',
                templateOpen: false
            });
        },

        removeItem(index) {
            if (this.items.length > 1) this.items.splice(index, 1);
        },

        total() {
            return this.items.reduce((s, i) => s + (parseInt(String(i.estimated_amount).replace(/[^0-9]/g,'')) || 0), 0);
        },

        formatRp(val) {
            let n = parseInt(String(val).replace(/[^0-9]/g,''));
            return n ? n.toLocaleString('id-ID') : '0';
        }
    }"
>
    @csrf
    <div class="bg-white rounded-xl border border-gray-200 p-4 md:p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">Data Task</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Task No *</label>
                <input type="text" name="task_no" value="{{ old('task_no') }}" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" required>
            </div>
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">VID *</label>
                <input type="text" name="vid" value="{{ old('vid') }}" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" required>
            </div>
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Jenis Pekerjaan</label>
                <select name="job_type" x-model="jobType" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                    <option value="INSTALASI">Instalasi</option>
                    <option value="RELOKASI">Relokasi</option>
                    <option value="PMCM">PMCM</option>
                    <option value="DISMANTLE">Dismantle</option>
                    <option value="SURVEY">Survey</option>
                </select>
            </div>
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Lokasi</label>
                <select name="location_id" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                    <option value="">Pilih Lokasi</option>
                    @foreach($locations as $loc)
                        <option value="{{ $loc->id }}">{{ $loc->remote_name }}</option>
                    @endforeach
                </select>
            </div>
        </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-200 p-4 md:p-5 shadow-sm">
        <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-semibold text-gray-700">Item Budget (<span x-text="items.length"></span>)</h3>
            <button type="button" @click="addItem()" class="px-3 py-1.5 bg-brand-600 text-white text-xs rounded-lg hover:bg-brand-700">+ Tambah</button>
        </div>

        <template x-for="(item, index) in items" :key="index">
            <div class="border border-gray-100 rounded-lg p-3 mb-3" x-data="{ tplOpen: false }">
                <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-semibold text-gray-500" x-text="'Item #' + (index + 1)"></span>
                    <button type="button" x-show="items.length > 1" @click="removeItem(index)" class="text-red-500 text-xs hover:underline">Hapus</button>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                    {{-- SEARCHABLE TEMPLATE --}}
                    <div class="relative" @click.away="item.templateOpen = false">
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Kategori</label>
                        <input type="text"
                               x-model="item.templateSearch"
                               @focus="item.templateOpen = true"
                               @click="item.templateOpen = true"
                               placeholder="Cari kategori..."
                               class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                        <input type="hidden" :name="'items['+index+'][template_id]'" x-model="item.template_id">

                        {{-- DROPDOWN --}}
                        <div x-show="item.templateOpen" class="absolute z-50 w-full bg-white border border-gray-200 rounded-lg shadow-lg mt-1 max-h-48 overflow-y-auto">
                            <template x-for="tpl in filteredTemplates(index)" :key="tpl.id">
                                <div @click="selectTemplate(index, tpl); item.templateOpen = false"
                                     class="px-3 py-2 text-sm hover:bg-brand-50 cursor-pointer border-b border-gray-50">
                                    <div class="flex justify-between">
                                        <span>
                                            <span x-text="tpl.name" class="font-medium"></span>
                                            <span class="ml-1 text-xs text-gray-400" x-text="tpl.pagu_type"></span>
                                        </span>
                                        <span x-show="tpl.pagu_amount" class="text-xs text-gray-400" x-text="'Rp '+tpl.pagu_amount.toLocaleString('id-ID')"></span>
                                    </div>
                                </div>
                            </template>
                            <div x-show="filteredTemplates(index).length === 0" class="px-3 py-2 text-xs text-gray-400">Tidak ditemukan</div>
                        </div>
                    </div>

                    {{-- ESTIMATED AMOUNT --}}
                    <div>
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Estimasi (Rp)</label>
                        <input type="text" inputmode="numeric"
                               :name="'items['+index+'][estimated_amount]'"
                               x-model="item.estimated_amount"
                               x-on:input="item.estimated_amount = $el.value.replace(/[^0-9]/g,''); $el.value = item.estimated_amount ? parseInt(item.estimated_amount).toLocaleString('id-ID') : ''"
                               class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm text-right"
                               placeholder="0">
                    </div>

                    {{-- TANGGAL --}}
                    <div>
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Tanggal</label>
                        <input type="date" :name="'items['+index+'][tanggal]'" x-model="item.tanggal"
                               class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                    </div>
                </div>
            </div>
        </template>
    </div>

    {{-- TOTAL --}}
    <div class="bg-white rounded-xl border border-gray-200 p-4 flex items-center justify-between">
        <span class="text-sm font-semibold text-gray-700">Total Estimasi</span>
        <span class="text-lg font-bold text-brand-600" x-text="'Rp ' + formatRp(total())"></span>
    </div>

    <div class="flex flex-col md:flex-row justify-between gap-3">
        <a href="{{ route('web.dashboard') }}" class="px-6 py-2.5 bg-gray-100 text-gray-600 text-sm rounded-lg text-center">Batal</a>
        <div class="flex gap-2">
            <button type="submit" name="action" value="draft" class="flex-1 md:flex-none px-6 py-2.5 bg-gray-600 text-white text-sm rounded-lg">Simpan Draft</button>
            <button type="submit" name="action" value="submit" class="flex-1 md:flex-none px-6 py-2.5 bg-brand-600 text-white text-sm rounded-lg">Submit</button>
        </div>
    </div>
</form>
@endsection

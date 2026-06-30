@extends('layouts.app')
@section('title', 'Estimasi Budget')
@section('content')
<div class="flex items-center justify-between mb-4">
    <h2 class="text-lg font-semibold text-gray-800">Estimasi Budget</h2>
</div>

<form method="POST" action="{{ route('web.budget.store') }}" class="space-y-4"
      x-data="{
        items: [],
        pendingTemplateId: null,
        jobType: 'INSTALASI',

        allTemplates: {!! json_encode($templates->map(fn($t) => [
            'id' => $t->id,
            'name' => $t->category_name,
            'pagu_type' => $t->pagu_type ?? 'FIXED_PAGU',
            'pagu_amount' => $t->pagu_amount,
        ])) !!},

        filteredTemplatesBySearch(q) {
            q = (q || '').toLowerCase();
            return this.allTemplates.filter(t => t.name.toLowerCase().includes(q));
        },

        filledCount() {
            return this.items.length;
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
            <h3 class="text-sm font-semibold text-gray-700">Item Budget (<span x-text="filledCount()"></span> terisi)</h3>
        </div>

        {{-- ALWAYS-EMPTY ROW AT TOP --}}
        <div class="border-2 border-dashed border-brand-200 rounded-lg p-3 mb-3 bg-brand-50/30"
             x-data="{
                tplOpen: false,
                tplSearch: '',
                qty: 1,
                amount: '0',
                tanggal: '',
                get selectedTemplate() {
                    let id = this.$parent.pendingTemplateId;
                    if (!id) return null;
                    return this.$parent.allTemplates.find(t => t.id == id);
                },
                get isHotel() {
                    let t = this.selectedTemplate;
                    return t && (t.name.toLowerCase().includes('hotel') || t.name.toLowerCase().includes('akomodasi'));
                },
                get ratePerUnit() {
                    let t = this.selectedTemplate;
                    return t ? (t.pagu_amount || 0) : 0;
                },
                autoCalc() {
                    if (this.isHotel && this.ratePerUnit > 0) {
                        this.amount = String(this.ratePerUnit * this.qty);
                        this.$el.querySelector('[x-model=amount]').value = parseInt(this.amount).toLocaleString('id-ID');
                    }
                },
                commit() {
                    if (!this.$parent.pendingTemplateId) return;
                    let amt = this.amount.replace(/[^0-9]/g,'') || '0';
                    let item = {
                        template_id: this.$parent.pendingTemplateId,
                        estimated_amount: amt,
                        tanggal: this.tanggal,
                        templateSearch: this.tplSearch,
                        qty: this.qty,
                    };
                    this.$parent.items.push(item);
                    this.$parent.pendingTemplateId = null;
                    this.tplSearch = '';
                    this.qty = 1;
                    this.amount = '0';
                    this.tanggal = '';
                }
             }"
             x-effect="autoCalc()">
            <div class="flex items-center gap-1 mb-2">
                <span class="text-xs text-brand-600 font-medium">+ Item Baru</span>
                <span class="text-xs text-gray-400">(isi lalu Enter)</span>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
                {{-- KATEGORI --}}
                <div class="relative" @click.away="tplOpen = false">
                    <input type="text" x-model="tplSearch"
                           @focus="tplOpen = true" @click="tplOpen = true"
                           placeholder="Cari kategori..."
                           class="w-full border border-brand-300 rounded-lg px-3 py-2 text-sm bg-white">
                    <div x-show="tplOpen" class="absolute z-50 w-full bg-white border border-gray-200 rounded-lg shadow-lg mt-1 max-h-48 overflow-y-auto">
                        <template x-for="tpl in $parent.filteredTemplatesBySearch(tplSearch)" :key="tpl.id">
                            <div @click="$parent.pendingTemplateId = tpl.id; tplSearch = tpl.name + ' (' + tpl.pagu_type + ')'; tplOpen = false"
                                 class="px-3 py-2 text-sm hover:bg-brand-50 cursor-pointer border-b border-gray-50">
                                <div class="flex justify-between">
                                    <span><span x-text="tpl.name" class="font-medium"></span>
                                    <span class="ml-1 text-xs" :class="tpl.pagu_type === 'HOTEL' ? 'text-orange-500' : 'text-gray-400'" x-text="tpl.pagu_type"></span></span>
                                    <span x-show="tpl.pagu_amount" class="text-xs text-gray-400" x-text="'Rp '+tpl.pagu_amount.toLocaleString('id-ID')+'/hari'"></span>
                                </div>
                            </div>
                        </template>
                        <div x-show="$parent.filteredTemplatesBySearch(tplSearch).length === 0" class="px-3 py-2 text-xs text-gray-400">Tidak ditemukan</div>
                    </div>
                </div>

                {{-- QTY (only for hotel) --}}
                <div x-show="isHotel">
                    <input type="number" x-model.number="qty" min="1" max="30"
                           class="w-full border border-brand-300 rounded-lg px-3 py-2 text-sm text-center bg-white"
                           placeholder="Jml Hari">
                    <div class="text-xs text-gray-400 mt-1 text-center">
                        <span x-text="qty + ' hr × Rp ' + ratePerUnit.toLocaleString('id-ID')"></span>
                    </div>
                </div>

                {{-- AMOUNT --}}
                <div :class="isHotel ? 'md:col-span-1' : 'md:col-span-2'">
                    <input type="text" inputmode="numeric" x-model="amount"
                           x-on:input="let n=$el.value.replace(/[^0-9]/g,''); amount=n; $el.value=n?parseInt(n).toLocaleString('id-ID'):'';"
                           x-on:keydown.enter.prevent="commit()"
                           :placeholder="isHotel ? 'Auto (Rp)' : 'Estimasi (Rp)'"
                           :readonly="isHotel"
                           :class="isHotel ? 'bg-gray-50 text-gray-600' : 'bg-white'"
                           class="w-full border border-brand-300 rounded-lg px-3 py-2 text-sm text-right">
                </div>

                {{-- TANGGAL + ADD --}}
                <div class="flex gap-2">
                    <input type="date" x-model="tanggal" x-on:keydown.enter.prevent="commit()"
                           class="flex-1 border border-brand-300 rounded-lg px-3 py-2 text-sm bg-white">
                    <button type="button" @click="commit()"
                            :disabled="!$parent.pendingTemplateId || !amount"
                            class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg disabled:opacity-50">+</button>
                </div>
            </div>
        </div>

        {{-- FILLED ITEMS --}}
        <template x-for="(item, index) in items" :key="index">
            <div class="border border-gray-100 rounded-lg p-3 mb-2">
                <div class="flex items-center justify-between mb-2">
                    <span class="text-xs font-semibold text-gray-500" x-text="'#' + (index + 1) + ' ' + (item.templateSearch || 'Item')"></span>
                    <button type="button" @click="$parent.items.splice(index, 1)" class="text-red-500 text-xs hover:underline">✕</button>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <input type="hidden" :name="'items['+index+'][template_id]'" x-model="item.template_id">
                    <div class="text-sm text-gray-700" x-text="item.templateSearch"></div>
                    <div class="text-sm text-right font-medium" x-text="'Rp ' + (parseInt(item.estimated_amount) || 0).toLocaleString('id-ID')"></div>
                    <input type="hidden" :name="'items['+index+'][estimated_amount]'" x-model="item.estimated_amount">
                    <input type="hidden" :name="'items['+index+'][tanggal]'" x-model="item.tanggal">
                    <div class="text-sm text-gray-500" x-text="item.tanggal"></div>
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

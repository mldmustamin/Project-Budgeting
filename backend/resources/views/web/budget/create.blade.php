@extends('layouts.app')

@section('title', isset($task) ? 'Edit Budget — ' . $task->task_no : 'Buat Budget Estimate')

@section('content')
<div x-data="budgetForm()" x-init="init({{ Js::from(old('items', isset($task) ? $task->items->map(fn($i) => [
    'template_id' => $i->template_id,
    'estimated_amount' => $i->estimated_amount,
    'tanggal' => $i->tanggal?->format('Y-m-d'),
    'pagu' => $i->template?->getAmountForJobType($task->job_type ?? '') ?? $i->template?->pagu_amount ?? null,
    'name' => $i->template?->category_name ?? '',
])->toArray() : [])) }})" class="max-w-4xl">

    <div class="flex items-center gap-3 mb-6">
        <a href="{{ route('web.budget.index') }}" class="text-gray-500 hover:text-gray-700">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        </a>
        <h2 class="text-lg font-semibold text-gray-800">{{ isset($task) ? 'Edit Budget — ' . $task->task_no : 'Buat Budget Estimate' }}</h2>
        @if(isset($draftCount) && $draftCount > 0)
            <span class="px-2 py-0.5 bg-amber-100 text-amber-700 text-xs rounded-full font-medium">{{ $draftCount }} Draft</span>
        @endif
    </div>

    <form method="POST"
          action="{{ isset($task) ? route('web.budget.update', $task) : route('web.budget.store') }}"
          class="bg-white rounded-xl border border-gray-200 p-6 space-y-6 shadow-sm"
          @submit="prepareSubmit">

        @csrf
        @if(isset($task)) @method('PUT') @endif

        {{-- Header Fields --}}
        <div>
            <h3 class="text-sm font-semibold text-gray-700 mb-3">Data Task</h3>
            <div class="grid grid-cols-2 gap-4">
                <div>
                    <label class="block text-xs font-semibold text-gray-500 mb-1">Task No *</label>
                    <input type="text" name="task_no" value="{{ old('task_no', $task->task_no ?? '') }}"
                           class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-brand-500 @error('task_no') border-red-400 @enderror"
                           required maxlength="50" placeholder="Contoh: TASK-001">
                    @error('task_no') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-500 mb-1">VID *</label>
                    <input type="text" name="vid" value="{{ old('vid', $task->vid ?? '') }}"
                           class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-brand-500 @error('vid') border-red-400 @enderror"
                           required maxlength="50" placeholder="Contoh: VID-2025-001">
                    @error('vid') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-500 mb-1">Jenis Pekerjaan *</label>
                    <select name="job_type"
                            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-brand-500 @error('job_type') border-red-400 @enderror"
                            x-model="jobType" required>
                        <option value="">Pilih Jenis Pekerjaan</option>
                        <option value="INSTALASI" @selected(old('job_type', $task->job_type ?? '') === 'INSTALASI')>Instalasi</option>
                        <option value="RELOKASI" @selected(old('job_type', $task->job_type ?? '') === 'RELOKASI')>Relokasi</option>
                        <option value="PMCM" @selected(old('job_type', $task->job_type ?? '') === 'PMCM')>PMCM</option>
                        <option value="DISMANTLE" @selected(old('job_type', $task->job_type ?? '') === 'DISMANTLE')>Dismantle</option>
                        <option value="SURVEY" @selected(old('job_type', $task->job_type ?? '') === 'SURVEY')>Survey</option>
                    </select>
                    @error('job_type') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-500 mb-1">Lokasi *</label>
                    <select name="location_id"
                            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-brand-500 @error('location_id') border-red-400 @enderror"
                            required>
                        <option value="">Pilih Lokasi</option>
                        @foreach($locations as $loc)
                            <option value="{{ $loc->id }}" @selected(old('location_id', $task->location_id ?? '') == $loc->id)>{{ $loc->remote_name }}</option>
                        @endforeach
                    </select>
                    @error('location_id') <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
                </div>
            </div>
        </div>

        {{-- Items Section --}}
        <div>
            <div class="flex items-center justify-between mb-3">
                <h3 class="text-sm font-semibold text-gray-700">Item Anggaran</h3>
                <button type="button" @click="addRow()"
                        class="px-3 py-1.5 bg-brand-50 text-brand-600 text-xs rounded-lg hover:bg-brand-100 font-medium border border-brand-200">
                    + Tambah Item
                </button>
            </div>

            {{-- Item Rows --}}
            <div class="space-y-3">
                <template x-for="(item, index) in items" :key="index">
                    <div class="bg-gray-50 rounded-lg p-3 border border-gray-100 relative">
                        <button type="button" @click="removeRow(index)"
                                class="absolute top-2 right-2 text-gray-400 hover:text-red-500"
                                x-show="items.length > 1">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                        </button>

                        <div class="grid grid-cols-12 gap-3 items-end">
                            {{-- Template Dropdown --}}
                            <div class="col-span-5">
                                <label class="block text-xs font-semibold text-gray-500 mb-1">Kategori</label>
                                <select :name="'items[' + index + '][template_id]'"
                                        x-model="item.template_id"
                                        @change="onTemplateChange(index)"
                                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-brand-500"
                                        required>
                                    <option value="">Pilih Kategori</option>
                                    @foreach($templates as $tpl)
                                        <option value="{{ $tpl->id }}"
                                                data-pagu="{{ $tpl->pagu_amount }}"
                                                data-jt-pagu='@json($tpl->paguAmounts->pluck('amount', 'job_type'))'
                                                data-name="{{ $tpl->category_name }}">
                                            {{ $tpl->category_name }}
                                            @if($tpl->pagu_amount)
                                                (PAGU: Rp{{ number_format($tpl->pagu_amount, 0, ',', '.') }})
                                            @endif
                                        </option>
                                    @endforeach
                                </select>
                            </div>

                            {{-- Estimated Amount --}}
                            <div class="col-span-3">
                                <label class="block text-xs font-semibold text-gray-500 mb-1">Estimasi (Rp)</label>
                                <input type="number" :name="'items[' + index + '][estimated_amount]'"
                                       x-model.number="item.estimated_amount"
                                       class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm text-right focus:ring-2 focus:ring-brand-500"
                                       min="0" required placeholder="0">
                            </div>

                            {{-- Tanggal --}}
                            <div class="col-span-2">
                                <label class="block text-xs font-semibold text-gray-500 mb-1">Tanggal</label>
                                <input type="date" :name="'items[' + index + '][tanggal]'"
                                       x-model="item.tanggal"
                                       class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-brand-500"
                                       required>
                            </div>

                            {{-- Pagu Info --}}
                            <div class="col-span-2">
                                <span x-show="item.pagu > 0" x-text="'PAGU: ' + formatRupiah(item.pagu)"
                                      class="text-xs text-amber-600 font-medium bg-amber-50 px-2 py-1 rounded"></span>
                            </div>
                        </div>
                    </div>
                </template>
            </div>

            @error('items')
                <p class="text-red-500 text-xs mt-2">{{ $message }}</p>
            @enderror
            @error('items.*')
                <p class="text-red-500 text-xs mt-2">Ada item yang belum lengkap.</p>
            @enderror
        </div>

        {{-- Total Estimated --}}
        <div class="bg-gray-50 rounded-lg p-4 flex items-center justify-between">
            <span class="text-sm font-semibold text-gray-700">Total Estimasi</span>
            <span class="text-lg font-bold text-brand-600" x-text="'Rp ' + formatRupiah(totalEstimated)"></span>
        </div>

        {{-- Action Buttons --}}
        <div class="flex justify-between pt-2">
            <div>
                @if(isset($task))
                    <button type="button"
                            onclick="if(confirm('Hapus draft ini?')){document.getElementById('deleteForm').submit()}"
                            class="px-4 py-2 bg-red-50 text-red-600 text-sm rounded-lg hover:bg-red-100 font-medium">
                        Hapus Draft
                    </button>
                @endif
            </div>
            <div class="flex gap-2">
                <a href="{{ route('web.budget.index') }}" class="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 font-medium">Batal</a>
                <button type="submit" name="action" value="draft"
                        class="px-4 py-2 bg-gray-600 text-white text-sm rounded-lg hover:bg-gray-700 font-medium">
                    💾 Simpan Draft
                </button>
                <button type="submit" name="action" value="submit"
                        class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">
                    📤 Submit Estimasi
                </button>
            </div>
        </div>
    </form>

    @if(isset($task))
    <form id="deleteForm" method="POST" action="{{ route('web.budget.destroy', $task) }}" class="hidden">
        @csrf @method('DELETE')
    </form>
    @endif
</div>
@endsection

<script>
    function budgetForm() {
        return {
            items: [],
            jobType: '{{ old('job_type', $task->job_type ?? '') }}',

            init(serverItems) {
                if (serverItems && serverItems.length > 0) {
                    this.items = serverItems;
                } else {
                    this.addRow();
                }
            },

            addRow() {
                this.items.push({
                    template_id: '',
                    estimated_amount: 0,
                    tanggal: '',
                    pagu: null,
                    name: '',
                });
            },

            removeRow(index) {
                if (this.items.length > 1) {
                    this.items.splice(index, 1);
                }
            },

            onTemplateChange(index) {
                const item = this.items[index];
                const select = document.querySelector('select[name="items[' + index + '][template_id]"]');
                if (!select) return;
                const opt = select.selectedOptions[0];
                if (!opt) {
                    item.pagu = null;
                    return;
                }

                const jtPaguRaw = opt.dataset.jtPagu;
                if (jtPaguRaw && this.jobType) {
                    try {
                        const jtPagu = JSON.parse(jtPaguRaw);
                        if (jtPagu[this.jobType]) {
                            item.pagu = parseInt(jtPagu[this.jobType]);
                            return;
                        }
                    } catch (e) {}
                }
                item.pagu = opt.dataset.pagu ? parseInt(opt.dataset.pagu) : null;
            },

            prepareSubmit() {
                // Alpine handles binding
            },

            get totalEstimated() {
                return this.items.reduce((sum, item) => sum + (parseInt(item.estimated_amount) || 0), 0);
            },

            formatRupiah(val) {
                return new Intl.NumberFormat('id-ID').format(val || 0);
            },
        };
    }
</script>

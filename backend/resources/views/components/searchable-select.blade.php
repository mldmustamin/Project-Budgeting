<div
    x-data="{
        open: false,
        search: '',
        selected: '{{ $value ? \App\Models\BudgetItemTemplate::find($value)?->category_name ?? '' : '' }}',
        selectedId: '{{ $value }}',
        get options() {
            if (!this.search) return {{ json_encode($options ?? \App\Models\BudgetItemTemplate::orderBy('category_name')->get(['id','category_name','pagu_type','pagu_amount'])->toArray()) }};
            return {{ json_encode($options ?? \App\Models\BudgetItemTemplate::orderBy('category_name')->get(['id','category_name','pagu_type','pagu_amount'])->toArray()) }}.filter(o =>
                o.category_name.toLowerCase().includes(this.search.toLowerCase())
            );
        },
        select(opt) {
            this.selected = opt.category_name;
            this.selectedId = opt.id;
            this.open = false;
            this.search = '';
            $dispatch('template-selected', opt);
        }
    }"
    @click.away="open = false"
    class="relative"
>
    <label class="block text-xs font-semibold text-gray-500 mb-1">{{ $label ?? 'Kategori' }}</label>
    <div class="relative">
        <input type="text"
               x-model="search"
               @focus="open = true; search = selected"
               @click="open = true; search = selected"
               :placeholder="selected || 'Cari kategori...'"
               class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-brand-500">
        <span x-show="selected" @click="selected=''; selectedId=''; search=''; $dispatch('template-selected', {id:null})"
              class="absolute right-3 top-1/2 -translate-y-1/2 cursor-pointer text-gray-400 hover:text-gray-600">✕</span>
    </div>
    <input type="hidden" name="{{ $name }}" x-bind:value="selectedId">

    <div x-show="open" class="absolute z-50 w-full bg-white border border-gray-200 rounded-lg shadow-lg mt-1 max-h-60 overflow-y-auto">
        <template x-for="opt in options" :key="opt.id">
            <div @click="select(opt)"
                 class="px-3 py-2 text-sm hover:bg-brand-50 cursor-pointer flex justify-between items-center">
                <span>
                    <span x-text="opt.category_name"></span>
                    <span x-show="opt.pagu_type" class="ml-1 text-xs text-gray-400" x-text="'('+opt.pagu_type+')'"></span>
                </span>
                <span x-show="opt.pagu_amount" class="text-xs text-gray-400" x-text="'Rp '+opt.pagu_amount?.toLocaleString('id-ID')"></span>
            </div>
        </template>
        <div x-show="options.length === 0" class="px-3 py-2 text-sm text-gray-400">Tidak ditemukan</div>
    </div>
</div>

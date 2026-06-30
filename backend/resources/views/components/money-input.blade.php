<div x-data="{
    display: '{{ $value ? number_format((int) $value, 0, ',', '.') : '' }}',
    get actual() { return this.display.replace(/[^0-9]/g, '') },
    init() {
        this.$watch('display', v => {
            let num = v.replace(/[^0-9]/g, '')
            if (!num) { this.display = ''; return }
            this.display = parseInt(num).toLocaleString('id-ID')
        })
    }
}">
    <label class="block text-xs font-semibold text-gray-500 mb-1">{{ $label }}</label>
    <div class="relative">
        <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">Rp</span>
        <input
            type="text"
            inputmode="numeric"
            x-model="display"
            name="{{ $name }}"
            class="w-full border border-gray-300 rounded-lg pl-10 pr-3 py-2 text-sm focus:ring-2 focus:ring-brand-500 @error($name) border-red-400 @enderror"
            placeholder="0"
            {{ $attributes }}
        >
        <input type="hidden" name="{{ $name }}" x-bind:value="actual">
    </div>
    @error($name) <p class="text-red-500 text-xs mt-1">{{ $message }}</p> @enderror
</div>

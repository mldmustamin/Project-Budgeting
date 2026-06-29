@extends('layouts.app')

@section('title', 'Equipment Options')

@section('content')
<div class="flex items-center justify-between mb-4">
    <h2 class="text-lg font-semibold text-gray-800">Equipment Options</h2>
    <button onclick="document.getElementById('createModal').classList.remove('hidden')" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">+ Tambah Option</button>
</div>

{{-- Filters --}}
<form method="GET" class="flex flex-wrap gap-3 mb-4 bg-white rounded-xl border border-gray-200 p-4">
    <div>
        <label class="block text-xs font-semibold text-gray-500 mb-1">Field Key</label>
        <select name="field_key" class="border border-gray-300 rounded-lg px-3 py-2 text-sm">
            <option value="">Semua</option>
            @foreach($fieldKeys as $key)
                <option value="{{ $key }}" @selected(request('field_key') == $key)>{{ $key }}</option>
            @endforeach
        </select>
    </div>
    <div class="flex items-end">
        <button type="submit" class="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200">Filter</button>
    </div>
</form>

{{-- Table --}}
<div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
    <table class="w-full text-sm">
        <thead class="bg-gray-50">
            <tr>
                <th class="px-5 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Field Key</th>
                <th class="px-5 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Label</th>
                <th class="px-5 py-3 text-center text-xs font-semibold text-gray-500 uppercase w-24">Sort Order</th>
                <th class="px-5 py-3 text-center text-xs font-semibold text-gray-500 uppercase w-20">Status</th>
                <th class="px-5 py-3 text-right text-xs font-semibold text-gray-500 uppercase w-20">Aksi</th>
            </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
            @forelse($options as $opt)
            <tr class="hover:bg-gray-50">
                <td class="px-5 py-3 text-gray-900 font-mono text-xs">{{ $opt->field_key }}</td>
                <td class="px-5 py-3 text-gray-900">{{ $opt->label }}</td>
                <td class="px-5 py-3 text-center text-gray-500">{{ $opt->sort_order }}</td>
                <td class="px-5 py-3 text-center">
                    @if($opt->is_active)
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">Active</span>
                    @else
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-500">Inactive</span>
                    @endif
                </td>
                <td class="px-5 py-3 text-right">
                    <form method="POST" action="{{ route('web.equipment.destroy', $opt) }}" class="inline" onsubmit="return confirm('Hapus option ini?')">
                        @csrf @method('DELETE')
                        <button class="text-red-500 hover:underline text-xs">Hapus</button>
                    </form>
                </td>
            </tr>
            @empty
            <tr><td colspan="5" class="px-5 py-8 text-center text-gray-400">Belum ada equipment option.</td></tr>
            @endforelse
        </tbody>
    </table>
</div>
<div class="mt-4">{{ $options->links() }}</div>

{{-- Create Modal --}}
<div id="createModal" class="hidden fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
    <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl mx-4">
        <h3 class="text-lg font-semibold mb-4">Tambah Equipment Option</h3>
        <form method="POST" action="{{ route('web.equipment.store') }}" class="space-y-3">
            @csrf
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Field Key *</label>
                <input type="text" name="field_key" value="{{ old('field_key') }}"
                       class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                       required maxlength="100" placeholder="Contoh: equipment_type">
            </div>
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Label *</label>
                <input type="text" name="label" value="{{ old('label') }}"
                       class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                       required maxlength="255" placeholder="Contoh: Excavator">
            </div>
            <div>
                <label class="block text-xs font-semibold text-gray-500 mb-1">Sort Order</label>
                <input type="number" name="sort_order" value="{{ old('sort_order', 0) }}"
                       class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                       min="0" placeholder="0">
            </div>
            <div class="flex justify-end gap-2 pt-2">
                <button type="button" onclick="document.getElementById('createModal').classList.add('hidden')" class="px-4 py-2 bg-gray-100 text-sm rounded-lg">Batal</button>
                <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg">Simpan</button>
            </div>
        </form>
    </div>
</div>
@endsection

@extends('layouts.app')

@section('title', 'Master Data')

@section('content')
<div x-data="{ tab: 'locations' }" class="max-w-7xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-semibold text-gray-800 mb-6">Master Data</h1>

    {{-- tabs --}}
    <nav class="flex gap-1 border-b border-gray-200 mb-6">
        @foreach([
            'locations' => 'Lokasi',
            'templates' => 'Kategori Budget',
            'equipment' => 'Opsi Equipment',
        ] as $key => $label)
            <button @click="tab = '{{ $key }}'"
                :class="tab === '{{ $key }}' ? 'border-green-600 text-green-700 bg-green-50' : 'border-transparent text-gray-500 hover:text-gray-700'"
                class="px-4 py-2 text-sm font-medium border-b-2 transition">
                {{ $label }}
            </button>
        @endforeach
    </nav>

    {{-- Locations tab --}}
    <div x-show="tab === 'locations'" x-cloak>
        <div class="bg-white rounded-lg shadow-sm border p-4 mb-4">
            <h2 class="text-lg font-medium mb-3">Tambah Lokasi Baru</h2>
            <form method="POST" action="{{ route('web.locations.store') }}" class="grid grid-cols-1 md:grid-cols-3 gap-3">
                @csrf
                <input name="remote_name" placeholder="Nama Site" required class="border rounded px-3 py-2 text-sm" maxlength="100">
                <input name="address" placeholder="Alamat" required class="border rounded px-3 py-2 text-sm" maxlength="255">
                <select name="project_id" required class="border rounded px-3 py-2 text-sm">
                    <option value="">-- Pilih Project --</option>
                    @foreach(\App\Models\Project::orderBy('name')->get() as $p)
                        <option value="{{ $p->id }}">{{ $p->name }}</option>
                    @endforeach
                </select>
                <input name="provinsi" placeholder="Provinsi" class="border rounded px-3 py-2 text-sm" maxlength="100">
                <input name="kota_kab" placeholder="Kota/Kab" class="border rounded px-3 py-2 text-sm" maxlength="100">
                <button type="submit" class="bg-green-600 text-white px-4 py-2 rounded text-sm hover:bg-green-700">Simpan</button>
            </form>
        </div>

        <div class="bg-white rounded-lg shadow-sm border overflow-hidden">
            <table class="w-full text-sm">
                <thead class="bg-gray-50 text-gray-500 uppercase text-xs">
                    <tr>
                        <th class="px-4 py-3 text-left">Nama</th>
                        <th class="px-4 py-3 text-left">Project</th>
                        <th class="px-4 py-3 text-left">Alamat</th>
                        <th class="px-4 py-3 text-left">Dibuat</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                    @foreach($locations as $loc)
                    <tr class="hover:bg-gray-50">
                        <td class="px-4 py-2">{{ $loc->remote_name }}</td>
                        <td class="px-4 py-2 text-gray-500">{{ $loc->project?->name }}</td>
                        <td class="px-4 py-2 text-gray-500 text-xs truncate max-w-xs">{{ $loc->address }}</td>
                        <td class="px-4 py-2 text-gray-400 text-xs">{{ $loc->createdBy?->name }}</td>
                    </tr>
                    @endforeach
                </tbody>
            </table>
            <div class="px-4 py-3">{{ $locations->appends(['tab' => 'locations'])->links() }}</div>
        </div>
    </div>

    {{-- Budget Templates tab --}}
    <div x-show="tab === 'templates'" x-cloak>
        <div class="bg-white rounded-lg shadow-sm border overflow-hidden">
            <table class="w-full text-sm">
                <thead class="bg-gray-50 text-gray-500 uppercase text-xs">
                    <tr>
                        <th class="px-4 py-3 text-left">Kategori</th>
                        <th class="px-4 py-3 text-left">Tipe Pagu</th>
                        <th class="px-4 py-3 text-left">Unit</th>
                        <th class="px-4 py-3 text-left">Wajib Bukti</th>
                        <th class="px-4 py-3 text-left">Tarif (per Job Type)</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                    @foreach($templates as $tpl)
                    <tr class="hover:bg-gray-50">
                        <td class="px-4 py-2 font-medium">{{ $tpl->name }}</td>
                        <td class="px-4 py-2">
                            <span class="text-xs px-2 py-0.5 rounded-full
                                @if($tpl->pagu_type === 'FIXED_PAGU') bg-blue-100 text-blue-700
                                @elseif($tpl->pagu_type === 'TICKET') bg-amber-100 text-amber-700
                                @else bg-purple-100 text-purple-700
                                @endif">
                                {{ $tpl->pagu_type }}
                            </span>
                        </td>
                        <td class="px-4 py-2 text-gray-500">{{ $tpl->unit }}</td>
                        <td class="px-4 py-2">{{ $tpl->wajib_bukti ? '✓' : '—' }}</td>
                        <td class="px-4 py-2 text-xs text-gray-500">
                            @foreach($tpl->paguAmounts as $pa)
                                <div>{{ $pa->job_type }}: Rp {{ number_format($pa->amount, 0, ',', '.') }}</div>
                            @endforeach
                        </td>
                    </tr>
                    @endforeach
                </tbody>
            </table>
        </div>
    </div>

    {{-- Equipment tab --}}
    <div x-show="tab === 'equipment'" x-cloak>
        <div class="bg-white rounded-lg shadow-sm border p-4 mb-4">
            <h2 class="text-lg font-medium mb-3">Tambah Equipment Option</h2>
            <form method="POST" action="{{ route('web.equipment.store') }}" class="flex gap-3 items-end flex-wrap">
                @csrf
                <select name="field_key" required class="border rounded px-3 py-2 text-sm">
                    <option value="">-- Field --</option>
                    @foreach(\App\Models\MasterEquipmentOption::distinct()->pluck('field_key') as $fk)
                        <option value="{{ $fk }}">{{ $fk }}</option>
                    @endforeach
                </select>
                <input name="value" placeholder="Nilai" required class="border rounded px-3 py-2 text-sm" maxlength="100">
                <button type="submit" class="bg-green-600 text-white px-4 py-2 rounded text-sm hover:bg-green-700">Tambah</button>
            </form>
        </div>

        <div class="bg-white rounded-lg shadow-sm border overflow-hidden">
            <table class="w-full text-sm">
                <thead class="bg-gray-50 text-gray-500 uppercase text-xs">
                    <tr>
                        <th class="px-4 py-3 text-left">Field Key</th>
                        <th class="px-4 py-3 text-left">Value</th>
                        <th class="px-4 py-3 text-right">Action</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                    @foreach($equipment as $eq)
                    <tr class="hover:bg-gray-50">
                        <td class="px-4 py-2 text-xs text-gray-500">{{ $eq->field_key }}</td>
                        <td class="px-4 py-2">{{ $eq->value }}</td>
                        <td class="px-4 py-2 text-right">
                            <form method="POST" action="{{ route('web.equipment.destroy', $eq) }}" class="inline" onsubmit="return confirm('Hapus?')">
                                @csrf @method('DELETE')
                                <button class="text-red-500 hover:text-red-700 text-xs">Hapus</button>
                            </form>
                        </td>
                    </tr>
                    @endforeach
                </tbody>
            </table>
            <div class="px-4 py-3">{{ $equipment->appends(['tab' => 'equipment'])->links() }}</div>
        </div>
    </div>
</div>
@endsection

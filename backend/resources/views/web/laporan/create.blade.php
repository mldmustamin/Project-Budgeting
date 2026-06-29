@extends('layouts.app')
@section('title', 'Laporan Pekerjaan')
@section('content')
<div class="flex items-center justify-between mb-4">
    <h2 class="text-lg font-semibold text-gray-800">Laporan Pekerjaan</h2>
</div>

<form method="POST" action="{{ route('web.laporan.store', $task ?? '') }}" class="space-y-6">
    @csrf
    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">1. Data Tim</h3>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Nama Teknisi</label><input type="text" name="nama_teknisi" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">No HP</label><input type="text" name="no_hp_teknisi" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Koordinator</label><input type="text" name="koordinator" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Tgl Berangkat</label><input type="date" name="tgl_berangkat" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Tgl Tiba</label><input type="date" name="tgl_tiba" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Tgl Mulai</label><input type="date" name="tgl_mulai" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Tgl Selesai</label><input type="date" name="tgl_selesai" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
        </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">2. Data Customer</h3>
        <div class="grid grid-cols-2 gap-3">
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Nama Customer</label><input type="text" name="nama_customer" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">PIC Lokasi</label><input type="text" name="pic_lokasi" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div class="col-span-2"><label class="block text-xs font-semibold text-gray-500 mb-1">Alamat</label><textarea name="alamat_customer" rows="2" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></textarea></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">IP LAN</label><input type="text" name="ip_lan" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
        </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">3. Parameter Sinyal</h3>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Hub/Satelite</label><input type="text" name="hub_satelite" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">SQF Awal</label><input type="text" name="sqf_awal" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">SQF Pointing</label><input type="text" name="sqf_pointing" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Target ESNO</label><input type="text" name="target_esno" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Signal Telkomsel</label><input type="text" name="signal_telkomsel" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Signal Indosat</label><input type="text" name="signal_indosat" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
        </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">4. Sarpen</h3>
        <div class="grid grid-cols-2 gap-3">
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Kondisi AC</label><input type="text" name="kondisi_ac" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Sumber Elektrikal</label><input type="text" name="sumber_elektrikal" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></div>
        </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">5. Tindakan</h3>
        <div><label class="block text-xs font-semibold text-gray-500 mb-1">Tindakan Teknisi</label><textarea name="tindakan_teknisi" rows="3" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm mb-3"></textarea></div>
        <div><label class="block text-xs font-semibold text-gray-500 mb-1">Tindakan FLM</label><textarea name="tindakan_flm" rows="3" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></textarea></div>
    </div>

    <div class="bg-white rounded-xl border border-gray-200 p-5 shadow-sm">
        <h3 class="text-sm font-semibold text-gray-700 mb-3">6. Catatan</h3>
        <div><label class="block text-xs font-semibold text-gray-500 mb-1">Penyebab Gangguan</label><textarea name="penyebab_gangguan" rows="2" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm mb-3"></textarea></div>
        <div><label class="block text-xs font-semibold text-gray-500 mb-1">Catatan</label><textarea name="catatan" rows="3" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"></textarea></div>
    </div>

    <div class="flex justify-between">
        <a href="{{ route('web.dashboard') }}" class="px-6 py-2.5 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200 font-medium">Batal</a>
        <button type="submit" class="px-6 py-2.5 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">Simpan Laporan</button>
    </div>
</form>
@endsection

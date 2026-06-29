@extends('layouts.app')

@section('title', 'Master Lokasi')

@section('content')
<div class="flex items-center justify-between mb-4">
    <h2 class="text-lg font-semibold text-gray-800">Master Lokasi</h2>
    <button onclick="document.getElementById('createModal').classList.remove('hidden')" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">+ Tambah Lokasi</button>
</div>

<form method="GET" class="flex flex-wrap gap-3 mb-4 bg-white rounded-xl border border-gray-200 p-4">
    <div>
        <label class="block text-xs font-semibold text-gray-500 mb-1">Project</label>
        <select name="project_id" class="border border-gray-300 rounded-lg px-3 py-2 text-sm">
            <option value="">Semua</option>
            @foreach($projects as $p)
                <option value="{{ $p->id }}" @selected(request('project_id') == $p->id)>{{ $p->name }}</option>
            @endforeach
        </select>
    </div>
    <div>
        <label class="block text-xs font-semibold text-gray-500 mb-1">Cari</label>
        <input type="text" name="search" value="{{ request('search') }}" placeholder="Nama lokasi..." class="border border-gray-300 rounded-lg px-3 py-2 text-sm">
    </div>
    <div class="flex items-end">
        <button type="submit" class="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200">Filter</button>
    </div>
</form>

<div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
    <table class="w-full text-sm">
        <thead class="bg-gray-50">
            <tr>
                <th class="px-5 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Nama</th>
                <th class="px-5 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Project</th>
                <th class="px-5 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Alamat</th>
                <th class="px-5 py-3 text-left text-xs font-semibold text-gray-500 uppercase">Kota/Prov</th>
                <th class="px-5 py-3 text-right text-xs font-semibold text-gray-500 uppercase">Aksi</th>
            </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
            @forelse($locations as $loc)
            <tr class="hover:bg-gray-50">
                <td class="px-5 py-3 text-gray-900 font-medium">{{ $loc->remote_name }}</td>
                <td class="px-5 py-3 text-gray-500">{{ $loc->project?->name ?? '-' }}</td>
                <td class="px-5 py-3 text-gray-500 text-xs">{{ $loc->address ?? '-' }}</td>
                <td class="px-5 py-3 text-gray-500 text-xs">{{ implode(', ', array_filter([$loc->city, $loc->province])) ?: '-' }}</td>
                <td class="px-5 py-3 text-right">
                    <button onclick="editLocation({{ $loc->id }}, '{{ $loc->remote_name }}', '{{ $loc->project_id }}', '{{ $loc->address }}', '{{ $loc->city }}', '{{ $loc->province }}')" class="text-brand-600 hover:underline text-xs mr-2">Edit</button>
                    <form method="POST" action="{{ route('web.locations.destroy', $loc) }}" class="inline" onsubmit="return confirm('Hapus lokasi ini?')">
                        @csrf @method('DELETE')
                        <button class="text-red-500 hover:underline text-xs">Hapus</button>
                    </form>
                </td>
            </tr>
            @empty
            <tr><td colspan="5" class="px-5 py-8 text-center text-gray-400">Belum ada lokasi.</td></tr>
            @endforelse
        </tbody>
    </table>
</div>
<div class="mt-4">{{ $locations->links() }}</div>

{{-- Create Modal --}}
<div id="createModal" class="hidden fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
    <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-xl mx-4">
        <h3 class="text-lg font-semibold mb-4">Tambah Lokasi</h3>
        <form method="POST" action="{{ route('web.locations.store') }}" class="space-y-3">
            @csrf
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Project *</label>
                <select name="project_id" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                    <option value="">Pilih Project</option>
                    @foreach($projects as $p)<option value="{{ $p->id }}">{{ $p->name }}</option>@endforeach
                </select>
            </div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Nama Lokasi *</label>
                <input type="text" name="remote_name" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" required>
            </div>
            <div><label class="block text-xs font-semibold text-gray-500 mb-1">Alamat</label>
                <input type="text" name="address" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
            </div>
            <div class="grid grid-cols-2 gap-3">
                <div><label class="block text-xs font-semibold text-gray-500 mb-1">Kota</label>
                    <input type="text" name="city" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                </div>
                <div><label class="block text-xs font-semibold text-gray-500 mb-1">Provinsi</label>
                    <input type="text" name="province" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                </div>
            </div>
            <div class="flex justify-end gap-2 pt-2">
                <button type="button" onclick="document.getElementById('createModal').classList.add('hidden')" class="px-4 py-2 bg-gray-100 text-sm rounded-lg">Batal</button>
                <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg">Simpan</button>
            </div>
        </form>
    </div>
</div>

<script>
function editLocation(id, name, projectId, address, city, province) {
    document.getElementById('createModal').classList.remove('hidden');
    // Simple: reload with edit params or use inline form
    // For now, user can delete and re-create, or we can add edit modal later
}
</script>
@endsection

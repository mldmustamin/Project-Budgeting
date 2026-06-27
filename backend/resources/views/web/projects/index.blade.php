@extends('layouts.app')

@section('title', 'Project')

@section('content')
<div class="bg-white rounded-xl border border-gray-200">
    <div class="px-5 py-3 border-b border-gray-100 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-700">Daftar Project ({{ $projects->total() }})</h3>
        @if(auth()->user()?->hasRole(['OWNER','ADMIN']))
        <button onclick="document.getElementById('createModal').classList.remove('hidden')" class="px-3 py-1.5 bg-brand-600 text-white text-xs rounded-lg hover:bg-brand-700 font-medium">+ Tambah</button>
        @endif
    </div>
    <div class="overflow-x-auto">
        <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left">
                <tr>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Nama</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">UUID</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Owner</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Status</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Transaksi</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Dibuat</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($projects as $project)
                <tr class="hover:bg-gray-50">
                    <td class="px-5 py-3"><a href="{{ route('web.projects.show', $project) }}" class="text-brand-600 hover:text-brand-800 font-medium text-sm">{{ $project->name }}</a></td>
                    <td class="px-5 py-3 text-gray-500 font-mono text-xs">{{ \Illuminate\Support\Str::limit($project->uuid, 12, '') }}</td>
                    <td class="px-5 py-3 text-gray-600">{{ $project->user?->name ?? '-' }}</td>
                    <td class="px-5 py-3">
                        @if($project->is_archived)
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600">Archived</span>
                        @elseif($project->completed_at)
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Completed</span>
                        @else
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">Active</span>
                        @endif
                    </td>
                    <td class="px-5 py-3 text-gray-600">{{ $project->transactions_count }}</td>
                    <td class="px-5 py-3 text-gray-500 whitespace-nowrap">{{ $project->created_at->format('d M Y') }}</td>
                </tr>
                @empty
                <tr>
                    <td colspan="6" class="px-5 py-8 text-center text-gray-400">Belum ada project.</td>
                </tr>
                @endforelse
            </tbody>
        </table>
    </div>
    @if($projects->hasPages())
    <div class="px-5 py-3 border-t border-gray-100">
        {{ $projects->links() }}
    </div>
    @endif
</div>

{{-- Create Modal --}}
<div id="createModal" class="hidden fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
    <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl" onclick="event.stopPropagation()">
        <h4 class="text-sm font-semibold text-gray-800 mb-4">Tambah Project</h4>
        <form method="POST" action="{{ route('web.projects.store') }}">
            @csrf
            <input name="name" required placeholder="Nama project" class="w-full border rounded-lg px-3 py-2 text-sm mb-3 focus:ring-2 focus:ring-brand-500">
            <textarea name="description" rows="2" placeholder="Deskripsi" class="w-full border rounded-lg px-3 py-2 text-sm mb-3"></textarea>
            <div class="grid grid-cols-2 gap-3 mb-3">
                <div><label class="text-xs text-gray-500">Mulai</label><input type="date" name="start_at" class="w-full border rounded-lg px-3 py-2 text-sm mt-1"></div>
                <div><label class="text-xs text-gray-500">Selesai</label><input type="date" name="completed_at" class="w-full border rounded-lg px-3 py-2 text-sm mt-1"></div>
            </div>
            <div class="flex gap-2 justify-end">
                <button type="button" onclick="document.getElementById('createModal').classList.add('hidden')" class="px-4 py-2 bg-gray-100 text-sm rounded-lg">Batal</button>
                <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700">Simpan</button>
            </div>
        </form>
    </div>
</div>
@endsection

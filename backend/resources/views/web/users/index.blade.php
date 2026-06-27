@extends('layouts.app')
@section('title', 'User Management')
@section('content')

<div class="mb-4">
    <button onclick="document.getElementById('createModal').classList.remove('hidden')"
            class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700 font-medium">+ Tambah User</button>
</div>

<div id="createModal" class="hidden fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
    <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl" onclick="event.stopPropagation()">
        <h4 class="text-sm font-semibold text-gray-800 mb-4">Tambah User</h4>
        <form method="POST" action="{{ route('web.users.store') }}">
            @csrf
            <input name="name" required placeholder="Nama" class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
            <input name="employee_id" required placeholder="ID Karyawan (angka)" class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
            <input name="email" type="email" placeholder="Email (opsional)" class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
            <select name="role" required class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
                <option value="">Pilih Role</option>
                @foreach($roles as $role)<option value="{{ $role->name }}">{{ $role->name }}</option>@endforeach
            </select>
            <div class="flex gap-2 justify-end">
                <button type="button" onclick="document.getElementById('createModal').classList.add('hidden')" class="px-4 py-2 bg-gray-100 text-sm rounded-lg">Batal</button>
                <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700">Simpan</button>
            </div>
        </form>
    </div>
</div>

<div class="bg-white rounded-xl border border-gray-200">
    <div class="px-5 py-3 border-b border-gray-100">
        <h3 class="text-sm font-semibold text-gray-700">Daftar User ({{ $users->total() }})</h3>
    </div>
    <div class="overflow-x-auto">
        <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left">
                <tr>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Nama</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">ID Karyawan</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Email</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Role</th>
                    <th class="px-5 py-3 text-xs font-semibold text-gray-500 uppercase">Dibuat</th>
                    <th class="px-5 py-3"></th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
                @forelse($users as $user)
                <tr class="hover:bg-gray-50">
                    <td class="px-5 py-3 text-gray-900 font-medium">{{ $user->name }}</td>
                    <td class="px-5 py-3 text-gray-600 text-xs font-mono">{{ $user->employee_id ?? '-' }}</td>
                    <td class="px-5 py-3 text-gray-600 text-xs">{{ $user->email ?: '-' }}</td>
                    <td class="px-5 py-3">
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-brand-50 text-brand-700 ring-1 ring-brand-200">{{ $user->roles->first()?->name ?? '-' }}</span>
                    </td>
                    <td class="px-5 py-3 text-gray-500 text-xs">{{ $user->created_at->format('d M Y') }}</td>
                    <td class="px-5 py-3">
                        <form method="POST" action="{{ route('web.users.reset-password', $user) }}" class="inline mr-2">@csrf @method('PATCH')<button class="text-xs text-red-600 hover:text-red-800 font-medium">Reset Pwd</button></form>
                        <button onclick="document.getElementById('editModal{{$user->id}}').classList.remove('hidden')" class="text-xs text-brand-600 font-medium">Edit</button>
                        <div id="editModal{{$user->id}}" class="hidden fixed inset-0 z-50 flex items-center justify-center" style="background: rgba(0,0,0,0.4)">
                            <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl" onclick="event.stopPropagation()">
                                <h4 class="text-sm font-semibold text-gray-800 mb-4">Edit User</h4>
                                <form method="POST" action="{{ route('web.users.update', $user) }}">
                                    @csrf @method('PATCH')
                                    <input name="name" value="{{ $user->name }}" required class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
                                    <input name="employee_id" value="{{ $user->employee_id }}" required class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
                                    <input name="email" value="{{ $user->email }}" class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
                                    <select name="role" required class="w-full border rounded-lg px-3 py-2 text-sm mb-3">
                                        @foreach($roles as $role)
                                            <option value="{{ $role->name }}" @selected($user->hasRole($role->name))>{{ $role->name }}</option>
                                        @endforeach
                                    </select>
                                    <div class="flex gap-2 justify-end">
                                        <button type="button" onclick="document.getElementById('editModal{{$user->id}}').classList.add('hidden')" class="px-4 py-2 bg-gray-100 text-sm rounded-lg">Batal</button>
                                        <button type="submit" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg hover:bg-brand-700">Simpan</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </td>
                </tr>
                @empty
                <tr><td colspan="5" class="px-5 py-8 text-center text-gray-400">Belum ada user.</td></tr>
                @endforelse
            </tbody>
        </table>
    </div>
    @if($users->hasPages())<div class="px-5 py-3 border-t border-gray-100">{{ $users->links() }}</div>@endif
</div>
@endsection

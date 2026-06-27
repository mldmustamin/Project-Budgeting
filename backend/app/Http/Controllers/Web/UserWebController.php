<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\View\View;
use Spatie\Permission\Models\Role;

class UserWebController extends Controller
{
    public function index(): View
    {
        $users = User::with('roles')->orderBy('name')->paginate(25);
        $roles = Role::all();
        return view('web.users.index', compact('users', 'roles'));
    }

    public function store(Request $request): RedirectResponse
    {
        if (! $request->user()->hasRole(['OWNER', 'ADMIN'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $v = $request->validate([
            'name' => 'required|string|max:255',
            'employee_id' => 'required|string|unique:users',
            'email' => 'nullable|email|unique:users',
            'role' => 'required|string|exists:roles,name',
        ]);
        $generatedPassword = Str::random(8);
        $user = User::create([
            'uuid' => (string) Str::uuid(),
            'name' => $v['name'],
            'employee_id' => $v['employee_id'],
            'email' => $v['email'],
            'password' => bcrypt($generatedPassword),
            'password_change_required' => true,
        ]);
        $user->assignRole($v['role']);
        return back()->with('success', "User '{$v['name']}' dibuat. Password: {$generatedPassword}");
    }

    public function update(Request $request, User $user): RedirectResponse
    {
        if (! $request->user()->hasRole(['OWNER', 'ADMIN'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $v = $request->validate([
            'name' => 'required|string|max:255',
            'employee_id' => 'required|string|unique:users,employee_id,'.$user->id,
            'email' => 'nullable|email|unique:users,email,'.$user->id,
            'role' => 'required|string|exists:roles,name',
        ]);
        $user->update(['name' => $v['name'], 'employee_id' => $v['employee_id'], 'email' => $v['email']]);
        $user->syncRoles([$v['role']]);
        return back()->with('success', "User '{$v['name']}' diupdate.");
    }

    public function resetPassword(Request $request, User $user): RedirectResponse
    {
        if (! $request->user()->hasRole(['OWNER', 'ADMIN'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $newPassword = Str::random(8);
        $user->update(['password' => bcrypt($newPassword), 'password_change_required' => true]);
        return back()->with('success', "Password '{$user->name}' direset. Password baru: {$newPassword}");
    }
}

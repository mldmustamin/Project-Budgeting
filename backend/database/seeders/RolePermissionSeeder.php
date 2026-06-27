<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Spatie\Permission\Models\Role;

class RolePermissionSeeder extends Seeder
{
    public function run(): void
    {
        // Guard: web (Blade + Livewire) — same roles used for Sanctum API via $user->can()
        $roles = [
            'OWNER',
            'ADMIN',
            'FINANCE_MANAGER',
            'SUPERVISOR',
            'PIC',
            'FIELD_ENGINEER',
            'AUDITOR',
            'VIEWER',
        ];

        foreach ($roles as $roleName) {
            Role::findOrCreate($roleName, 'web');
        }
    }
}
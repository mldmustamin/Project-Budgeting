<?php

namespace Database\Seeders;

use App\Models\Project;
use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Str;

class StagingSeeder extends Seeder
{
    public function run(): void
    {
        $this->call(RolePermissionSeeder::class);

        $admin = User::factory()->create([
            'name' => 'Super Admin',
            'employee_id' => '10001',
            'email' => 'admin@fundsmanager.test',
            'password' => bcrypt('admin'),
            'password_change_required' => false,
            'uuid' => (string) Str::uuid(),
        ]);
        $admin->assignRole('OWNER');

        $finance = User::factory()->create([
            'name' => 'Finance Staging',
            'employee_id' => '10002',
            'email' => 'finance@fundsmanager.test',
            'password' => bcrypt('password123'),
            'password_change_required' => false,
            'uuid' => (string) Str::uuid(),
        ]);
        $finance->assignRole('FINANCE_MANAGER');

        $engineer = User::factory()->create([
            'name' => 'Engineer Staging',
            'employee_id' => '10003',
            'email' => 'engineer@fundsmanager.test',
            'password' => bcrypt('password123'),
            'password_change_required' => true,
            'uuid' => (string) Str::uuid(),
        ]);
        $engineer->assignRole('FIELD_ENGINEER');

        Project::factory()->create(['user_id' => $engineer->id, 'name' => 'Project Alpha Staging', 'description' => 'Sample project']);

        $this->command?->info('Super Admin: 10001 / admin');
        $this->command?->info('Finance: 10002 / password123');
        $this->command?->info('Engineer: 10003 / password123 (harus ganti password)');
    }
}

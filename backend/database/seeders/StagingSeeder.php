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
        // Roles already seeded by RolePermissionSeeder (called by DatabaseSeeder)

        $owner = User::factory()->create([
            'name' => 'Super Admin',
            'employee_id' => '10001',
            'email' => 'admin@fundsmanager.test',
            'password' => bcrypt('admin'),
            'password_change_required' => false,
            'uuid' => (string) Str::uuid(),
        ]);
        $owner->assignRole('OWNER');

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

        $supervisor = User::factory()->create([
            'name' => 'Supervisor Staging',
            'employee_id' => '10004',
            'email' => 'supervisor@fundsmanager.test',
            'password' => bcrypt('password123'),
            'password_change_required' => false,
            'uuid' => (string) Str::uuid(),
        ]);
        $supervisor->assignRole('SUPERVISOR');

        $auditor = User::factory()->create([
            'name' => 'Auditor Staging',
            'employee_id' => '10005',
            'email' => 'auditor@fundsmanager.test',
            'password' => bcrypt('password123'),
            'password_change_required' => false,
            'uuid' => (string) Str::uuid(),
        ]);
        $auditor->assignRole('AUDITOR');

        Project::factory()->create([
            'user_id' => $engineer->id,
            'name' => 'Project Alpha Staging',
            'description' => 'Sample project',
        ]);

        $this->command?->info('Super Admin (OWNER): 10001 / admin');
        $this->command?->info('Finance Manager: 10002 / password123');
        $this->command?->info('Field Engineer: 10003 / password123 (harus ganti password)');
        $this->command?->info('Supervisor: 10004 / password123');
        $this->command?->info('Auditor: 10005 / password123');
    }
}

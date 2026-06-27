<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call(RolePermissionSeeder::class);

        // Test user for API auth testing
        $user = User::factory()->create([
            'name' => 'Test Owner',
            'email' => 'owner@fundsmanager.test',
            'password' => bcrypt('password'),
            'uuid' => \Illuminate\Support\Str::uuid()->toString(),
        ]);
        $user->assignRole('OWNER');

        $engineer = User::factory()->create([
            'name' => 'Test Engineer',
            'email' => 'engineer@fundsmanager.test',
            'password' => bcrypt('password'),
            'uuid' => \Illuminate\Support\Str::uuid()->toString(),
        ]);
        $engineer->assignRole('FIELD_ENGINEER');
    }
}
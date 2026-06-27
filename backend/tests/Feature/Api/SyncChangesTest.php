<?php

namespace Tests\Feature\Api;

use App\Models\Device;
use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SyncChangesTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_unauthenticated_user_cannot_pull_changes(): void
    {
        $response = $this->getJson('/api/v1/sync/changes?device_uuid=' . \Illuminate\Support\Str::uuid());

        $response->assertStatus(401);
    }

    public function test_missing_device_uuid_rejected(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/changes');

        $response->assertStatus(422)
            ->assertJsonPath('error', 'MISSING_DEVICE_UUID');
    }

    public function test_unregistered_device_rejected(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/changes?device_uuid=' . \Illuminate\Support\Str::uuid());

        $response->assertStatus(401)
            ->assertJsonPath('error', 'DEVICE_NOT_REGISTERED');
    }

    public function test_revoked_device_rejected(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id, 'is_revoked' => true]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/changes?device_uuid=' . $device->uuid);

        $response->assertStatus(403)
            ->assertJsonPath('error', 'DEVICE_REVOKED');
    }

    public function test_field_engineer_only_pulls_assigned_project_transactions(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);

        $assignedProject = Project::factory()->create();
        $unassignedProject = Project::factory()->create();

        ProjectAssignment::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'project_id' => $assignedProject->id,
            'user_id' => $user->id,
            'role_on_project' => 'FIELD_ENGINEER',
        ]);

        Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $assignedProject->id,
            'project_uuid' => $assignedProject->uuid,
        ]);
        Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $unassignedProject->id,
            'project_uuid' => $unassignedProject->uuid,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/changes?device_uuid=' . $device->uuid);

        $response->assertStatus(200)
            ->assertJsonCount(1, 'changes.transactions')
            ->assertJsonPath('changes.transactions.0.project_uuid', $assignedProject->uuid);
    }

    public function test_owner_can_pull_all_transactions(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);

        Transaction::factory()->count(3)->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/changes?device_uuid=' . $device->uuid);

        $response->assertStatus(200)
            ->assertJsonCount(3, 'changes.transactions');
    }

    public function test_since_cursor_filters_older_transactions(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);

        $old = Transaction::factory()->create(['updated_at' => now()->subHours(2)]);
        $new = Transaction::factory()->create(['updated_at' => now()->subMinutes(30)]);

        $token = $user->createToken('test')->plainTextToken;
        $since = now()->subHour()->toIso8601String();

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/changes?device_uuid=' . $device->uuid . '&since=' . urlencode($since));

        $response->assertStatus(200)
            ->assertJsonCount(1, 'changes.transactions')
            ->assertJsonPath('changes.transactions.0.uuid', $new->uuid);
    }

    public function test_response_includes_server_time_and_next_cursor(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);

        Transaction::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/changes?device_uuid=' . $device->uuid);

        $response->assertStatus(200)
            ->assertJsonStructure([
                'server_time',
                'next_cursor',
                'changes' => ['transactions'],
            ]);
    }
}
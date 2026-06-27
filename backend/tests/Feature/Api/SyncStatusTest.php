<?php

namespace Tests\Feature\Api;

use App\Models\Device;
use App\Models\SyncOutbox;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SyncStatusTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_unauthenticated_user_cannot_get_status(): void
    {
        $response = $this->getJson('/api/v1/sync/status?device_uuid=' . \Illuminate\Support\Str::uuid());

        $response->assertStatus(401);
    }

    public function test_missing_device_uuid_rejected(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/status');

        $response->assertStatus(422)
            ->assertJsonPath('error', 'MISSING_DEVICE_UUID');
    }

    public function test_registered_device_returns_status(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/status?device_uuid=' . $device->uuid);

        $response->assertStatus(200)
            ->assertJsonPath('device_uuid', $device->uuid)
            ->assertJsonPath('is_revoked', false)
            ->assertJsonPath('pending_outbox_count', 0)
            ->assertJsonPath('rejected_outbox_count', 0);
    }

    public function test_pending_and_rejected_counts_are_correct(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        SyncOutbox::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'user_id' => $user->id,
            'device_id' => $device->uuid,
            'session_id' => 's1',
            'entity_type' => 'transaction',
            'entity_uuid' => \Illuminate\Support\Str::uuid(),
            'operation' => 'CREATE',
            'payload' => [],
            'idempotency_key' => 'key-pending',
            'status' => 'PENDING',
        ]);
        SyncOutbox::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'user_id' => $user->id,
            'device_id' => $device->uuid,
            'session_id' => 's2',
            'entity_type' => 'transaction',
            'entity_uuid' => \Illuminate\Support\Str::uuid(),
            'operation' => 'CREATE',
            'payload' => [],
            'idempotency_key' => 'key-rejected',
            'status' => 'REJECTED',
            'rejection_reason' => 'Test rejection',
        ]);

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/status?device_uuid=' . $device->uuid);

        $response->assertStatus(200)
            ->assertJsonPath('pending_outbox_count', 1)
            ->assertJsonPath('rejected_outbox_count', 1);
    }

    public function test_last_synced_at_is_null_when_no_sync(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/status?device_uuid=' . $device->uuid);

        $response->assertStatus(200)
            ->assertJsonPath('last_synced_at', null);
    }

    public function test_revoked_device_rejected(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id, 'is_revoked' => true]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/sync/status?device_uuid=' . $device->uuid);

        $response->assertStatus(403)
            ->assertJsonPath('error', 'DEVICE_REVOKED');
    }
}
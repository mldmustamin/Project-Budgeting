<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class DeviceTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_authenticated_user_can_register_device(): void
    {
        $user = User::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/devices/register', [
                'device_name' => 'Android Test Phone',
                'device_platform' => 'android',
                'device_version' => '14.0',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('device.device_name', 'Android Test Phone')
            ->assertJsonPath('device.device_platform', 'android')
            ->assertJsonPath('device.is_revoked', false);

        $this->assertDatabaseHas('devices', [
            'user_id' => $user->id,
            'device_name' => 'Android Test Phone',
        ]);
    }

    public function test_unauthenticated_user_cannot_register_device(): void
    {
        $response = $this->postJson('/api/v1/devices/register', [
            'device_name' => 'Test',
            'device_platform' => 'android',
        ]);

        $response->assertStatus(401);
    }

    public function test_same_device_uuid_updates_existing_device(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('test')->plainTextToken;
        $deviceUuid = \Illuminate\Support\Str::uuid()->toString();

        // First registration
        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/devices/register', [
                'device_name' => 'Old Name',
                'device_platform' => 'android',
                'device_uuid' => $deviceUuid,
            ]);

        // Second registration with same UUID should update
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/devices/register', [
                'device_name' => 'New Name',
                'device_platform' => 'ios',
                'device_uuid' => $deviceUuid,
            ]);

        $response->assertStatus(200) // update returns 200
            ->assertJsonPath('device.device_name', 'New Name')
            ->assertJsonPath('device.device_platform', 'ios');

        $this->assertDatabaseCount('devices', 1);
    }
}
<?php

namespace Tests\Feature\Api;

use App\Models\MasterEquipmentOption;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MasterEquipmentOptionApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(\Database\Seeders\RolePermissionSeeder::class);
        $this->seed(\Database\Seeders\MasterEquipmentOptionSeeder::class);
    }

    /** @test */
    public function can_list_equipment_options_by_field_key()
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $response = $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/equipment-options?field_key=JENIS_ANTENNA');

        $response->assertOk()->assertJsonStructure(['data']);
    }

    /** @test */
    public function returns_empty_for_unknown_field_key()
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $response = $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/equipment-options?field_key=NONEXISTENT');

        $response->assertOk()->assertJsonCount(0, 'data');
    }

    /** @test */
    public function unauthenticated_user_cannot_access()
    {
        $response = $this->getJson('/api/v1/equipment-options?field_key=JENIS_ANTENNA');

        $response->assertUnauthorized();
    }
}

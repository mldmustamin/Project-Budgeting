<?php

namespace Tests\Feature;

use App\Models\BudgetItemTemplate;
use App\Models\MasterEquipmentOption;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class BudgetTemplateApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(\Database\Seeders\RolePermissionSeeder::class);
        $this->seed(\Database\Seeders\BudgetItemTemplateSeeder::class);
        $this->seed(\Database\Seeders\MasterEquipmentOptionSeeder::class);
    }

    /** @test */
    public function can_list_budget_templates()
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $response = $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/budget-templates');

        $response->assertOk()
            ->assertJsonPath('meta.total', 35)
            ->assertJsonPath('meta.fixed_pagu', 10)
            ->assertJsonPath('meta.ticket', 12)
            ->assertJsonPath('meta.manager_approval', 13);
    }

    /** @test */
    public function can_list_equipment_options()
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $response = $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/equipment-options');

        $response->assertOk()
            ->assertJsonStructure(['data' => ['JENIS_ANTENNA', 'JENIS_MOUNTING', 'TYPE_MODEM', 'PENYEBAB_GANGGUAN', 'FOTO_WAJIB_SCM']]);
    }

    /** @test */
    public function can_filter_equipment_options_by_field_key()
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $response = $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/equipment-options?field_key=JENIS_ANTENNA');

        $response->assertOk()
            ->assertJsonCount(9, 'data.JENIS_ANTENNA');
    }

    /** @test */
    public function unauthenticated_user_cannot_access_templates()
    {
        $this->getJson('/api/v1/budget-templates')->assertUnauthorized();
    }
}

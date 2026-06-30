<?php

namespace Tests\Feature\Api;

use App\Models\MasterLocation;
use App\Models\Project;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MasterLocationApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(\Database\Seeders\RolePermissionSeeder::class);
    }

    private function createLocation(Project $project, User $user, array $overrides = []): MasterLocation
    {
        return MasterLocation::create(array_merge([
            'uuid'       => (string) \Illuminate\Support\Str::uuid(),
            'project_id' => $project->id,
            'remote_name'=> 'Site A',
            'address'    => 'Jl. Test No. 1',
            'provinsi'   => 'Sulawesi Tenggara',
            'kota_kab'   => 'Kendari',
            'created_by' => $user->id,
        ], $overrides));
    }

    /** @test */
    public function can_list_locations()
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');
        $project = Project::factory()->create();

        $this->createLocation($project, $user);
        $this->createLocation($project, $user, ['remote_name' => 'Site B']);

        $response = $this->actingAs($user, 'sanctum')
            ->getJson("/api/v1/projects/{$project->id}/locations");

        $response->assertOk()->assertJsonCount(2, 'data');
    }

    /** @test */
    public function admin_can_create_location()
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');
        $project = Project::factory()->create();

        $response = $this->actingAs($user, 'sanctum')
            ->postJson("/api/v1/projects/{$project->id}/locations", [
                'remote_name' => 'Site C',
                'address'     => 'Jl. Baru No. 3',
                'provinsi'    => 'Sulawesi Selatan',
                'kota_kab'    => 'Makassar',
            ]);

        $response->assertCreated();
        $this->assertDatabaseHas('master_locations', ['remote_name' => 'Site C']);
    }

    /** @test */
    public function can_view_single_location()
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');
        $project = Project::factory()->create();
        $location = $this->createLocation($project, $user);

        $response = $this->actingAs($user, 'sanctum')
            ->getJson("/api/v1/locations/{$location->uuid}");

        $response->assertOk();
    }

    /** @test */
    public function admin_can_update_location()
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');
        $project = Project::factory()->create();
        $location = $this->createLocation($project, $user);

        $response = $this->actingAs($user, 'sanctum')
            ->putJson("/api/v1/locations/{$location->uuid}", [
                'remote_name' => 'Site Updated',
                'address'     => $location->address,
            ]);

        $response->assertOk();
        $this->assertDatabaseHas('master_locations', ['remote_name' => 'Site Updated']);
    }

    /** @test */
    public function admin_can_delete_location()
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');
        $project = Project::factory()->create();
        $location = $this->createLocation($project, $user);

        $response = $this->actingAs($user, 'sanctum')
            ->deleteJson("/api/v1/locations/{$location->uuid}");

        $response->assertOk();
        $this->assertSoftDeleted('master_locations', ['id' => $location->id]);
    }
}

<?php

namespace Tests\Feature\Api;

use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ProjectTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_field_engineer_only_sees_assigned_projects(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $assigned = Project::factory()->create();
        $unassigned = Project::factory()->create();

        ProjectAssignment::create([
            'uuid' => \Illuminate\Support\Str::uuid()->toString(),
            'project_id' => $assigned->id,
            'user_id' => $user->id,
            'role_on_project' => 'FIELD_ENGINEER',
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->getJson('/api/v1/projects');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'projects')
            ->assertJsonPath('projects.0.uuid', $assigned->uuid);
    }

    public function test_unauthenticated_user_cannot_list_projects(): void
    {
        $response = $this->getJson('/api/v1/projects');

        $response->assertStatus(401);
    }

    public function test_owner_can_create_project(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/projects', [
                'name' => 'Test Project',
                'description' => 'A test project',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('project.name', 'Test Project')
            ->assertJsonPath('project.user_id', $user->id);

        $this->assertDatabaseHas('projects', [
            'name' => 'Test Project',
            'user_id' => $user->id,
        ]);
    }

    public function test_field_engineer_cannot_create_project(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->postJson('/api/v1/projects', [
                'name' => 'Should Fail',
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['role']);
    }

    public function test_owner_can_update_project(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $project = Project::factory()->create([
            'user_id' => $user->id,
            'name' => 'Old Name',
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer ' . $token)
            ->patchJson('/api/v1/projects/' . $project->uuid, [
                'name' => 'New Name',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('project.name', 'New Name');

        $this->assertDatabaseHas('projects', [
            'id' => $project->id,
            'name' => 'New Name',
        ]);
    }
}
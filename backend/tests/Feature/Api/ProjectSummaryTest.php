<?php

namespace Tests\Feature\Api;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ProjectSummaryTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_project_summary_matches_service_output(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $project = Project::factory()->create(['user_id' => $user->id]);

        Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
            'type' => 'FUND_IN', 'reported_amount' => 5000000, 'real_amount' => 5000000,
        ]);
        Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
            'type' => 'OFFICE_EXPENSE', 'reported_amount' => 2000000, 'real_amount' => 1800000,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/projects/'.$project->uuid.'/summary');

        $response->assertStatus(200)
            ->assertJsonPath('summary.total_fund_in', 5000000)
            ->assertJsonPath('summary.total_office_reported', 2000000)
            ->assertJsonPath('summary.total_office_real', 1800000)
            ->assertJsonPath('summary.total_cash_out', 1800000)
            ->assertJsonPath('summary.net_position', 3200000)
            ->assertJsonPath('summary.transaction_count', 2)
            ->assertJsonPath('summary.project_uuid', $project->uuid);
    }

    public function test_project_export_includes_transactions_and_summary(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $project = Project::factory()->create(['user_id' => $user->id]);

        Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
            'type' => 'FUND_IN', 'reported_amount' => 1000000, 'real_amount' => 1000000,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/projects/'.$project->uuid.'/export');

        $response->assertStatus(200)
            ->assertJsonPath('project.uuid', $project->uuid)
            ->assertJsonPath('summary.total_fund_in', 1000000)
            ->assertJsonCount(1, 'transactions');
    }

    public function test_empty_project_summary_returns_zeros(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $project = Project::factory()->create(['user_id' => $user->id]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/projects/'.$project->uuid.'/summary');

        $response->assertStatus(200)
            ->assertJsonPath('summary.total_fund_in', 0)
            ->assertJsonPath('summary.net_position', 0)
            ->assertJsonPath('summary.transaction_count', 0);
    }

    public function test_soft_deleted_transactions_excluded_from_summary(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $project = Project::factory()->create(['user_id' => $user->id]);

        Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
            'type' => 'FUND_IN', 'reported_amount' => 5000000, 'real_amount' => 5000000,
        ]);
        Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
            'type' => 'OFFICE_EXPENSE', 'reported_amount' => 1000000, 'real_amount' => 1000000,
            'deleted_at' => now(),
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/projects/'.$project->uuid.'/summary');

        $response->assertStatus(200)
            ->assertJsonPath('summary.total_fund_in', 5000000)
            ->assertJsonPath('summary.total_office_reported', 0)   // deleted
            ->assertJsonPath('summary.transaction_count', 1);
    }
}

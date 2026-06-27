<?php

namespace Tests\Feature\Api;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class TransactionApprovalTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_field_engineer_can_submit_draft_transaction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_DRAFT,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/submit');

        $response->assertStatus(200)
            ->assertJsonPath('transaction.approval_status', Transaction::APPROVAL_PENDING);

        $this->assertDatabaseHas('transactions', [
            'id' => $tx->id,
            'approval_status' => Transaction::APPROVAL_PENDING,
        ]);

        $this->assertDatabaseHas('audit_events', [
            'entity_uuid' => $tx->uuid,
            'action' => 'submit',
        ]);
    }

    public function test_submit_non_draft_transaction_is_rejected(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_PENDING,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/submit');

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['status']);
    }

    public function test_finance_manager_can_approve_pending_transaction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_PENDING,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/approve');

        $response->assertStatus(200)
            ->assertJsonPath('transaction.approval_status', Transaction::APPROVAL_APPROVED);

        $this->assertDatabaseHas('transactions', [
            'id' => $tx->id,
            'approval_status' => Transaction::APPROVAL_APPROVED,
        ]);

        $this->assertDatabaseHas('audit_events', [
            'entity_uuid' => $tx->uuid,
            'action' => 'approve',
        ]);
    }

    public function test_approve_draft_transaction_is_rejected(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_DRAFT,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/approve');

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['status']);
    }

    public function test_finance_manager_can_reject_pending_transaction_with_reason(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_PENDING,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/reject', [
                'reason' => 'Amount does not match receipts.',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('transaction.approval_status', Transaction::APPROVAL_REJECTED);

        $this->assertDatabaseHas('audit_events', [
            'entity_uuid' => $tx->uuid,
            'action' => 'reject',
        ]);
    }

    public function test_reject_without_reason_is_rejected(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_PENDING,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/reject', []);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['reason']);
    }

    public function test_field_engineer_cannot_approve_transaction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_PENDING,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/approve');

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['role']);
    }

    public function test_viewer_cannot_approve(): void
    {
        $user = User::factory()->create();
        $user->assignRole('VIEWER');

        $project = Project::factory()->create();
        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_PENDING,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/approve');

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['role']);
    }
}
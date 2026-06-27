<?php

namespace Tests\Feature\Api;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class DisputeTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    private function createApprovedTx(): array
    {
        $admin = User::factory()->create();
        $admin->assignRole('ADMIN');
        $project = Project::factory()->create(['user_id' => $admin->id]);
        $tx = Transaction::factory()->create([
            'user_id' => $admin->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_APPROVED,
            'reported_amount' => 1000000, 'real_amount' => 950000,
        ]);
        $token = $admin->createToken('test')->plainTextToken;
        return compact('admin', 'project', 'tx', 'token');
    }

    public function test_admin_can_dispute_approved_transaction(): void
    {
        extract($this->createApprovedTx());

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/dispute', [
                'disputed_amount' => 800000,
                'dispute_reason' => 'Harga pasar hanya 800rb, bukan 950rb.',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('transaction.approval_status', 'DISPUTED');

        $this->assertDatabaseHas('transactions', [
            'id' => $tx->id, 'approval_status' => 'DISPUTED', 'disputed_amount' => 800000,
        ]);
        $this->assertDatabaseHas('audit_events', [
            'entity_uuid' => $tx->uuid, 'action' => 'dispute',
        ]);
    }

    public function test_dispute_non_approved_fails(): void
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');
        $tx = Transaction::factory()->create(['user_id' => $user->id, 'approval_status' => 'DRAFT']);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/dispute', [
                'disputed_amount' => 100000, 'dispute_reason' => 'Test',
            ]);

        $response->assertStatus(422)->assertJsonValidationErrors(['status']);
    }

    public function test_field_engineer_cannot_dispute(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $tx = Transaction::factory()->create(['user_id' => $user->id, 'approval_status' => 'APPROVED']);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/dispute', [
                'disputed_amount' => 100000, 'dispute_reason' => 'Test',
            ]);

        $response->assertStatus(422)->assertJsonValidationErrors(['role']);
    }

    public function test_resolve_dispute_accept_creates_correction(): void
    {
        extract($this->createApprovedTx());

        // First, dispute
        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/dispute', [
                'disputed_amount' => 800000, 'dispute_reason' => 'Overpriced.',
            ]);

        // Resolve — accept
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/resolve-dispute', [
                'action' => 'accept_dispute', 'resolution_note' => 'Correction approved.',
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('transaction.finance_status', 'CORRECTED')
            ->assertJsonPath('correction.real_amount', 800000);
    }

    public function test_resolve_dispute_reject_restores_approved(): void
    {
        extract($this->createApprovedTx());

        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/dispute', [
                'disputed_amount' => 800000, 'dispute_reason' => 'Test.',
            ]);

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/resolve-dispute', [
                'action' => 'reject_dispute',
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('transaction.approval_status', 'APPROVED');
    }
}

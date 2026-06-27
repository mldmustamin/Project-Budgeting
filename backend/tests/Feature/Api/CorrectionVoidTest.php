<?php

namespace Tests\Feature\Api;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CorrectionVoidTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    private function createApprovedTx(User $user): Transaction
    {
        $project = Project::factory()->create(['user_id' => $user->id]);
        return Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => Transaction::APPROVAL_APPROVED,
            'finance_status' => Transaction::FINANCE_ACTIVE,
        ]);
    }

    public function test_finance_manager_can_void_approved_transaction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');
        $tx = $this->createApprovedTx($user);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/void', ['reason' => 'Duplicate entry.']);

        $response->assertStatus(200)->assertJsonPath('transaction.finance_status', 'VOIDED');
        $this->assertDatabaseHas('audit_events', ['entity_uuid' => $tx->uuid, 'action' => 'void']);
    }


    public function test_void_non_approved_transaction_fails(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');
        $tx = Transaction::factory()->create(['user_id' => $user->id, 'approval_status' => 'DRAFT']);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/void', ['reason' => 'Test']);
        $response->assertStatus(422)->assertJsonValidationErrors(['status']);
    }

    public function test_field_engineer_cannot_void(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $tx = $this->createApprovedTx($user);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/void', ['reason' => 'Test']);
        $response->assertStatus(422)->assertJsonValidationErrors(['role']);
    }

    public function test_finance_manager_can_create_correction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');
        $tx = $this->createApprovedTx($user);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/correction', [
                'reported_amount' => 200000, 'real_amount' => 180000, 'reason' => 'Amount was incorrect.',
            ]);
        $response->assertStatus(201)
            ->assertJsonPath('original.finance_status', 'CORRECTED')
            ->assertJsonPath('correction.reported_amount', 200000);
        $this->assertDatabaseHas('audit_events', ['entity_uuid' => $tx->uuid, 'action' => 'corrected']);
    }

    public function test_correction_non_approved_fails(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');
        $tx = Transaction::factory()->create(['user_id' => $user->id, 'approval_status' => 'DRAFT']);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/correction', [
                'reported_amount' => 100000, 'real_amount' => 100000, 'reason' => 'Test',
            ]);
        $response->assertStatus(422)->assertJsonValidationErrors(['status']);
    }

    public function test_approved_transaction_update_rejected_by_sync_push(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $user->uuid = \Illuminate\Support\Str::uuid()->toString();
        $user->save();
        $tx = $this->createApprovedTx($user);
        \App\Models\ProjectAssignment::create(['project_id' => $tx->project_id, 'user_id' => $user->id, 'role_on_project' => 'FIELD_ENGINEER']);
        $device = \App\Models\Device::factory()->create(['user_id' => $user->id, 'uuid' => \Illuminate\Support\Str::uuid()->toString(), 'is_revoked' => false]);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'usr:dev:op:immutability',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $tx->uuid,
                    'operation' => 'UPDATE',
                    'payload' => ['type' => $tx->type, 'project_uuid' => $tx->project_uuid, 'date' => $tx->date->format('Y-m-d'), 'reported_amount' => 999999, 'real_amount' => 999999, 'description' => 'Attempted update'],
                ]],
            ]);
        $response->assertStatus(200)->assertJsonPath('results.0.status', 'REJECTED');
    }
}

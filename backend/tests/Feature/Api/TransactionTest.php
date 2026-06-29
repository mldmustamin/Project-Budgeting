<?php

namespace Tests\Feature\Api;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class TransactionTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_authenticated_user_can_list_transactions(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        Transaction::factory()->count(3)->create(['user_id' => $user->id]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/transactions');

        $response->assertStatus(200)
            ->assertJsonCount(3, 'transactions');
    }

    public function test_unauthenticated_user_cannot_list_transactions(): void
    {
        $response = $this->getJson('/api/v1/transactions');

        $response->assertStatus(401);
    }

    public function test_authenticated_user_can_create_transaction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'FUND_IN',
                'project_uuid' => $project->uuid,
                'date' => '2026-06-27',
                'reported_amount' => 500000,
                'real_amount' => 500000,
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('transaction.type', 'FUND_IN')
            ->assertJsonPath('transaction.reported_amount', 500000)
            ->assertJsonPath('transaction.project_uuid', $project->uuid);

        $this->assertDatabaseHas('transactions', [
            'type' => 'FUND_IN',
            'project_uuid' => $project->uuid,
            'user_id' => $user->id,
        ]);
    }

    public function test_validation_rejects_missing_required_fields(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'OFFICE_EXPENSE',
                // missing project_uuid, date, reported_amount, real_amount, description
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['project_uuid', 'date', 'reported_amount', 'real_amount', 'description']);
    }

    public function test_authenticated_user_can_view_transaction_by_uuid(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $transaction = Transaction::factory()->create(['user_id' => $user->id]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/transactions/'.$transaction->uuid);

        $response->assertStatus(200)
            ->assertJsonPath('transaction.uuid', $transaction->uuid)
            ->assertJsonPath('transaction.type', $transaction->type);
    }

    public function test_fund_in_can_omit_description(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'FUND_IN',
                'project_uuid' => $project->uuid,
                'date' => '2026-06-27',
                'reported_amount' => 500000,
                'real_amount' => 500000,
                // description omitted — valid for FUND_IN
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('transaction.type', 'FUND_IN');
    }

    public function test_office_expense_requires_description(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'OFFICE_EXPENSE',
                'project_uuid' => $project->uuid,
                'date' => '2026-06-27',
                'reported_amount' => 500000,
                'real_amount' => 450000,
                // description omitted — required for OFFICE_EXPENSE
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['description']);
    }

    public function test_personal_expense_requires_description(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'PERSONAL_EXPENSE',
                'project_uuid' => $project->uuid,
                'date' => '2026-06-27',
                'reported_amount' => 200000,
                'real_amount' => 200000,
                // description omitted — required for PERSONAL_EXPENSE
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['description']);
    }

    public function test_reported_amount_rejects_decimal(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'FUND_IN',
                'project_uuid' => $project->uuid,
                'date' => '2026-06-27',
                'reported_amount' => 500.50,
                'real_amount' => 500000,
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['reported_amount']);
    }

    public function test_real_amount_rejects_string_float(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'FUND_IN',
                'project_uuid' => $project->uuid,
                'date' => '2026-06-27',
                'reported_amount' => 500000,
                'real_amount' => '1000.99',
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['real_amount']);
    }

    public function test_filter_by_project_uuid(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $projectA = Project::factory()->create();
        $projectB = Project::factory()->create();

        Transaction::factory()->create(['user_id' => $user->id, 'project_id' => $projectA->id, 'project_uuid' => $projectA->uuid]);
        Transaction::factory()->create(['user_id' => $user->id, 'project_id' => $projectB->id, 'project_uuid' => $projectB->uuid]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/transactions?project_uuid='.$projectA->uuid);

        $response->assertStatus(200)
            ->assertJsonCount(1, 'transactions')
            ->assertJsonPath('transactions.0.project_uuid', $projectA->uuid);
    }

    public function test_filter_by_type(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        Transaction::factory()->create(['user_id' => $user->id, 'type' => 'FUND_IN']);
        Transaction::factory()->create(['user_id' => $user->id, 'type' => 'OFFICE_EXPENSE']);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/transactions?type=FUND_IN');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'transactions')
            ->assertJsonPath('transactions.0.type', 'FUND_IN');
    }

    public function test_filter_by_approval_status(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        Transaction::factory()->create(['user_id' => $user->id, 'approval_status' => 'DRAFT']);
        Transaction::factory()->create(['user_id' => $user->id, 'approval_status' => 'PENDING']);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/transactions?approval_status=PENDING');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'transactions')
            ->assertJsonPath('transactions.0.approval_status', 'PENDING');
    }

    public function test_filter_by_finance_status(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        Transaction::factory()->create(['user_id' => $user->id, 'finance_status' => 'ACTIVE']);
        Transaction::factory()->create(['user_id' => $user->id, 'finance_status' => 'CORRECTED']);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/transactions?finance_status=CORRECTED');

        $response->assertStatus(200)
            ->assertJsonCount(1, 'transactions')
            ->assertJsonPath('transactions.0.finance_status', 'CORRECTED');
    }

    public function test_auditor_cannot_create_transaction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('AUDITOR');

        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions', [
                'type' => 'FUND_IN',
                'project_uuid' => $project->uuid,
                'date' => '2026-06-27',
                'reported_amount' => 500000,
                'real_amount' => 500000,
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['role']);
    }
}
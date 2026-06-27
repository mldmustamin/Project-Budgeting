<?php

namespace Tests\Feature\Api;

use App\Models\AccountingPeriod;
use App\Models\Device;
use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Tests\TestCase;

class PeriodTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_authenticated_user_can_list_periods(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        AccountingPeriod::factory()->create(['period_start' => '2026-01-01', 'period_end' => '2026-01-31', 'status' => 'OPEN']);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->getJson('/api/v1/periods');
        $response->assertStatus(200)->assertJsonCount(1, 'periods');
    }

    public function test_finance_manager_can_close_period(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');
        $period = AccountingPeriod::factory()->create(['status' => 'OPEN', 'period_start' => '2026-03-01', 'period_end' => '2026-03-31']);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/periods/'.$period->uuid.'/close', ['reason' => 'Bulan Maret ditutup.']);
        $response->assertStatus(200)->assertJsonPath('period.status', 'CLOSED');
        $this->assertDatabaseHas('accounting_periods', ['id' => $period->id, 'status' => 'CLOSED', 'closed_by' => $user->id]);
        $this->assertDatabaseHas('audit_events', ['entity_uuid' => $period->uuid, 'action' => 'close_period']);
    }

    public function test_field_engineer_cannot_close_period(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $period = AccountingPeriod::factory()->create(['status' => 'OPEN']);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/periods/'.$period->uuid.'/close');
        $response->assertStatus(422)->assertJsonValidationErrors(['role']);
    }

    public function test_finance_manager_can_reopen_period(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');
        $period = AccountingPeriod::factory()->create(['status' => 'CLOSED', 'closed_by' => $user->id, 'closed_at' => now()]);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/periods/'.$period->uuid.'/reopen');
        $response->assertStatus(200)->assertJsonPath('period.status', 'OPEN');
        $this->assertDatabaseHas('audit_events', ['entity_uuid' => $period->uuid, 'action' => 'reopen_period']);
    }


    public function test_transaction_create_rejected_in_closed_period(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $project = Project::factory()->create(['user_id' => $user->id]);
        AccountingPeriod::factory()->create(['status' => 'CLOSED', 'period_start' => '2026-02-01', 'period_end' => '2026-02-28', 'closed_by' => $user->id, 'closed_at' => now()]);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/transactions', [
            'project_uuid' => $project->uuid, 'type' => 'FUND_IN', 'date' => '2026-02-15', 'reported_amount' => 100000, 'real_amount' => 100000,
        ]);
        $response->assertStatus(422)->assertJsonValidationErrors(['date']);
    }

    public function test_transaction_create_allowed_in_open_period(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $project = Project::factory()->create(['user_id' => $user->id]);
        AccountingPeriod::factory()->create(['status' => 'OPEN', 'period_start' => '2026-02-01', 'period_end' => '2026-02-28']);
        $token = $user->createToken('test')->plainTextToken;
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/transactions', [
            'project_uuid' => $project->uuid, 'type' => 'FUND_IN', 'date' => '2026-02-15', 'reported_amount' => 100000, 'real_amount' => 100000,
        ]);
        $response->assertStatus(201);
    }

    public function test_sync_push_create_rejected_in_closed_period(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $user->uuid = Str::uuid()->toString();
        $user->save();
        $project = Project::factory()->create(['user_id' => $user->id]);
        ProjectAssignment::create(['project_id' => $project->id, 'user_id' => $user->id, 'role_on_project' => 'FIELD_ENGINEER']);
        $device = Device::factory()->create(['user_id' => $user->id, 'uuid' => Str::uuid()->toString(), 'is_revoked' => false]);
        AccountingPeriod::factory()->create(['status' => 'CLOSED', 'period_start' => '2026-02-01', 'period_end' => '2026-02-28', 'closed_by' => $user->id, 'closed_at' => now()]);
        $token = $user->createToken('test')->plainTextToken;
        $entityUuid = Str::uuid()->toString();
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/sync/push', [
            'device_uuid' => $device->uuid,
            'operations' => [[
                'idempotency_key' => 'usr:dev:op:period-closed',
                'entity_type' => 'transaction',
                'entity_uuid' => $entityUuid,
                'operation' => 'CREATE',
                'payload' => ['type' => 'FUND_IN', 'project_uuid' => $project->uuid, 'date' => '2026-02-15', 'reported_amount' => 500000, 'real_amount' => 500000, 'description' => 'Rejected'],
            ]],
        ]);
        $response->assertStatus(200)->assertJsonPath('results.0.status', 'REJECTED');
    }

    public function test_sync_push_create_allowed_when_no_closed_period(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');
        $user->uuid = Str::uuid()->toString();
        $user->save();
        $project = Project::factory()->create(['user_id' => $user->id]);
        ProjectAssignment::create(['project_id' => $project->id, 'user_id' => $user->id, 'role_on_project' => 'FIELD_ENGINEER']);
        $device = Device::factory()->create(['user_id' => $user->id, 'uuid' => Str::uuid()->toString(), 'is_revoked' => false]);
        $token = $user->createToken('test')->plainTextToken;
        $entityUuid = Str::uuid()->toString();
        $response = $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/sync/push', [
            'device_uuid' => $device->uuid,
            'operations' => [[
                'idempotency_key' => 'usr:dev:op:period-open',
                'entity_type' => 'transaction',
                'entity_uuid' => $entityUuid,
                'operation' => 'CREATE',
                'payload' => ['type' => 'FUND_IN', 'project_uuid' => $project->uuid, 'date' => '2026-02-15', 'reported_amount' => 500000, 'real_amount' => 500000, 'description' => 'Accepted'],
            ]],
        ]);
        $response->assertStatus(200)->assertJsonPath('results.0.status', 'ACCEPTED');
    }
}

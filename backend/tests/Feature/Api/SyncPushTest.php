<?php

namespace Tests\Feature\Api;

use App\Models\Device;
use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\SyncOutbox;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SyncPushTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    // ========== CREATE tests ==========

    public function test_authenticated_user_can_push_transaction_create(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        ProjectAssignment::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'project_id' => $project->id,
            'user_id' => $user->id,
            'role_on_project' => 'FIELD_ENGINEER',
        ]);

        $entityUuid = \Illuminate\Support\Str::uuid()->toString();
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'user:'.$device->uuid.':op-001',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $entityUuid,
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'OFFICE_EXPENSE',
                        'date' => '2026-06-27',
                        'description' => 'Transport',
                        'reported_amount' => 500000,
                        'real_amount' => 450000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonCount(1, 'results')
            ->assertJsonPath('results.0.status', 'ACCEPTED')
            ->assertJsonPath('results.0.entity_uuid', $entityUuid);

        $this->assertDatabaseHas('transactions', [
            'uuid' => $entityUuid,
            'project_uuid' => $project->uuid,
            'sync_status' => 'SYNCED',
        ]);

        $this->assertDatabaseHas('audit_events', [
            'entity_type' => 'transaction',
            'entity_uuid' => $entityUuid,
            'action' => 'sync_create',
        ]);

        $this->assertDatabaseHas('sync_outboxes', [
            'idempotency_key' => 'user:'.$device->uuid.':op-001',
            'entity_uuid' => $entityUuid,
            'status' => 'SYNCED',
        ]);
    }

    public function test_unauthenticated_user_cannot_push(): void
    {
        $response = $this->postJson('/api/v1/sync/push', [
            'device_uuid' => \Illuminate\Support\Str::uuid()->toString(),
            'operations' => [],
        ]);

        $response->assertStatus(401);
    }

    public function test_duplicate_idempotency_key_returns_duplicate(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        ProjectAssignment::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'project_id' => $project->id,
            'user_id' => $user->id,
            'role_on_project' => 'FIELD_ENGINEER',
        ]);

        $entityUuid = \Illuminate\Support\Str::uuid()->toString();
        $idempotencyKey = 'user:'.$device->uuid.':op-002';
        $token = $user->createToken('test')->plainTextToken;

        // First push
        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => $idempotencyKey,
                    'entity_type' => 'transaction',
                    'entity_uuid' => $entityUuid,
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'OFFICE_EXPENSE',
                        'date' => '2026-06-27',
                        'description' => 'Transport',
                        'reported_amount' => 500000,
                        'real_amount' => 450000,
                    ],
                ]],
            ]);

        // Second push — same idempotency key
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => $idempotencyKey,
                    'entity_type' => 'transaction',
                    'entity_uuid' => $entityUuid,
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'OFFICE_EXPENSE',
                        'date' => '2026-06-27',
                        'description' => 'Transport',
                        'reported_amount' => 500000,
                        'real_amount' => 450000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'DUPLICATE');
    }

    public function test_device_owned_by_another_user_is_rejected(): void
    {
        $ownerUser = User::factory()->create();
        $ownerDevice = Device::factory()->create(['user_id' => $ownerUser->id]);

        $otherUser = User::factory()->create();
        $otherUser->assignRole('FIELD_ENGINEER');
        $token = $otherUser->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $ownerDevice->uuid,
                'operations' => [[
                    'idempotency_key' => 'key-001',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                        'type' => 'FUND_IN',
                        'date' => '2026-06-27',
                        'reported_amount' => 500000,
                        'real_amount' => 500000,
                    ],
                ]],
            ]);

        $response->assertStatus(401)
            ->assertJsonPath('error', 'DEVICE_NOT_REGISTERED');
    }

    public function test_revoked_device_rejected(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create([
            'user_id' => $user->id,
            'is_revoked' => true,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'key-002',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                        'type' => 'FUND_IN',
                        'date' => '2026-06-27',
                        'reported_amount' => 500000,
                        'real_amount' => 500000,
                    ],
                ]],
            ]);

        $response->assertStatus(403)
            ->assertJsonPath('error', 'DEVICE_REVOKED');
    }

    public function test_batch_over_50_returns_validation_error(): void
    {
        $user = User::factory()->create();
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $operations = [];
        for ($i = 1; $i <= 51; $i++) {
            $operations[] = [
                'idempotency_key' => 'key-' . $i,
                'entity_type' => 'transaction',
                'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                'operation' => 'CREATE',
                'payload' => [
                    'project_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'type' => 'FUND_IN',
                    'date' => '2026-06-27',
                    'reported_amount' => 500000,
                    'real_amount' => 500000,
                ],
            ];
        }

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => $operations,
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['operations']);
    }

    public function test_unsupported_entity_type_returns_rejected(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'key-unsup',
                    'entity_type' => 'attachment',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'CREATE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED')
            ->assertJsonPath('results.0.reason', 'Unsupported entity_type: attachment');
    }

    public function test_unsupported_operation_returns_rejected(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'key-unknown',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'PURGE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED')
            ->assertJsonPath('results.0.reason', 'Unsupported operation: PURGE');
    }

    // ========== CREATE scoping tests ==========

    public function test_owner_can_push_to_unassigned_project(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $entityUuid = \Illuminate\Support\Str::uuid()->toString();
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'owner-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $entityUuid,
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'OFFICE_EXPENSE',
                        'date' => '2026-06-27',
                        'description' => 'Owner expense',
                        'reported_amount' => 1000000,
                        'real_amount' => 800000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'ACCEPTED')
            ->assertJsonPath('results.0.entity_uuid', $entityUuid);
    }

    public function test_unassigned_user_rejected(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();
        // No ProjectAssignment created

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'key-003',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'OFFICE_EXPENSE',
                        'date' => '2026-06-27',
                        'description' => 'Test',
                        'reported_amount' => 500000,
                        'real_amount' => 450000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED');
    }

    public function test_create_without_payload_returns_rejected_result(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'missing-create-payload',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'CREATE',
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED')
            ->assertJsonPath('results.0.reason', 'Validation failed: type, project_uuid, date, reported_amount, real_amount');
    }

    // ========== UPDATE tests ==========

    public function test_update_accepted_updates_transaction_fields(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        ProjectAssignment::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'project_id' => $project->id,
            'user_id' => $user->id,
            'role_on_project' => 'FIELD_ENGINEER',
        ]);

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'type' => 'OFFICE_EXPENSE',
            'date' => '2026-06-27',
            'description' => 'Old desc',
            'reported_amount' => 500000,
            'real_amount' => 400000,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'upd-key-001',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'UPDATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'FUND_IN',
                        'date' => '2026-07-01',
                        'reported_amount' => 1000000,
                        'real_amount' => 1000000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'ACCEPTED');

        $this->assertDatabaseHas('transactions', [
            'id' => $existing->id,
            'type' => 'FUND_IN',
            'reported_amount' => 1000000,
        ]);
    }

    public function test_update_creates_audit_event_sync_update(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'type' => 'FUND_IN',
            'date' => '2026-06-27',
            'reported_amount' => 500000,
            'real_amount' => 500000,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'upd-audit-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'UPDATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'FUND_IN',
                        'date' => '2026-07-01',
                        'reported_amount' => 700000,
                        'real_amount' => 700000,
                    ],
                ]],
            ]);

        $this->assertDatabaseHas('audit_events', [
            'entity_type' => 'transaction',
            'entity_uuid' => $existing->uuid,
            'action' => 'sync_update',
        ]);
    }

    public function test_update_rejected_when_transaction_uuid_not_found(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');
        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'upd-miss-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'UPDATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'FUND_IN',
                        'date' => '2026-06-27',
                        'reported_amount' => 500000,
                        'real_amount' => 500000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED')
            ->assertJsonPath('results.0.reason', 'Transaction not found.');
    }

    public function test_update_without_payload_returns_rejected_result(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();
        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'missing-update-payload',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'UPDATE',
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED')
            ->assertJsonPath('results.0.reason', 'Validation failed: type, project_uuid, date, reported_amount, real_amount');
    }

    public function test_update_rejected_when_field_engineer_not_assigned(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'type' => 'FUND_IN',
            'date' => '2026-06-27',
            'reported_amount' => 500000,
            'real_amount' => 500000,
        ]);
        // No ProjectAssignment

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'upd-noassign-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'UPDATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'FUND_IN',
                        'date' => '2026-07-01',
                        'reported_amount' => 700000,
                        'real_amount' => 700000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED');
    }

    public function test_owner_can_update_without_assignment(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'reported_amount' => 500000,
            'real_amount' => 500000,
        ]);
        // No ProjectAssignment

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'owner-upd-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'UPDATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'FUND_IN',
                        'date' => '2026-07-01',
                        'reported_amount' => 999000,
                        'real_amount' => 999000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'ACCEPTED');
    }

    public function test_duplicate_update_returns_duplicate_and_does_not_apply_second_payload(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'reported_amount' => 500000,
            'description' => 'First update',
        ]);

        $token = $user->createToken('test')->plainTextToken;
        $idempotencyKey = 'dup-upd-key-x';

        // First UPDATE
        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => $idempotencyKey,
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'UPDATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'FUND_IN',
                        'date' => '2026-07-01',
                        'description' => 'First update',
                        'reported_amount' => 700000,
                        'real_amount' => 700000,
                    ],
                ]],
            ]);

        // Second UPDATE with different payload — should be DUPLICATE, not re-applied
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => $idempotencyKey,
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'UPDATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'OFFICE_EXPENSE',
                        'date' => '2026-08-01',
                        'description' => 'Should not apply',
                        'reported_amount' => 999999,
                        'real_amount' => 888888,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'DUPLICATE');

        $this->assertDatabaseHas('transactions', [
            'id' => $existing->id,
            'reported_amount' => 700000, // still first update value
        ]);
    }

    // ========== SOFT_DELETE tests ==========

    public function test_soft_delete_accepted_soft_deletes_transaction(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        ProjectAssignment::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'project_id' => $project->id,
            'user_id' => $user->id,
            'role_on_project' => 'FIELD_ENGINEER',
        ]);

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'sd-key-001',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'SOFT_DELETE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'ACCEPTED');

        $this->assertSoftDeleted('transactions', [
            'uuid' => $existing->uuid,
        ]);
    }

    public function test_soft_delete_creates_audit_event_sync_soft_delete(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
        ]);

        $token = $user->createToken('test')->plainTextToken;

        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'sd-audit-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'SOFT_DELETE',
                    'payload' => [],
                ]],
            ]);

        $this->assertDatabaseHas('audit_events', [
            'entity_type' => 'transaction',
            'entity_uuid' => $existing->uuid,
            'action' => 'sync_soft_delete',
        ]);
    }

    public function test_soft_delete_rejected_when_transaction_uuid_not_found(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');
        $device = Device::factory()->create(['user_id' => $user->id]);
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'sd-miss-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'SOFT_DELETE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED')
            ->assertJsonPath('results.0.reason', 'Transaction not found.');
    }

    public function test_soft_delete_rejected_when_field_engineer_not_assigned(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
        ]);
        // No ProjectAssignment

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'sd-noassign-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'SOFT_DELETE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED');
    }

    public function test_owner_can_soft_delete_without_assignment(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
        ]);
        // No ProjectAssignment

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'owner-sd-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'SOFT_DELETE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'ACCEPTED');

        $this->assertSoftDeleted('transactions', [
            'uuid' => $existing->uuid,
        ]);
    }

    public function test_duplicate_soft_delete_returns_duplicate(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $existing = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
        ]);

        $token = $user->createToken('test')->plainTextToken;
        $idempotencyKey = 'dup-sd-key-x';

        // First SOFT_DELETE
        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => $idempotencyKey,
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'SOFT_DELETE',
                    'payload' => [],
                ]],
            ]);

        // Second SOFT_DELETE — same key
        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => $idempotencyKey,
                    'entity_type' => 'transaction',
                    'entity_uuid' => $existing->uuid,
                    'operation' => 'SOFT_DELETE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'DUPLICATE');
    }

    // ========== Response shape ==========

    public function test_response_shape_includes_all_required_fields(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $entityUuid = \Illuminate\Support\Str::uuid()->toString();
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [[
                    'idempotency_key' => 'shape-key',
                    'entity_type' => 'transaction',
                    'entity_uuid' => $entityUuid,
                    'operation' => 'CREATE',
                    'payload' => [
                        'project_uuid' => $project->uuid,
                        'type' => 'FUND_IN',
                        'date' => '2026-06-27',
                        'reported_amount' => 500000,
                        'real_amount' => 500000,
                    ],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonStructure([
                'results' => [
                    ['idempotency_key', 'entity_uuid', 'status', 'server_id', 'reason'],
                ],
            ]);
    }
}

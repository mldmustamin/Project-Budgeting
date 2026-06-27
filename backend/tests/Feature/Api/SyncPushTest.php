<?php

namespace Tests\Feature\Api;

use App\Models\Device;
use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\SyncOutbox;
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
                    'idempotency_key' => 'key-update',
                    'entity_type' => 'transaction',
                    'entity_uuid' => \Illuminate\Support\Str::uuid()->toString(),
                    'operation' => 'UPDATE',
                    'payload' => [],
                ]],
            ]);

        $response->assertStatus(200)
            ->assertJsonPath('results.0.status', 'REJECTED')
            ->assertJsonPath('results.0.reason', 'Unsupported operation: UPDATE');
    }

    public function test_owner_can_push_to_unassigned_project(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();
        // No ProjectAssignment — OWNER should still succeed

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

    public function test_mixed_batch_returns_accepted_and_rejected(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $device = Device::factory()->create(['user_id' => $user->id]);
        $project = Project::factory()->create();

        $entityUuid1 = \Illuminate\Support\Str::uuid()->toString();
        $entityUuid2 = \Illuminate\Support\Str::uuid()->toString();
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/sync/push', [
                'device_uuid' => $device->uuid,
                'operations' => [
                    [
                        'idempotency_key' => 'accept-key',
                        'entity_type' => 'transaction',
                        'entity_uuid' => $entityUuid1,
                        'operation' => 'CREATE',
                        'payload' => [
                            'project_uuid' => $project->uuid,
                            'type' => 'FUND_IN',
                            'date' => '2026-06-27',
                            'reported_amount' => 500000,
                            'real_amount' => 500000,
                        ],
                    ],
                    [
                        'idempotency_key' => 'reject-key',
                        'entity_type' => 'attachment',  // unsupported
                        'entity_uuid' => $entityUuid2,
                        'operation' => 'CREATE',
                        'payload' => [],
                    ],
                ],
            ]);

        $response->assertStatus(200)
            ->assertJsonCount(2, 'results')
            ->assertJsonPath('results.0.status', 'ACCEPTED')
            ->assertJsonPath('results.1.status', 'REJECTED');
    }

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
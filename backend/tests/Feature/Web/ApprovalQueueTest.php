<?php

namespace Tests\Feature\Web;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ApprovalQueueTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_finance_manager_can_view_approval_queue(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FINANCE_MANAGER');

        $response = $this->actingAs($user)->get('/approval');

        $response->assertStatus(200);
        $response->assertSee('Pending');
    }

    public function test_field_engineer_cannot_access_approval_queue(): void
    {
        $user = User::factory()->create();
        $user->assignRole('FIELD_ENGINEER');

        $response = $this->actingAs($user)->get('/approval');

        $response->assertStatus(403);
    }

    public function test_finance_manager_can_approve_from_web(): void
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

        $response = $this->actingAs($user)
            ->post('/transactions/'.$tx->uuid.'/approve');

        $response->assertRedirect();
        $response->assertSessionHas('success');

        $this->assertDatabaseHas('transactions', [
            'id' => $tx->id,
            'approval_status' => Transaction::APPROVAL_APPROVED,
        ]);

        $this->assertDatabaseHas('audit_events', [
            'entity_uuid' => $tx->uuid,
            'action' => 'approve',
        ]);
    }

    public function test_finance_manager_can_reject_with_reason_from_web(): void
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

        $response = $this->actingAs($user)
            ->post('/transactions/'.$tx->uuid.'/reject', [
                'reason' => 'Amount mismatch.',
            ]);

        $response->assertRedirect();
        $response->assertSessionHas('success');

        $this->assertDatabaseHas('transactions', [
            'id' => $tx->id,
            'approval_status' => Transaction::APPROVAL_REJECTED,
        ]);

        $this->assertDatabaseHas('audit_events', [
            'entity_uuid' => $tx->uuid,
            'action' => 'reject',
        ]);
    }

    public function test_reject_without_reason_fails_validation(): void
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

        $response = $this->actingAs($user)
            ->post('/transactions/'.$tx->uuid.'/reject', []);

        $response->assertSessionHasErrors(['reason']);
    }

    public function test_field_engineer_cannot_approve_from_web(): void
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

        $response = $this->actingAs($user)
            ->post('/transactions/'.$tx->uuid.'/approve');

        $response->assertStatus(403);
    }

    public function test_admin_can_view_sync_monitor(): void
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');

        $response = $this->actingAs($user)->get('/sync');

        $response->assertStatus(200);
        $response->assertSee('Device Sync Status');
    }

    public function test_viewer_cannot_access_sync_monitor(): void
    {
        $user = User::factory()->create();
        $user->assignRole('VIEWER');

        $response = $this->actingAs($user)->get('/sync');

        $response->assertStatus(403);
    }
}

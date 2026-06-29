<?php

namespace Tests\Feature;

use App\Models\MasterLocation;
use App\Models\Project;
use App\Models\TaskExpense;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class TaskExpenseApiTest extends TestCase
{
    use RefreshDatabase;

    private User $engineer;
    private User $supervisor;
    private User $owner;
    private User $finance;
    private Project $project;
    private MasterLocation $location;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(\Database\Seeders\RolePermissionSeeder::class);
        $this->seed(\Database\Seeders\BudgetItemTemplateSeeder::class);

        $this->engineer = User::factory()->create();
        $this->engineer->assignRole('FIELD_ENGINEER');

        $this->supervisor = User::factory()->create();
        $this->supervisor->assignRole('SUPERVISOR');

        $this->owner = User::factory()->create();
        $this->owner->assignRole('OWNER');

        $this->finance = User::factory()->create();
        $this->finance->assignRole('FINANCE_MANAGER');

        $this->project = Project::factory()->create(['user_id' => $this->engineer->id]);

        $this->location = MasterLocation::create([
            'uuid' => \Illuminate\Support\Str::uuid(),
            'project_id' => $this->project->id,
            'remote_name' => 'BNI ATM Sanya Mart',
            'address' => 'Jl. A.H. Nasution 108, Kendari',
            'created_by' => $this->owner->id,
        ]);
    }

    /** @test */
    public function engineer_can_create_draft_task_expense()
    {
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id,
                'location_id' => $this->location->id,
                'task_no' => '101757',
                'vid' => 'BNM26071',
                'task_name' => 'Order PSB M2M Permanent',
                'job_type' => 'INSTALASI',
                'items' => [
                    ['template_id' => 1, 'tanggal' => '2026-06-11', 'estimated_amount' => 120000],
                    ['template_id' => 2, 'tanggal' => '2026-06-11', 'estimated_amount' => 200000],
                ],
            ]);

        $response->assertCreated()
            ->assertJsonPath('data.stage', 'DRAFT')
            ->assertJsonPath('data.task_no', '101757')
            ->assertJsonPath('data.remote_name', 'BNI ATM Sanya Mart')
            ->assertJsonCount(2, 'data.items');

        $this->assertDatabaseHas('task_expenses', [
            'task_no' => '101757',
            'stage' => 'DRAFT',
            'submitted_by' => $this->engineer->id,
        ]);
    }

    /** @test */
    public function engineer_cannot_create_more_than_5_drafts()
    {
        for ($i = 1; $i <= 5; $i++) {
            TaskExpense::create([
                'uuid' => \Illuminate\Support\Str::uuid(),
                'project_id' => $this->project->id,
                'task_no' => "TASK-{$i}",
                'vid' => "VID-{$i}",
                'job_type' => 'INSTALASI',
                'stage' => 'DRAFT',
                'submitted_by' => $this->engineer->id,
            ]);
        }

        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id,
                'task_no' => 'TASK-6',
                'vid' => 'VID-6',
                'job_type' => 'INSTALASI',
            ]);

        $response->assertStatus(422)
            ->assertJsonPath('message', 'Maksimal 5 draft. Harap hapus atau selesaikan draft yang ada.');
    }

    /** @test */
    public function full_happy_path_workflow()
    {
        // 1. Engineer creates draft
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id,
                'task_no' => '101758',
                'vid' => 'BNM26072',
                'job_type' => 'SURVEY',
                'items' => [
                    ['template_id' => 32, 'tanggal' => '2026-06-27', 'estimated_amount' => 300000],
                ],
            ]);
        $response->assertCreated();
        $uuid = $response->json('data.uuid');

        // 2. Engineer submits
        $submit = $this->actingAs($this->engineer, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/submit");
        $submit->assertOk()->assertJsonPath('data.stage', 'ESTIMASI');

        // 3. Supervisor forwards (with revision)
        $forward = $this->actingAs($this->supervisor, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/forward", [
                'items' => [
                    ['id' => $response->json('data.items.0.id'), 'revised_amount' => 250000],
                ],
                'notes' => 'Budget dikurangi karena lokasi dekat',
            ]);
        $forward->assertOk()->assertJsonPath('data.stage', 'FORWARDED');

        // 4. Owner approves
        $approve = $this->actingAs($this->owner, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/approve", [
                'items' => [
                    ['id' => $response->json('data.items.0.id'), 'approved_amount' => 250000, 'item_status' => 'APPROVED'],
                ],
            ]);
        $approve->assertOk()->assertJsonPath('data.stage', 'APPROVED');

        // 5. Engineer realizes
        $realize = $this->actingAs($this->engineer, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/realize", [
                'items' => [
                    ['id' => $response->json('data.items.0.id'), 'realization_amount' => 245000],
                ],
            ]);
        $realize->assertOk()->assertJsonPath('data.stage', 'REALISASI');

        // 6. Finance verifies
        $verify = $this->actingAs($this->finance, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/verify", [
                'items' => [
                    ['id' => $response->json('data.items.0.id'), 'bill_verified' => true],
                ],
            ]);
        $verify->assertOk()->assertJsonPath('data.stage', 'VERIFIED');

        // 7. Finance reconciles
        $reconcile = $this->actingAs($this->finance, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/reconcile");
        $reconcile->assertOk()->assertJsonPath('data.stage', 'RECONCILED');

        // Verify totals tracked correctly
        $this->assertDatabaseHas('task_expenses', [
            'uuid' => $uuid,
            'total_estimated' => 300000,
            'total_revised' => 250000,
            'total_approved' => 250000,
            'total_realization' => 245000,
        ]);

        // Verify audit trail
        $this->assertDatabaseHas('task_expense_histories', [
            'action' => 'submitted',
            'old_stage' => 'DRAFT',
            'new_stage' => 'ESTIMASI',
        ]);
        $this->assertDatabaseHas('task_expense_histories', [
            'action' => 'reconciled',
            'old_stage' => 'VERIFIED',
            'new_stage' => 'RECONCILED',
        ]);
    }

    /** @test */
    public function supervisor_can_reject_back_to_draft()
    {
        // Create and submit
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id,
                'task_no' => '101759',
                'vid' => 'BNM26073',
                'job_type' => 'DISMANTLE',
            ]);
        $uuid = $response->json('data.uuid');

        $this->actingAs($this->engineer, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/submit");

        // Supervisor rejects
        $reject = $this->actingAs($this->supervisor, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/reject", [
                'rejection_reason' => 'Anggaran tidak realistis, mohon direvisi',
            ]);

        $reject->assertOk()
            ->assertJsonPath('data.stage', 'DRAFT')
            ->assertJsonPath('data.rejection_reason', 'Anggaran tidak realistis, mohon direvisi');

        // Items should be reset
        $this->assertDatabaseHas('task_expenses', [
            'uuid' => $uuid,
            'stage' => 'DRAFT',
            'total_revised' => 0,
            'total_approved' => 0,
        ]);
    }

    /** @test */
    public function engineer_can_see_rejection_history()
    {
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id,
                'task_no' => '101760',
                'vid' => 'BNM26074',
                'job_type' => 'PMCM',
            ]);
        $uuid = $response->json('data.uuid');

        $this->actingAs($this->engineer, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/submit");

        $this->actingAs($this->supervisor, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/reject", [
                'rejection_reason' => 'Revisi item #2',
            ]);

        $histories = $this->actingAs($this->engineer, 'sanctum')
            ->getJson("/api/v1/task-expenses/{$uuid}/histories");

        $histories->assertOk()
            ->assertJsonCount(2, 'data') // submitted + rejected
            ->assertJsonPath('data.0.action', 'rejected')
            ->assertJsonPath('data.0.notes', 'Revisi item #2');
    }

    /** @test */
    public function engineer_can_delete_own_draft()
    {
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id,
                'task_no' => 'TO-DELETE',
                'vid' => 'VID-DEL',
                'job_type' => 'INSTALASI',
            ]);
        $uuid = $response->json('data.uuid');

        $delete = $this->actingAs($this->engineer, 'sanctum')
            ->deleteJson("/api/v1/task-expenses/{$uuid}");

        $delete->assertOk();
        $this->assertSoftDeleted('task_expenses', ['uuid' => $uuid]);
    }

    /** @test */
    public function cannot_delete_non_draft_task()
    {
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id,
                'task_no' => 'CANT-DELETE',
                'vid' => 'VID-NO-DEL',
                'job_type' => 'INSTALASI',
            ]);
        $uuid = $response->json('data.uuid');

        $this->actingAs($this->engineer, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/submit");

        $delete = $this->actingAs($this->engineer, 'sanctum')
            ->deleteJson("/api/v1/task-expenses/{$uuid}");

        $delete->assertStatus(422);
    }

    /** @test */
    public function engineer_cannot_approve_own_budget()
    {
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id, 'task_no' => 'SEC-001', 'vid' => 'SEC-001',
                'job_type' => 'INSTALASI',
                'items' => [['template_id' => 32, 'tanggal' => '2026-06-27', 'estimated_amount' => 50000]],
            ]);
        $uuid = $response->json('data.uuid');
        $this->actingAs($this->engineer, 'sanctum')->postJson("/api/v1/task-expenses/{$uuid}/submit");
        $this->actingAs($this->supervisor, 'sanctum')->postJson("/api/v1/task-expenses/{$uuid}/forward");
        $approve = $this->actingAs($this->engineer, 'sanctum')
            ->postJson("/api/v1/task-expenses/{$uuid}/approve", [
                'items' => [['id' => $response->json('data.items.0.id'), 'approved_amount' => 50000]],
            ]);
        $approve->assertForbidden();
    }

    /** @test */
    public function pagu_enforcement_blocks_exceeded_fixed_pagu()
    {
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id, 'task_no' => 'PAGU-001', 'vid' => 'PAGU-001',
                'job_type' => 'INSTALASI',
                'items' => [['template_id' => 1, 'tanggal' => '2026-06-27', 'estimated_amount' => 500000]],
            ]);
        $response->assertStatus(422)->assertJsonPath('violations.0.type', 'EXCEEDED');
    }

    /** @test */
    public function hotel_can_exceed_pagu_with_warning()
    {
        $response = $this->actingAs($this->engineer, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id, 'task_no' => 'PAGU-002', 'vid' => 'PAGU-002',
                'job_type' => 'INSTALASI',
                'items' => [['template_id' => 2, 'tanggal' => '2026-06-27', 'estimated_amount' => 350000]],
            ]);
        $response->assertCreated()->assertJsonPath('warnings.0.type', 'HOTEL_EXCEED');
    }

    /** @test */
    public function non_engineer_cannot_create_budget_request()
    {
        $this->actingAs($this->supervisor, 'sanctum')
            ->postJson('/api/v1/task-expenses', [
                'project_id' => $this->project->id, 'task_no' => 'SEC-003', 'vid' => 'SEC-003',
                'job_type' => 'INSTALASI',
            ])->assertForbidden();
    }
}

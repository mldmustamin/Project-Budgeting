<?php

namespace Tests\Feature\Web;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class DashboardTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_guest_is_redirected_to_login(): void
    {
        $response = $this->get('/dashboard');

        $response->assertRedirect('/login');
    }

    public function test_authenticated_user_can_view_dashboard(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $response = $this->actingAs($user)->get('/dashboard');

        $response->assertStatus(200);
        $response->assertSee('Dashboard');
        $response->assertSee('Dana Masuk');
    }

    public function test_dashboard_shows_pending_approval_count(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $project = Project::factory()->create();

        Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'approval_status' => 'PENDING',
        ]);

        $response = $this->actingAs($user)->get('/dashboard');

        $response->assertStatus(200);
        $response->assertSee('Pending');
    }

    public function test_user_can_view_projects(): void
    {
        $user = User::factory()->create();
        $user->assignRole('AUDITOR');

        Project::factory()->count(3)->create(['user_id' => $user->id]);

        $response = $this->actingAs($user)->get('/projects');

        $response->assertStatus(200);
        $response->assertSee('Daftar Project');
    }

    public function test_user_can_view_transactions_with_filters(): void
    {
        $user = User::factory()->create();
        $user->assignRole('AUDITOR');

        $project = Project::factory()->create(['user_id' => $user->id]);

        Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'type' => 'FUND_IN',
        ]);

        $response = $this->actingAs($user)->get('/transactions?type=FUND_IN');

        $response->assertStatus(200);
        $response->assertSee('Dana Masuk');
    }

    public function test_user_can_view_transaction_detail(): void
    {
        $user = User::factory()->create();
        $user->assignRole('AUDITOR');

        $project = Project::factory()->create(['user_id' => $user->id]);

        $tx = Transaction::factory()->create([
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
        ]);

        $response = $this->actingAs($user)->get('/transactions/'.$tx->uuid);

        $response->assertStatus(200);
        $response->assertSee('Detail Transaksi');
    }
}

<?php

namespace Tests\Unit;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use App\Services\TransactionSummaryService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class TransactionSummaryServiceTest extends TestCase
{
    use RefreshDatabase;

    private TransactionSummaryService $service;

    protected function setUp(): void
    {
        parent::setUp();
        $this->service = new TransactionSummaryService();
    }

    public function test_mixed_transaction_types_produce_correct_summary(): void
    {
        $user = User::factory()->create();
        $project = Project::factory()->create();

        $fundIn = Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id,
            'type' => Transaction::TYPE_FUND_IN,
            'reported_amount' => 1000000, 'real_amount' => 1000000,
        ]);
        $office1 = Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id,
            'type' => Transaction::TYPE_OFFICE_EXPENSE,
            'reported_amount' => 500000, 'real_amount' => 450000,
        ]);
        $office2 = Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id,
            'type' => Transaction::TYPE_OFFICE_EXPENSE,
            'reported_amount' => 300000, 'real_amount' => 280000,
        ]);
        $personal = Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id,
            'type' => Transaction::TYPE_PERSONAL_EXPENSE,
            'reported_amount' => 200000, 'real_amount' => 200000,
        ]);

        $transactions = collect([$fundIn, $office1, $office2, $personal]);
        $result = $this->service->calculate($transactions);

        // Verify individual totals
        $this->assertSame(1000000, $result['total_fund_in']);
        $this->assertSame(800000, $result['total_office_reported']); // 500000 + 300000
        $this->assertSame(730000, $result['total_office_real']);     // 450000 + 280000
        $this->assertSame(200000, $result['total_personal_expense']);

        // Verify derived values
        $this->assertSame(70000, $result['saving']);                 // 800000 - 730000
        $this->assertSame(200000, $result['remaining_reported']);    // 1000000 - 800000
        $this->assertSame(270000, $result['remaining_real']);        // 1000000 - 730000
        $this->assertSame(930000, $result['total_cash_out']);        // 730000 + 200000
        $this->assertSame(70000, $result['net_position']);           // 1000000 - 930000
    }

    public function test_soft_deleted_transaction_is_ignored(): void
    {
        $user = User::factory()->create();
        $project = Project::factory()->create();

        $active = Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id,
            'type' => Transaction::TYPE_FUND_IN,
            'reported_amount' => 1000000, 'real_amount' => 1000000,
            'deleted_at' => null,
        ]);
        $deleted = Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id,
            'type' => Transaction::TYPE_OFFICE_EXPENSE,
            'reported_amount' => 500000, 'real_amount' => 500000,
            'deleted_at' => now(),
        ]);

        $transactions = collect([$active, $deleted]);
        $result = $this->service->calculate($transactions);

        $this->assertSame(1000000, $result['total_fund_in']);
        $this->assertSame(0, $result['total_office_reported']);  // deleted row ignored
        $this->assertSame(0, $result['total_office_real']);
        $this->assertSame(0, $result['total_personal_expense']);
        $this->assertSame(1000000, $result['net_position']);
    }

    public function test_empty_collection_returns_all_zero(): void
    {
        $result = $this->service->calculate(collect([]));

        $this->assertSame(0, $result['total_fund_in']);
        $this->assertSame(0, $result['total_office_reported']);
        $this->assertSame(0, $result['total_office_real']);
        $this->assertSame(0, $result['total_personal_expense']);
        $this->assertSame(0, $result['saving']);
        $this->assertSame(0, $result['remaining_reported']);
        $this->assertSame(0, $result['remaining_real']);
        $this->assertSame(0, $result['total_cash_out']);
        $this->assertSame(0, $result['net_position']);
    }

    public function test_all_outputs_are_integers(): void
    {
        $user = User::factory()->create();
        $project = Project::factory()->create();

        Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id,
            'type' => Transaction::TYPE_FUND_IN,
            'reported_amount' => 1234567, 'real_amount' => 1234567,
        ]);

        $transactions = Transaction::all();
        $result = $this->service->calculate($transactions);

        foreach ($result as $key => $value) {
            $this->assertIsInt($value, "Field '$key' should be an integer, got: " . gettype($value));
        }
    }
}

<?php

namespace Database\Factories;

use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;
use Illuminate\Support\Str;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\Transaction>
 */
class TransactionFactory extends Factory
{
    protected $model = Transaction::class;

    public function definition(): array
    {
        $type = $this->faker->randomElement(['FUND_IN', 'OFFICE_EXPENSE', 'PERSONAL_EXPENSE']);
        $project = Project::factory()->create();

        return [
            'uuid' => (string) Str::uuid(),
            'user_id' => User::factory(),
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'user_uuid' => (string) Str::uuid(),
            'type' => $type,
            'date' => $this->faker->date(),
            'description' => $this->faker->sentence(),
            'reported_amount' => $this->faker->numberBetween(10000, 10000000),
            'real_amount' => $this->faker->numberBetween(10000, 10000000),
            'approval_status' => 'DRAFT',
            'finance_status' => 'ACTIVE',
            'sync_status' => 'PENDING',
        ];
    }
}
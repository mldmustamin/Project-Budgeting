<?php

namespace Database\Factories;

use App\Models\AccountingPeriod;
use Illuminate\Database\Eloquent\Factories\Factory;
use Illuminate\Support\Str;

class AccountingPeriodFactory extends Factory
{
    protected $model = AccountingPeriod::class;

    public function definition(): array
    {
        $start = $this->faker->dateTimeBetween('-6 months', 'now');

        return [
            'uuid' => (string) Str::uuid(),
            'period_start' => $start->format('Y-m-d'),
            'period_end' => (clone $start)->modify('+1 month')->format('Y-m-d'),
            'status' => 'OPEN',
        ];
    }
}

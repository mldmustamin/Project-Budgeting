<?php

namespace Database\Factories;

use App\Models\ClickLog;
use Illuminate\Database\Eloquent\Factories\Factory;

class ClickLogFactory extends Factory
{
    protected $model = ClickLog::class;

    public function definition(): array
    {
        return [
            'session_id' => 'sess-' . fake()->uuid(),
            'user_id'    => null,
            'url'        => fake()->url(),
            'selector'   => '#' . fake()->word(),
            'tag'        => fake()->randomElement(['BUTTON', 'A', 'INPUT', null]),
            'text'       => fake()->randomElement(['Submit', 'Cancel', null]),
            'meta'       => null,
            'created_at' => now(),
        ];
    }
}

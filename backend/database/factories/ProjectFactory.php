<?php

namespace Database\Factories;

use App\Models\Project;
use Illuminate\Database\Eloquent\Factories\Factory;
use Illuminate\Support\Str;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\Project>
 */
class ProjectFactory extends Factory
{
    protected $model = Project::class;

    public function definition(): array
    {
        return [
            'uuid' => (string) Str::uuid(),
            'user_id' => \App\Models\User::factory(),
            'name' => fake()->company(),
            'description' => fake()->sentence(),
            'is_archived' => false,
            'start_at' => fake()->dateTimeThisYear(),
            'completed_at' => null,
            'sync_status' => 'PENDING',
        ];
    }
}
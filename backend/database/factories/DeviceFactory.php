<?php

namespace Database\Factories;

use App\Models\Device;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;
use Illuminate\Support\Str;

/**
 * @extends \Illuminate\Database\Eloquent\Factories\Factory<\App\Models\Device>
 */
class DeviceFactory extends Factory
{
    protected $model = Device::class;

    public function definition(): array
    {
        return [
            'uuid' => (string) Str::uuid(),
            'user_id' => User::factory(),
            'device_name' => $this->faker->word() . ' ' . $this->faker->randomElement(['Phone', 'Tablet']),
            'device_platform' => $this->faker->randomElement(['android', 'ios']),
            'device_version' => $this->faker->numerify('##.#'),
        ];
    }
}
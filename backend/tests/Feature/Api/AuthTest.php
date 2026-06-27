<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AuthTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_login_returns_token_and_user_with_roles(): void
    {
        $user = User::factory()->create(['password' => bcrypt('password')]);
        $user->assignRole('FIELD_ENGINEER');

        $response = $this->postJson('/api/v1/auth/login', [
            'email' => $user->email,
            'password' => 'password',
            'device_name' => 'android-test',
        ]);

        $response->assertStatus(200)
            ->assertJsonStructure([
                'user' => ['id', 'uuid', 'name', 'email', 'roles'],
                'access_token',
                'token_type',
            ])
            ->assertJsonPath('user.roles', ['FIELD_ENGINEER'])
            ->assertJsonPath('token_type', 'Bearer');
    }

    public function test_login_with_invalid_credentials_returns_422(): void
    {
        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'nonexistent@example.com',
            'password' => 'wrong',
        ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['email']);
    }

    public function test_me_returns_authenticated_user_with_roles(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');

        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->getJson('/api/v1/auth/me');

        $response->assertStatus(200)
            ->assertJsonPath('user.id', $user->id)
            ->assertJsonPath('user.roles', ['OWNER']);
    }

    public function test_logout_deletes_all_user_tokens(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('test')->plainTextToken;

        $this->assertDatabaseCount('personal_access_tokens', 1);

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/auth/logout');

        $response->assertStatus(200)
            ->assertJson(['message' => 'Token revoked.']);

        $this->assertDatabaseCount('personal_access_tokens', 0);
    }
}
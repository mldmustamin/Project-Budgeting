<?php

namespace Tests\Feature\Web;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SearchWebTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(\Database\Seeders\RolePermissionSeeder::class);
    }

    /** @test */
    public function search_page_loads_for_authenticated_user()
    {
        $user = User::factory()->create();
        $user->assignRole('ADMIN');

        $response = $this->actingAs($user)
            ->get('/search?q=test');

        $response->assertOk();
    }

    /** @test */
    public function unauthenticated_user_redirected_to_login()
    {
        $response = $this->get('/search?q=test');

        $response->assertRedirect('/login');
    }
}

<?php

namespace Tests\Feature\Api;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CrashReportApiTest extends TestCase
{
    use RefreshDatabase;

    /** @test */
    public function can_submit_crash_report_from_android()
    {
        $user = User::factory()->create();

        $response = $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/crash-reports', [
                'report'       => 'NullPointerException at SummaryScreen.kt:42',
                'device_model' => 'Samsung Galaxy A14',
                'app_version'  => 'v2.0.0-b20',
            ]);

        $response->assertCreated();
    }
}

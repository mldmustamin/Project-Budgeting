<?php

namespace Tests\Console;

use App\Models\ClickLog;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CleanupClickLogsTest extends TestCase
{
    use RefreshDatabase;

    /** @test */
    public function it_deletes_logs_older_than_retention_period()
    {
        config(['click_logger.retention_days' => 14]);

        $old    = ClickLog::factory()->create(['created_at' => now()->subDays(20)]);
        $recent = ClickLog::factory()->create(['created_at' => now()->subDays(2)]);

        $this->artisan('click-logs:cleanup')->assertSuccessful();

        $this->assertDatabaseMissing('click_logs', ['id' => $old->id]);
        $this->assertDatabaseHas('click_logs', ['id' => $recent->id]);
    }
}

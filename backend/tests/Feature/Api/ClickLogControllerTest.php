<?php

namespace Tests\Feature\Api;

use App\Models\ClickLog;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ClickLogControllerTest extends TestCase
{
    use RefreshDatabase;

    protected function validPayload(array $overrides = []): array
    {
        return array_merge([
            'logs' => [[
                'timestamp' => now()->toIso8601String(),
                'action'    => 'click',
                'selector'  => '#btn-submit',
                'url'       => '/checkout',
                'text'      => 'Submit',
                'tag'       => 'BUTTON',
                'meta'      => ['viewport' => ['w' => 1280, 'h' => 720]],
            ]],
        ], $overrides);
    }

    /** @test */
    public function it_stores_valid_click_logs()
    {
        $user = \App\Models\User::factory()->create();

        $response = $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/logs', $this->validPayload(), [
                'X-Log-Session-Id' => 'sess-123',
            ]);

        $response->assertStatus(201);
        $this->assertDatabaseHas('click_logs', [
            'session_id' => 'sess-123',
            'selector'   => '#btn-submit',
            'url'        => '/checkout',
        ]);
    }

    /** @test */
    public function it_rejects_payload_without_required_fields()
    {
        $user = \App\Models\User::factory()->create();

        $response = $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/logs', [
                'logs' => [['action' => 'click']],
            ]);

        $response->assertStatus(422)
            ->assertJsonValidationErrors(['logs.0.selector', 'logs.0.url', 'logs.0.timestamp']);
    }

    /** @test */
    public function it_rejects_more_than_max_logs_per_batch()
    {
        $user = \App\Models\User::factory()->create();
        $logs = array_fill(0, 51, [
            'timestamp' => now()->toIso8601String(),
            'action'    => 'click',
            'selector'  => '#x',
            'url'       => '/x',
        ]);

        $response = $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/logs', ['logs' => $logs]);

        $response->assertStatus(422)->assertJsonValidationErrors(['logs']);
    }

    /** @test */
    public function it_redacts_sensitive_text_before_storing()
    {
        $user = \App\Models\User::factory()->create();
        config(['click_logger.redact_keys' => ['password', 'card']]);

        $payload = $this->validPayload();
        $payload['logs'][0]['text'] = 'password: secret123';

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/logs', $payload, ['X-Log-Session-Id' => 'sess-456']);

        $log = ClickLog::where('session_id', 'sess-456')->first();
        $this->assertStringNotContainsString('secret123', $log->text);
    }

    /** @test */
    public function it_does_not_store_when_logger_disabled()
    {
        $user = \App\Models\User::factory()->create();
        config(['click_logger.enabled' => false]);

        $response = $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/logs', $this->validPayload());

        $response->assertStatus(403);
        $this->assertDatabaseCount('click_logs', 0);
    }

    /** @test */
    public function it_requires_authentication()
    {
        $response = $this->postJson('/api/v1/logs', $this->validPayload());

        $response->assertUnauthorized();
    }
}

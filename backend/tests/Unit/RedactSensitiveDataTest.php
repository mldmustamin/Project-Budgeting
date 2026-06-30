<?php

namespace Tests\Unit;

use App\Services\RedactSensitiveData;
use Tests\TestCase;

class RedactSensitiveDataTest extends TestCase
{
    /** @test */
    public function it_redacts_text_containing_sensitive_keyword()
    {
        $redactor = new RedactSensitiveData(['password', 'token']);
        $this->assertSame('[REDACTED]', $redactor->handle('my password: abc123'));
    }

    /** @test */
    public function it_leaves_safe_text_untouched()
    {
        $redactor = new RedactSensitiveData(['password']);
        $this->assertSame('Submit Order', $redactor->handle('Submit Order'));
    }

    /** @test */
    public function it_is_case_insensitive()
    {
        $redactor = new RedactSensitiveData(['password']);
        $this->assertSame('[REDACTED]', $redactor->handle('PASSWORD field clicked'));
    }

    /** @test */
    public function it_handles_null_text()
    {
        $redactor = new RedactSensitiveData(['password']);
        $this->assertNull($redactor->handle(null));
    }
}

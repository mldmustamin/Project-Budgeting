<?php

namespace App\Services;

class RedactSensitiveData
{
    public function __construct(private array $redactKeys = []) {}

    public function handle(?string $text): ?string
    {
        if ($text === null) {
            return null;
        }

        $lower = mb_strtolower($text);
        foreach ($this->redactKeys as $key) {
            if (str_contains($lower, mb_strtolower($key))) {
                return '[REDACTED]';
            }
        }

        return $text;
    }
}

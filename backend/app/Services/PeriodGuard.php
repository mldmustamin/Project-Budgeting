<?php

namespace App\Services;

use App\Models\AccountingPeriod;
use Illuminate\Validation\ValidationException;

class PeriodGuard
{
    /**
     * Throw if the given date falls within a closed accounting period.
     */
    public static function guard(string $date): void
    {
        $closedPeriod = AccountingPeriod::where('status', 'CLOSED')
            ->where('period_start', '<=', $date)
            ->where('period_end', '>=', $date)
            ->first();

        if ($closedPeriod) {
            throw ValidationException::withMessages([
                'date' => ["Tanggal {$date} berada dalam periode tertutup ({$closedPeriod->period_start->format('Y-m-d')} s/d {$closedPeriod->period_end->format('Y-m-d')})."],
            ]);
        }
    }
}

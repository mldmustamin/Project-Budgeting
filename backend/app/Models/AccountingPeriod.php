<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class AccountingPeriod extends Model
{
    /** @use HasFactory<\Database\Factories\AccountingPeriodFactory> */
    use HasFactory;
    protected $fillable = [
        'uuid', 'period_start', 'period_end', 'status',
        'closed_by', 'closed_at', 'reopened_by', 'reopened_at', 'reason',
    ];

    protected function casts(): array
    {
        return [
            'period_start' => 'date',
            'period_end' => 'date',
            'closed_at' => 'datetime',
            'reopened_at' => 'datetime',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (AccountingPeriod $period) {
            if (empty($period->uuid)) {
                $period->uuid = (string) Str::uuid();
            }
        });
    }

    /**
     * Check if a given date falls within any closed period.
     */
    public static function isDateInClosedPeriod(string $date): bool
    {
        return static::where('status', 'CLOSED')
            ->where('period_start', '<=', $date)
            ->where('period_end', '>=', $date)
            ->exists();
    }

    /**
     * Route model binding by uuid.
     */
    public function getRouteKeyName(): string
    {
        return 'uuid';
    }
}

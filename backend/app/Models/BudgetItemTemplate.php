<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class BudgetItemTemplate extends Model
{
    protected $fillable = [
        'uuid', 'category_name', 'category_group', 'pagu_type',
        'pagu_amount', 'pagu_note', 'requires_bill', 'bill_note',
        'display_order', 'is_active',
    ];

    protected function casts(): array
    {
        return [
            'pagu_amount' => 'integer',
            'requires_bill' => 'boolean',
            'is_active' => 'boolean',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (BudgetItemTemplate $template) {
            if (empty($template->uuid)) {
                $template->uuid = (string) Str::uuid();
            }
        });
    }

    public function getRouteKeyName(): string
    {
        return 'uuid';
    }

    public function expenseItems(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(ExpenseItem::class, 'template_id');
    }

    public function paguAmounts(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(PaguJobTypeAmount::class, 'template_id');
    }

    // Get pagu amount for a specific job type
    public function getAmountForJobType(string $jobType): ?int
    {
        $amount = $this->paguAmounts()->forJobType($jobType)->first();
        return $amount?->amount;
    }

    // Scope: active templates
    public function scopeActive($query)
    {
        return $query->where('is_active', true)->orderBy('display_order');
    }

    // Scope: templates filtered by pagu type
    public function scopeOfType($query, string $paguType)
    {
        return $query->where('pagu_type', $paguType);
    }
}

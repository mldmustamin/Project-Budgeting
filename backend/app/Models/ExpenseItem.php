<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class ExpenseItem extends Model
{
    protected $fillable = [
        'uuid', 'task_expense_id', 'template_id', 'tanggal', 'note',
        'estimated_amount', 'revised_amount', 'approved_amount', 'realization_amount',
        'bukti_path', 'requires_bill', 'bill_verified',
        'item_status', 'rejection_reason', 'sort_order',
    ];

    protected function casts(): array
    {
        return [
            'tanggal' => 'date',
            'estimated_amount' => 'integer',
            'revised_amount' => 'integer',
            'approved_amount' => 'integer',
            'realization_amount' => 'integer',
            'requires_bill' => 'boolean',
            'bill_verified' => 'boolean',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (ExpenseItem $item) {
            if (empty($item->uuid)) {
                $item->uuid = (string) Str::uuid();
            }
        });
    }

    public function getRouteKeyName(): string
    {
        return 'uuid';
    }

    public function taskExpense(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(TaskExpense::class);
    }

    public function template(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(BudgetItemTemplate::class, 'template_id');
    }
}

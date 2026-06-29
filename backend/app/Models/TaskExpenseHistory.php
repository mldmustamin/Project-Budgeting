<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class TaskExpenseHistory extends Model
{
    protected $fillable = [
        'task_expense_id', 'actor_id', 'action',
        'old_stage', 'new_stage', 'notes', 'metadata',
    ];

    protected function casts(): array
    {
        return [
            'metadata' => 'array',
        ];
    }

    public function taskExpense(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(TaskExpense::class);
    }

    public function actor(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'actor_id');
    }
}

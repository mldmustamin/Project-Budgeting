<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class PaguJobTypeAmount extends Model
{
    protected $table = 'pagu_job_type_amounts';

    protected $fillable = [
        'template_id', 'job_type', 'amount',
    ];

    protected function casts(): array
    {
        return [
            'amount' => 'integer',
        ];
    }

    public function template(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(BudgetItemTemplate::class, 'template_id');
    }

    // Scope: get amount for specific job_type
    public function scopeForJobType($query, string $jobType)
    {
        return $query->where('job_type', $jobType);
    }
}

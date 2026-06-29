<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

class TaskExpense extends Model
{
    use SoftDeletes;

    // Stages
    const STAGE_DRAFT = 'DRAFT';
    const STAGE_ESTIMASI = 'ESTIMASI';
    const STAGE_FORWARDED = 'FORWARDED';
    const STAGE_APPROVED = 'APPROVED';
    const STAGE_REALISASI = 'REALISASI';
    const STAGE_VERIFIED = 'VERIFIED';
    const STAGE_RECONCILED = 'RECONCILED';
    const STAGE_REJECTED = 'REJECTED';

    // Job Types
    const JOB_INSTALASI = 'INSTALASI';
    const JOB_RELOKASI = 'RELOKASI';
    const JOB_PMCM = 'PMCM';
    const JOB_DISMANTLE = 'DISMANTLE';
    const JOB_SURVEY = 'SURVEY';

    protected $fillable = [
        'uuid', 'project_id', 'location_id', 'task_no', 'vid',
        'task_name', 'remote_name', 'job_type', 'stage',
        'submitted_by', 'forwarded_by', 'approved_by',
        'verified_by', 'reconciled_by',
        'total_estimated', 'total_revised', 'total_approved', 'total_realization',
        'rejection_reason', 'notes', 'completed_at', 'deadline_at',
        'sync_status', 'last_synced_at',
    ];

    protected function casts(): array
    {
        return [
            'total_estimated' => 'integer',
            'total_revised' => 'integer',
            'total_approved' => 'integer',
            'total_realization' => 'integer',
            'completed_at' => 'datetime',
            'deadline_at' => 'datetime',
            'last_synced_at' => 'datetime',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (TaskExpense $task) {
            if (empty($task->uuid)) {
                $task->uuid = (string) Str::uuid();
            }
        });
    }

    public function getRouteKeyName(): string
    {
        return 'uuid';
    }

    // Relationships
    public function project(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Project::class);
    }

    public function location(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(MasterLocation::class, 'location_id');
    }

    public function submittedBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'submitted_by');
    }

    public function forwardedBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'forwarded_by');
    }

    public function approvedBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'approved_by');
    }

    public function verifiedBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'verified_by');
    }

    public function reconciledBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'reconciled_by');
    }

    public function items(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(ExpenseItem::class);
    }

    public function histories(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(TaskExpenseHistory::class);
    }

    public function laporanPekerjaan(): \Illuminate\Database\Eloquent\Relations\HasOne
    {
        return $this->hasOne(LaporanPekerjaan::class);
    }

    // Scopes
    public function scopeByStage($query, string $stage)
    {
        return $query->where('stage', $stage);
    }

    public function scopeForUser($query, User $user)
    {
        return $query->where('submitted_by', $user->id);
    }

    // Total calculations
    public function recalculateTotals(): void
    {
        $this->total_estimated = $this->items->sum('estimated_amount');
        $this->total_revised = $this->items->sum('revised_amount');
        $this->total_approved = $this->items->sum('approved_amount');
        $this->total_realization = $this->items->sum('realization_amount');
        $this->save();
    }

    // Count drafts for a user (max 5 enforcement)
    public static function draftCountForUser(User $user): int
    {
        return static::where('submitted_by', $user->id)
            ->where('stage', self::STAGE_DRAFT)
            ->count();
    }
}

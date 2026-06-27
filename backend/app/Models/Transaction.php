<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

class Transaction extends Model
{
    use SoftDeletes;
    /** @use HasFactory<\Database\Factories\TransactionFactory> */
    use HasFactory;

    // Transaction types
    const TYPE_FUND_IN = 'FUND_IN';
    const TYPE_OFFICE_EXPENSE = 'OFFICE_EXPENSE';
    const TYPE_PERSONAL_EXPENSE = 'PERSONAL_EXPENSE';

    // Approval statuses
    const APPROVAL_DRAFT = 'DRAFT';
    const APPROVAL_PENDING = 'PENDING';
    const APPROVAL_APPROVED = 'APPROVED';
    const APPROVAL_REJECTED = 'REJECTED';
    const APPROVAL_DISPUTED = 'DISPUTED';

    // Finance statuses
    const FINANCE_ACTIVE = 'ACTIVE';
    const FINANCE_CORRECTED = 'CORRECTED';
    const FINANCE_VOIDED = 'VOIDED';

    /**
     * Route model binding — resolve by uuid.
     */
    public function getRouteKeyName(): string
    {
        return 'uuid';
    }

    protected $fillable = [
        'uuid', 'user_id', 'project_id', 'account_id', 'category_id',
        'type', 'date', 'description', 'reported_amount', 'real_amount',
        'source_text', 'note', 'legacy_hash', 'server_id', 'device_id',
        'sync_status', 'approval_status', 'finance_status', 'session_id',
        'server_user_id', 'user_uuid', 'project_uuid', 'last_synced_at',
        'disputed_amount', 'dispute_reason', 'dispute_response',
        'disputed_by', 'disputed_at', 'dispute_resolved_by', 'dispute_resolved_at',
    ];

    protected function casts(): array
    {
        return [
            'date' => 'date',
            'reported_amount' => 'integer',
            'real_amount' => 'integer',
            'last_synced_at' => 'datetime',
            'disputed_at' => 'datetime',
            'dispute_resolved_at' => 'datetime',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (Transaction $transaction) {
            if (empty($transaction->uuid)) {
                $transaction->uuid = (string) Str::uuid();
            }
        });
    }

    public function user(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function project(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Project::class);
    }

    public function account(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Account::class);
    }

    public function category(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Category::class);
    }

    public function attachments(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(Attachment::class);
    }
}
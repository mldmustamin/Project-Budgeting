<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class SyncOutbox extends Model
{
    protected $fillable = [
        'uuid', 'user_id', 'device_id', 'session_id',
        'entity_type', 'entity_uuid', 'operation', 'payload',
        'idempotency_key', 'status', 'rejection_reason', 'last_synced_at',
    ];

    protected function casts(): array
    {
        return [
            'payload' => 'json',
            'last_synced_at' => 'datetime',
        ];
    }

    public function user(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
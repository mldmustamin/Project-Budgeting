<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

class MasterLocation extends Model
{
    use SoftDeletes;

    protected $fillable = [
        'uuid', 'project_id', 'remote_name', 'address',
        'provinsi', 'kota_kab', 'latitude', 'longitude',
        'created_by', 'updated_by',
    ];

    protected function casts(): array
    {
        return [
            'latitude' => 'decimal:7',
            'longitude' => 'decimal:7',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (MasterLocation $location) {
            if (empty($location->uuid)) {
                $location->uuid = (string) Str::uuid();
            }
        });
    }

    public function getRouteKeyName(): string
    {
        return 'uuid';
    }

    public function project(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Project::class);
    }

    public function createdBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'created_by');
    }

    public function updatedBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'updated_by');
    }

    public function taskExpenses(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(TaskExpense::class, 'location_id');
    }
}

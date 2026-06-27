<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class ProjectAssignment extends Model
{
    /** @use HasFactory<\Database\Factories\ProjectAssignmentFactory> */
    use HasFactory;

    protected $fillable = [
        'uuid', 'project_id', 'user_id', 'role_on_project',
        'active_from', 'active_until',
    ];

    protected function casts(): array
    {
        return [
            'active_from' => 'datetime',
            'active_until' => 'datetime',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (ProjectAssignment $assignment) {
            if (empty($assignment->uuid)) {
                $assignment->uuid = (string) Str::uuid();
            }
        });
    }

    public function project(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(Project::class);
    }

    public function user(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
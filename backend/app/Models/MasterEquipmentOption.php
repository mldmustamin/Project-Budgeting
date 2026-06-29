<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class MasterEquipmentOption extends Model
{
    protected $table = 'master_equipment_options';

    protected $fillable = [
        'field_key', 'label', 'sort_order', 'is_active',
    ];

    protected function casts(): array
    {
        return [
            'is_active' => 'boolean',
        ];
    }

    // Scope: get options for a specific dropdown
    public function scopeForField($query, string $fieldKey)
    {
        return $query->where('field_key', $fieldKey)
            ->where('is_active', true)
            ->orderBy('sort_order');
    }
}

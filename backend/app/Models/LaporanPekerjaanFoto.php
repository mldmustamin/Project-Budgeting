<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class LaporanPekerjaanFoto extends Model
{
    protected $table = 'laporan_pekerjaan_foto';

    protected $fillable = [
        'uuid', 'laporan_pekerjaan_id', 'file_path', 'file_name',
        'mime_type', 'description', 'sort_order',
    ];

    protected static function booted(): void
    {
        static::creating(function (LaporanPekerjaanFoto $foto) {
            if (empty($foto->uuid)) {
                $foto->uuid = (string) Str::uuid();
            }
        });
    }

    public function getRouteKeyName(): string
    {
        return 'uuid';
    }

    public function laporanPekerjaan(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(LaporanPekerjaan::class);
    }
}

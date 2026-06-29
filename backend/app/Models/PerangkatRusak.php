<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class PerangkatRusak extends Model
{
    protected $table = 'perangkat_rusak';

    protected $fillable = [
        'uuid', 'laporan_pekerjaan_id', 'jenis',
        'type_modem_lama', 'sn_modem_lama', 'esn_modem_lama',
        'sn_adaptor_lama', 'sn_lnb_lama', 'sn_buc_lama',
        'sn_router_lama', 'sn_adaptor_router_lama',
        'sn_ap_lama', 'sn_adaptor_ap_lama',
        'm2m_type_modem_lama', 'm2m_sn_modem_lama', 'm2m_sn_adaptor_lama',
        'sort_order',
    ];

    protected static function booted(): void
    {
        static::creating(function (PerangkatRusak $p) {
            if (empty($p->uuid)) {
                $p->uuid = (string) Str::uuid();
            }
        });
    }

    public function laporanPekerjaan(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(LaporanPekerjaan::class);
    }
}

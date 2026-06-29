<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class PerangkatTerpasang extends Model
{
    protected $table = 'perangkat_terpasang';

    protected $fillable = [
        'uuid', 'laporan_pekerjaan_id', 'jenis',
        'jenis_antenna', 'jenis_mounting', 'jenis_ifl',
        'type_modem', 'sn_modem', 'esn_modem', 'sn_adaptor_modem',
        'sn_buc', 'sn_lnb', 'sn_router', 'sn_adaptor_router',
        'sn_ap', 'sn_adaptor_ap',
        'm2m_type_modem', 'm2m_sn_modem', 'm2m_sn_adaptor', 'm2m_no_simcard',
        'sort_order',
    ];

    protected static function booted(): void
    {
        static::creating(function (PerangkatTerpasang $p) {
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

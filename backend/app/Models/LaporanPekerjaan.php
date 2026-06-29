<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Str;

class LaporanPekerjaan extends Model
{
    protected $table = 'laporan_pekerjaan';

    const STATUS_DRAFT = 'DRAFT';
    const STATUS_SUBMITTED = 'SUBMITTED';
    const STATUS_VERIFIED = 'VERIFIED';

    protected $fillable = [
        'uuid', 'task_expense_id',
        // Tim
        'nama_teknisi', 'no_hp_teknisi', 'koordinator',
        'tgl_berangkat', 'tgl_tiba', 'tgl_mulai', 'tgl_online', 'tgl_selesai', 'tgl_pulang',
        // Customer
        'nama_customer', 'alamat_customer', 'provinsi', 'kota_kab',
        'site_id', 'pic_lokasi', 'ip_lan',
        // Parameter
        'hub_satelite', 'sqf_awal', 'sqf_pointing', 'initial_esno', 'target_esno',
        'hasil_xpol', 'cpi', 'c_n', 'asiasat', 'chinasat',
        'petugas_tcc', 'petugas_hd', 'signal_telkomsel', 'signal_indosat',
        // Sarpen
        'kondisi_ac', 'kondisi_blower_box', 'sumber_elektrikal',
        'p_n', 'p_g', 'n_g',
        // Tindakan
        'tindakan_teknisi', 'tindakan_flm',
        // Catatan
        'penyebab_gangguan', 'perangkat_diganti', 'catatan',
        'status', 'verified_by', 'verified_at',
    ];

    protected function casts(): array
    {
        return [
            'tgl_berangkat' => 'datetime',
            'tgl_tiba' => 'datetime',
            'tgl_mulai' => 'datetime',
            'tgl_online' => 'datetime',
            'tgl_selesai' => 'datetime',
            'tgl_pulang' => 'datetime',
            'verified_at' => 'datetime',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (LaporanPekerjaan $laporan) {
            if (empty($laporan->uuid)) {
                $laporan->uuid = (string) Str::uuid();
            }
        });
    }

    public function getRouteKeyName(): string
    {
        return 'uuid';
    }

    public function taskExpense(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(TaskExpense::class);
    }

    public function verifiedBy(): \Illuminate\Database\Eloquent\Relations\BelongsTo
    {
        return $this->belongsTo(User::class, 'verified_by');
    }

    public function perangkatTerpasang(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(PerangkatTerpasang::class);
    }

    public function perangkatRusak(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(PerangkatRusak::class);
    }

    public function fotos(): \Illuminate\Database\Eloquent\Relations\HasMany
    {
        return $this->hasMany(LaporanPekerjaanFoto::class);
    }
}

<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('laporan_pekerjaan', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('task_expense_id')->unique()->constrained('task_expenses')->cascadeOnDelete();

            // Section 1: Tim
            $table->string('nama_teknisi')->nullable();
            $table->string('no_hp_teknisi')->nullable();
            $table->string('koordinator')->nullable();
            $table->timestamp('tgl_berangkat')->nullable();
            $table->timestamp('tgl_tiba')->nullable();
            $table->timestamp('tgl_mulai')->nullable();
            $table->timestamp('tgl_online')->nullable();
            $table->timestamp('tgl_selesai')->nullable();
            $table->timestamp('tgl_pulang')->nullable();

            // Section 2: Customer (redundant but useful for reporting)
            $table->string('nama_customer')->nullable();
            $table->text('alamat_customer')->nullable();
            $table->string('provinsi')->nullable();
            $table->string('kota_kab')->nullable();
            $table->string('site_id')->nullable();
            $table->string('pic_lokasi')->nullable();
            $table->string('ip_lan')->nullable();

            // Section 4: Parameter (VSAT)
            $table->string('hub_satelite')->nullable();
            $table->string('sqf_awal')->nullable();
            $table->string('sqf_pointing')->nullable();
            $table->string('initial_esno')->nullable();
            $table->string('target_esno')->nullable();
            $table->string('hasil_xpol')->nullable();
            $table->string('cpi')->nullable();
            $table->string('c_n')->nullable();
            $table->string('asiasat')->nullable();
            $table->string('chinasat')->nullable();
            $table->string('petugas_tcc')->nullable();
            $table->string('petugas_hd')->nullable();
            $table->string('signal_telkomsel')->nullable();
            $table->string('signal_indosat')->nullable();

            // Section 5: Sarpen
            $table->string('kondisi_ac')->nullable();
            $table->string('kondisi_blower_box')->nullable();
            $table->string('sumber_elektrikal')->nullable();
            $table->string('p_n')->nullable();
            $table->string('p_g')->nullable();
            $table->string('n_g')->nullable();

            // Section 6: Detail Tindakan
            $table->text('tindakan_teknisi')->nullable();
            $table->text('tindakan_flm')->nullable();

            // Section 7: Catatan
            $table->text('penyebab_gangguan')->nullable();
            $table->text('perangkat_diganti')->nullable();
            $table->text('catatan')->nullable();

            $table->enum('status', ['DRAFT', 'SUBMITTED', 'VERIFIED'])->default('DRAFT');
            $table->foreignId('verified_by')->nullable()->constrained('users');
            $table->timestamp('verified_at')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('laporan_pekerjaan');
    }
};

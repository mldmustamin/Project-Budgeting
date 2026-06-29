<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Perangkat yang DIPASANG (VSAT or M2M)
        Schema::create('perangkat_terpasang', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('laporan_pekerjaan_id')->constrained('laporan_pekerjaan')->cascadeOnDelete();
            $table->enum('jenis', ['VSAT', 'M2M']);

            // VSAT fields
            $table->string('jenis_antenna')->nullable();
            $table->string('jenis_mounting')->nullable();
            $table->string('jenis_ifl')->nullable();
            $table->string('type_modem')->nullable();
            $table->string('sn_modem')->nullable();
            $table->string('esn_modem')->nullable();
            $table->string('sn_adaptor_modem')->nullable();
            $table->string('sn_buc')->nullable();
            $table->string('sn_lnb')->nullable();
            $table->string('sn_router')->nullable();
            $table->string('sn_adaptor_router')->nullable();
            $table->string('sn_ap')->nullable();
            $table->string('sn_adaptor_ap')->nullable();

            // M2M fields
            $table->string('m2m_type_modem')->nullable();
            $table->string('m2m_sn_modem')->nullable();
            $table->string('m2m_sn_adaptor')->nullable();
            $table->string('m2m_no_simcard')->nullable();

            $table->integer('sort_order')->default(0);
            $table->timestamps();
        });

        // Perangkat yang RUSAK/DIGANTI (VSAT or M2M)
        Schema::create('perangkat_rusak', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('laporan_pekerjaan_id')->constrained('laporan_pekerjaan')->cascadeOnDelete();
            $table->enum('jenis', ['VSAT', 'M2M']);

            // VSAT fields (old/damaged)
            $table->string('type_modem_lama')->nullable();
            $table->string('sn_modem_lama')->nullable();
            $table->string('esn_modem_lama')->nullable();
            $table->string('sn_adaptor_lama')->nullable();
            $table->string('sn_lnb_lama')->nullable();
            $table->string('sn_buc_lama')->nullable();
            $table->string('sn_router_lama')->nullable();
            $table->string('sn_adaptor_router_lama')->nullable();
            $table->string('sn_ap_lama')->nullable();
            $table->string('sn_adaptor_ap_lama')->nullable();

            // M2M fields (old/damaged)
            $table->string('m2m_type_modem_lama')->nullable();
            $table->string('m2m_sn_modem_lama')->nullable();
            $table->string('m2m_sn_adaptor_lama')->nullable();

            $table->integer('sort_order')->default(0);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('perangkat_rusak');
        Schema::dropIfExists('perangkat_terpasang');
    }
};

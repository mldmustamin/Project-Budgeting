<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('master_equipment_options', function (Blueprint $table) {
            $table->id();
            $table->string('field_key');    // 'JENIS_ANTENNA', 'JENIS_MOUNTING', 'TYPE_MODEM', 'PENYEBAB_GANGGUAN', etc.
            $table->string('label');         // 'ANTENNA - CBand 1,8m', 'Baseplate kingpost', etc.
            $table->integer('sort_order')->default(0);
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->index('field_key');
            $table->unique(['field_key', 'label']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('master_equipment_options');
    }
};

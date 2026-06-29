<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('pagu_job_type_amounts', function (Blueprint $table) {
            $table->id();
            $table->foreignId('template_id')->constrained('budget_item_templates')->cascadeOnDelete();
            $table->enum('job_type', ['INSTALASI', 'RELOKASI', 'PMCM', 'DISMANTLE', 'SURVEY']);
            $table->bigInteger('amount')->nullable(); // null = not applicable (e.g. BURUH not for PMCM)
            $table->timestamps();

            $table->unique(['template_id', 'job_type']);
            $table->index('job_type');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('pagu_job_type_amounts');
    }
};

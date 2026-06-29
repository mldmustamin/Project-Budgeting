<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('budget_item_templates', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->string('category_name'); // Display name: "Tiket Pesawat Berangkat"
            $table->string('category_group')->unique(); // Internal key: "TIKET_PESAWAT_BERANGKAT"
            $table->enum('pagu_type', ['FIXED_PAGU', 'TICKET', 'MANAGER_APPROVAL']);
            $table->bigInteger('pagu_amount')->nullable(); // null for TICKET and MANAGER_APPROVAL
            $table->string('pagu_note')->nullable(); // "Sesuai Gojek online", "Travel/Bus/Kapal"
            $table->boolean('requires_bill')->default(false);
            $table->text('bill_note')->nullable();
            $table->integer('display_order')->default(0);
            $table->boolean('is_active')->default(true);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('budget_item_templates');
    }
};

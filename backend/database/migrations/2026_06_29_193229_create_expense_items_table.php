<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('expense_items', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('task_expense_id')->constrained('task_expenses')->cascadeOnDelete();
            $table->foreignId('template_id')->nullable()->constrained('budget_item_templates')->nullOnDelete();
            $table->date('tanggal');
            $table->text('note')->nullable();
            $table->bigInteger('estimated_amount')->default(0);   // FE input
            $table->bigInteger('revised_amount')->nullable();      // SUPERVISOR edit
            $table->bigInteger('approved_amount')->nullable();     // OWNER final
            $table->bigInteger('realization_amount')->nullable();  // FE realization
            $table->text('bukti_path')->nullable();                // path to kwitansi/bill photo
            $table->boolean('requires_bill')->default(false);
            $table->boolean('bill_verified')->default(false);
            $table->enum('item_status', ['DRAFT', 'APPROVED', 'REJECTED'])->default('DRAFT');
            $table->text('rejection_reason')->nullable();
            $table->integer('sort_order')->default(0);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('expense_items');
    }
};

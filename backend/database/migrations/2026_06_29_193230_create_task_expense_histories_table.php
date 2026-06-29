<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('task_expense_histories', function (Blueprint $table) {
            $table->id();
            $table->foreignId('task_expense_id')->constrained('task_expenses')->cascadeOnDelete();
            $table->foreignId('actor_id')->constrained('users');
            $table->string('action'); // 'submitted', 'forwarded', 'approved', 'rejected', 'realized', 'verified', 'reconciled'
            $table->string('old_stage')->nullable();
            $table->string('new_stage');
            $table->text('notes')->nullable();
            $table->json('metadata')->nullable(); // extra data: revised items, approved amounts, etc.
            $table->timestamps();

            $table->index(['task_expense_id', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('task_expense_histories');
    }
};

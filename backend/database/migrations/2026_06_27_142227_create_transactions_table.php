<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('transactions', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('project_id')->constrained()->cascadeOnDelete();
            $table->foreignId('account_id')->nullable()->constrained()->nullOnDelete();
            $table->foreignId('category_id')->nullable()->constrained()->nullOnDelete();
            $table->string('type'); // FUND_IN, OFFICE_EXPENSE, PERSONAL_EXPENSE
            $table->date('date');
            $table->string('description');
            $table->bigInteger('reported_amount');
            $table->bigInteger('real_amount');
            $table->text('source_text')->nullable();
            $table->text('note')->nullable();
            $table->string('legacy_hash')->nullable()->unique();
            $table->string('server_id')->nullable();
            $table->string('device_id')->nullable();
            $table->string('sync_status')->default('PENDING');
            $table->string('approval_status')->default('DRAFT');
            $table->string('finance_status')->default('ACTIVE');
            $table->string('session_id')->nullable();
            $table->string('server_user_id')->nullable();
            $table->uuid('user_uuid')->nullable();
            $table->uuid('project_uuid')->nullable();
            $table->timestamp('last_synced_at')->nullable();
            $table->timestamps();
            $table->softDeletes();

            $table->index('sync_status');
            $table->index('server_id');
            $table->index('approval_status');
            $table->index('finance_status');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('transactions');
    }
};
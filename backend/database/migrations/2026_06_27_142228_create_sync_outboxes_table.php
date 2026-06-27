<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('sync_outboxes', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('device_id');
            $table->string('session_id');
            $table->string('entity_type'); // transaction, attachment
            $table->uuid('entity_uuid');
            $table->string('operation'); // CREATE, UPDATE, DELETE
            $table->json('payload');
            $table->string('idempotency_key')->unique();
            $table->string('status')->default('PENDING'); // PENDING, SYNCED, REJECTED, CONFLICT
            $table->text('rejection_reason')->nullable();
            $table->timestamp('last_synced_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'device_id', 'session_id']);
            $table->index('status');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('sync_outboxes');
    }
};
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('projects', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('name');
            $table->text('description')->nullable();
            $table->boolean('is_archived')->default(false);
            $table->timestamp('start_at')->nullable();
            $table->timestamp('completed_at')->nullable();
            $table->string('server_id')->nullable();
            $table->string('device_id')->nullable();
            $table->string('sync_status')->default('PENDING');
            $table->timestamp('last_synced_at')->nullable();
            $table->timestamps();
            $table->softDeletes();

            $table->index('sync_status');
            $table->index('server_id');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('projects');
    }
};
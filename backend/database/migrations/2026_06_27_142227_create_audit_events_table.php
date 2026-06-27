<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('audit_events', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('entity_type'); // project, transaction, user, etc.
            $table->string('entity_uuid'); // uuid of the entity
            $table->string('action'); // create, update, soft_delete, approve, reject, void, correction
            $table->json('old_value')->nullable();
            $table->json('new_value')->nullable();
            $table->string('device_id')->nullable();
            $table->string('session_id')->nullable();
            $table->string('reason')->nullable();
            $table->timestamps();

            $table->index(['entity_type', 'entity_uuid']);
            $table->index('user_id');
            $table->index('created_at');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('audit_events');
    }
};
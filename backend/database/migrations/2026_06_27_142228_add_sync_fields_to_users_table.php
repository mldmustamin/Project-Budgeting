<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->uuid('uuid')->unique()->nullable()->after('id');
            $table->string('server_id')->nullable()->after('uuid');
            $table->string('sync_status')->default('PENDING')->after('remember_token');
            $table->timestamp('last_synced_at')->nullable()->after('sync_status');
            $table->softDeletes();
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn(['uuid', 'server_id', 'sync_status', 'last_synced_at', 'deleted_at']);
        });
    }
};
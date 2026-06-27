<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('transactions', function (Blueprint $table) {
            $table->bigInteger('disputed_amount')->nullable()->after('real_amount');
            $table->text('dispute_reason')->nullable()->after('disputed_amount');
            $table->text('dispute_response')->nullable()->after('dispute_reason');
            $table->foreignId('disputed_by')->nullable()->after('dispute_response')->constrained('users')->nullOnDelete();
            $table->timestamp('disputed_at')->nullable()->after('disputed_by');
            $table->foreignId('dispute_resolved_by')->nullable()->after('disputed_at')->constrained('users')->nullOnDelete();
            $table->timestamp('dispute_resolved_at')->nullable()->after('dispute_resolved_by');
        });
    }

    public function down(): void
    {
        Schema::table('transactions', function (Blueprint $table) {
            $table->dropForeign(['disputed_by']);
            $table->dropForeign(['dispute_resolved_by']);
            $table->dropColumn(['disputed_amount', 'dispute_reason', 'dispute_response',
                'disputed_by', 'disputed_at', 'dispute_resolved_by', 'dispute_resolved_at']);
        });
    }
};

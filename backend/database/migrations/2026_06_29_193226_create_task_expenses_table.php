<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('task_expenses', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('project_id')->constrained()->cascadeOnDelete();
            $table->foreignId('location_id')->nullable()->constrained('master_locations')->nullOnDelete();
            $table->string('task_no'); // "101757"
            $table->string('vid'); // "BNM26071"
            $table->text('task_name')->nullable(); // "Order PSB M2M Permanent Lokasi BNI ATM Sanya Mart"
            $table->string('remote_name')->nullable(); // auto-filled from location
            $table->enum('job_type', ['INSTALASI', 'RELOKASI', 'PMCM', 'DISMANTLE', 'SURVEY']);
            $table->enum('stage', [
                'DRAFT', 'ESTIMASI', 'FORWARDED', 'APPROVED',
                'REALISASI', 'VERIFIED', 'RECONCILED', 'REJECTED'
            ])->default('DRAFT');
            $table->foreignId('submitted_by')->constrained('users');
            $table->foreignId('forwarded_by')->nullable()->constrained('users');
            $table->foreignId('approved_by')->nullable()->constrained('users');
            $table->foreignId('verified_by')->nullable()->constrained('users');
            $table->foreignId('reconciled_by')->nullable()->constrained('users');
            $table->bigInteger('total_estimated')->default(0);
            $table->bigInteger('total_revised')->default(0);
            $table->bigInteger('total_approved')->default(0);
            $table->bigInteger('total_realization')->default(0);
            $table->text('rejection_reason')->nullable();
            $table->text('notes')->nullable();
            $table->timestamp('completed_at')->nullable();
            $table->timestamp('deadline_at')->nullable();
            $table->string('sync_status')->default('PENDING');
            $table->timestamp('last_synced_at')->nullable();
            $table->timestamps();
            $table->softDeletes();

            $table->index('stage');
            $table->index('job_type');
            $table->index(['submitted_by', 'stage']);
            $table->index(['project_id', 'stage']);
            $table->unique(['task_no', 'vid']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('task_expenses');
    }
};

<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('master_locations', function (Blueprint $table) {
            $table->id();
            $table->uuid('uuid')->unique();
            $table->foreignId('project_id')->constrained()->cascadeOnDelete();
            $table->string('remote_name'); // "BNI ATM Sanya Mart"
            $table->text('address'); // "Jl. A.H. Nasution 108, Anduonohu, Kendari"
            $table->string('provinsi')->nullable(); // "SULAWESI TENGGARA"
            $table->string('kota_kab')->nullable(); // "KOTA KENDARI"
            $table->decimal('latitude', 10, 7)->nullable();
            $table->decimal('longitude', 10, 7)->nullable();
            $table->foreignId('created_by')->constrained('users');
            $table->foreignId('updated_by')->nullable()->constrained('users');
            $table->timestamps();
            $table->softDeletes();

            $table->index(['project_id', 'remote_name']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('master_locations');
    }
};

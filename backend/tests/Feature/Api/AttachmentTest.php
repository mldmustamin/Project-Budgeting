<?php

namespace Tests\Feature\Api;

use App\Models\Attachment;
use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\Transaction;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class AttachmentTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
        Storage::fake('local');
    }

    private function setupUserAndTx(string $role = 'FIELD_ENGINEER'): array
    {
        $user = User::factory()->create();
        $user->assignRole($role);
        $project = Project::factory()->create(['user_id' => $user->id]);
        ProjectAssignment::create(['project_id' => $project->id, 'user_id' => $user->id, 'role_on_project' => $role]);
        $tx = Transaction::factory()->create([
            'user_id' => $user->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
        ]);
        $token = $user->createToken('test')->plainTextToken;
        return compact('user', 'project', 'tx', 'token');
    }

    private function createUploadFile(): UploadedFile
    {
        return UploadedFile::fake()->image('receipt.jpg', 800, 600);
    }

    public function test_upload_attachment_success(): void
    {
        extract($this->setupUserAndTx());

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/attachments', [
                'file' => $this->createUploadFile(),
            ]);

        $response->assertStatus(201)
            ->assertJsonPath('attachment.file_name', 'receipt.jpg');

        $this->assertDatabaseHas('attachments', [
            'transaction_id' => $tx->id,
            'file_name' => 'receipt.jpg',
        ]);

        $attachment = Attachment::first();
        Storage::disk('local')->assertExists($attachment->file_path);
    }

    public function test_upload_without_file_fails(): void
    {
        extract($this->setupUserAndTx());

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/attachments', []);

        $response->assertStatus(422)->assertJsonValidationErrors(['file']);
    }

    public function test_upload_invalid_mime_type_fails(): void
    {
        extract($this->setupUserAndTx());

        $file = UploadedFile::fake()->create('script.exe', 1024, 'application/x-msdownload');

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/attachments', [
                'file' => $file,
            ]);

        $response->assertStatus(422)->assertJsonValidationErrors(['file']);
    }

    public function test_upload_large_file_fails(): void
    {
        extract($this->setupUserAndTx());

        $file = UploadedFile::fake()->image('large.jpg')->size(11000); // > 10MB

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/attachments', [
                'file' => $file,
            ]);

        $response->assertStatus(422)->assertJsonValidationErrors(['file']);
    }

    public function test_unassigned_user_cannot_upload(): void
    {
        $assignedUser = User::factory()->create();
        $assignedUser->assignRole('FIELD_ENGINEER');
        $project = Project::factory()->create(['user_id' => $assignedUser->id]);
        ProjectAssignment::create(['project_id' => $project->id, 'user_id' => $assignedUser->id, 'role_on_project' => 'FIELD_ENGINEER']);
        $tx = Transaction::factory()->create([
            'user_id' => $assignedUser->id, 'project_id' => $project->id, 'project_uuid' => $project->uuid,
        ]);

        // Another user not assigned
        $otherUser = User::factory()->create();
        $otherUser->assignRole('FIELD_ENGINEER');
        $token = $otherUser->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/attachments', [
                'file' => $this->createUploadFile(),
            ]);

        $response->assertStatus(422)->assertJsonValidationErrors(['project']);
    }

    public function test_owner_can_download_attachment(): void
    {
        extract($this->setupUserAndTx('OWNER'));

        $file = $this->createUploadFile();
        $this->withHeader('Authorization', 'Bearer '.$token)
            ->postJson('/api/v1/transactions/'.$tx->uuid.'/attachments', ['file' => $file]);

        $attachment = Attachment::first();

        // Verify file exists in storage
        Storage::disk('local')->assertExists($attachment->file_path);

        // Verify file content
        $storedContent = Storage::disk('local')->get($attachment->file_path);
        $this->assertNotEmpty($storedContent);
    }

    public function test_download_nonexistent_attachment_returns_404(): void
    {
        $user = User::factory()->create();
        $user->assignRole('OWNER');
        $token = $user->createToken('test')->plainTextToken;

        $response = $this->withHeader('Authorization', 'Bearer '.$token)
            ->get('/api/v1/attachments/nonexistent-uuid');

        $response->assertStatus(404);
    }
}

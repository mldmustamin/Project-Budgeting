<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Attachment;
use App\Models\ProjectAssignment;
use App\Models\Transaction;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\ValidationException;

class AttachmentController extends Controller
{
    private const MAX_FILE_SIZE_KB = 10240; // 10MB
    private const ALLOWED_MIMES = [
        'image/jpeg',
        'image/png',
        'image/webp',
        'application/pdf',
    ];
    private const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'pdf'];

    public function store(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        $this->authorizeAccess($user, $transaction);

        $request->validate([
            'file' => [
                'required',
                'file',
                'max:'.self::MAX_FILE_SIZE_KB,
                'mimetypes:'.implode(',', self::ALLOWED_MIMES),
                'mimes:'.implode(',', self::ALLOWED_EXTENSIONS),
            ],
        ]);

        $uploadedFile = $request->file('file');
        $fileName = $uploadedFile->getClientOriginalName();
        $mimeType = $uploadedFile->getMimeType();

        // Store to local disk under attachments/{transaction_uuid}/
        $path = $uploadedFile->store(
            'attachments/'.$transaction->uuid,
            'local'
        );

        $attachment = Attachment::create([
            'transaction_id' => $transaction->id,
            'file_path' => $path,
            'file_name' => $fileName,
            'mime_type' => $mimeType,
            'sync_status' => 'PENDING',
            'device_id' => $request->header('X-Device-Id'),
        ]);

        return response()->json([
            'attachment' => [
                'uuid' => $attachment->uuid,
                'file_name' => $attachment->file_name,
                'mime_type' => $attachment->mime_type,
                'sync_status' => $attachment->sync_status,
                'created_at' => $attachment->created_at,
            ],
        ], 201);
    }

    public function show(Attachment $attachment, Request $request): mixed
    {
        $user = $request->user();

        $this->authorizeAccess($user, $attachment->transaction);

        if (! Storage::disk('local')->exists($attachment->file_path)) {
            return response()->json([
                'error' => 'File not found.',
            ], 404);
        }

        return Storage::disk('local')->response($attachment->file_path);
    }

    private function authorizeAccess($user, Transaction $transaction): void
    {
        // OWNER, ADMIN, AUDITOR can access any
        if ($user->hasRole(['OWNER', 'ADMIN', 'AUDITOR'])) {
            return;
        }

        // Check project assignment
        $assigned = ProjectAssignment::where('project_id', $transaction->project_id)
            ->where('user_id', $user->id)
            ->exists();

        if (! $assigned) {
            throw ValidationException::withMessages([
                'project' => ['You are not assigned to this project.'],
            ]);
        }
    }
}

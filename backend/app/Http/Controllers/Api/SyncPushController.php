<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditEvent;
use App\Models\Device;
use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\SyncOutbox;
use App\Models\Transaction;
use App\Services\PeriodGuard;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

class SyncPushController extends Controller
{
    private const MAX_BATCH_SIZE = 50;
    private const SUPPORTED_ENTITY_TYPES = ['transaction'];
    private const SUPPORTED_OPERATIONS = ['CREATE', 'UPDATE', 'SOFT_DELETE'];

    public function push(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'device_uuid' => ['required', 'string', 'uuid'],
            'operations' => ['required', 'array', 'min:1', 'max:' . self::MAX_BATCH_SIZE],
            'operations.*.idempotency_key' => ['required', 'string', 'max:500'],
            'operations.*.entity_type' => ['required', 'string'],
            'operations.*.entity_uuid' => ['required', 'string', 'uuid'],
            'operations.*.operation' => ['required', 'string'],
            'operations.*.payload' => ['nullable', 'array'],
        ]);

        $user = $request->user();

        // Validate device belongs to the authenticated user
        $device = Device::where('uuid', $validated['device_uuid'])
            ->where('user_id', $user->id)
            ->first();

        if (! $device) {
            return response()->json([
                'error' => 'DEVICE_NOT_REGISTERED',
                'message' => 'Device not registered or does not belong to this user.',
            ], 401);
        }

        if ($device->is_revoked) {
            return response()->json([
                'error' => 'DEVICE_REVOKED',
                'message' => 'Device has been revoked.',
            ], 403);
        }

        $results = [];

        foreach ($validated['operations'] as $operation) {
            $results[] = $this->processOperation($operation, $user, $device);
        }

        return response()->json([
            'results' => $results,
        ]);
    }

    private function processOperation(array $operation, $user, Device $device): array
    {
        $idempotencyKey = $operation['idempotency_key'];
        $entityUuid = $operation['entity_uuid'];
        $opType = $operation['operation'];
        $entityType = $operation['entity_type'];

        // Unsupported entity_type → per-operation REJECTED
        if (! in_array($entityType, self::SUPPORTED_ENTITY_TYPES)) {
            return $this->rejectedResult($idempotencyKey, $entityUuid, 'Unsupported entity_type: ' . $entityType);
        }

        // Unsupported operation → per-operation REJECTED
        if (! in_array($opType, self::SUPPORTED_OPERATIONS)) {
            return $this->rejectedResult($idempotencyKey, $entityUuid, 'Unsupported operation: ' . $opType);
        }

        // Idempotency check — use sync_outboxes.idempotency_key
        $existingOutbox = SyncOutbox::where('idempotency_key', $idempotencyKey)->first();
        if ($existingOutbox) {
            return [
                'idempotency_key' => $idempotencyKey,
                'entity_uuid' => $existingOutbox->entity_uuid,
                'status' => 'DUPLICATE',
                'server_id' => null,
                'reason' => 'Already processed.',
            ];
        }

        // Dispatch to operation-specific handler
        return match ($opType) {
            'CREATE' => $this->processCreate($idempotencyKey, $entityUuid, $operation, $user, $device),
            'UPDATE' => $this->processUpdate($idempotencyKey, $entityUuid, $operation, $user, $device),
            'SOFT_DELETE' => $this->processSoftDelete($idempotencyKey, $entityUuid, $operation, $user, $device),
            default => $this->rejectedResult($idempotencyKey, $entityUuid, 'Unsupported operation: ' . $opType),
        };
    }

    private function processCreate(
        string $idempotencyKey,
        string $entityUuid,
        array $operation,
        $user,
        Device $device
    ): array {
        // Validate payload
        try {
            $payload = $operation['payload'] ?? [];
            $this->validateTransactionPayload($payload);
        } catch (ValidationException $e) {
            $reason = 'Validation failed: ' . implode(', ', array_keys($e->errors()));
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Period guard: reject if date falls in closed period
        try {
            PeriodGuard::guard($payload['date']);
        } catch (ValidationException $e) {
            $reason = $e->errors()['date'][0];
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Resolve project
        $project = Project::where('uuid', $payload['project_uuid'])->first();
        if (! $project) {
            $reason = 'Project not found.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Check project assignment (skip for OWNER/ADMIN)
        if (! $this->isAssignedOrAdmin($user, $project->id)) {
            $reason = 'User not assigned to this project.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Create transaction
        $transaction = Transaction::create([
            'uuid' => $entityUuid,
            'user_id' => $user->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'user_uuid' => $user->uuid,
            'type' => $payload['type'],
            'date' => $payload['date'],
            'description' => $payload['description'] ?? '',
            'reported_amount' => $payload['reported_amount'],
            'real_amount' => $payload['real_amount'],
            'account_id' => $payload['account_id'] ?? null,
            'category_id' => $payload['category_id'] ?? null,
            'note' => $payload['note'] ?? null,
            'source_text' => $payload['source_text'] ?? null,
            'device_id' => $device->uuid,
            'sync_status' => 'SYNCED',
            'approval_status' => Transaction::APPROVAL_DRAFT,
            'finance_status' => Transaction::FINANCE_ACTIVE,
        ]);

        // Write sync_outboxes row
        $this->writeOutbox($operation, $user, $device, 'SYNCED', null);

        // Create audit event
        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'sync_create',
            'new_value' => $transaction->toArray(),
            'device_id' => $device->uuid,
            'session_id' => $idempotencyKey,
        ]);

        return [
            'idempotency_key' => $idempotencyKey,
            'entity_uuid' => $transaction->uuid,
            'status' => 'ACCEPTED',
            'server_id' => (string) $transaction->id,
            'reason' => null,
        ];
    }

    private function processUpdate(
        string $idempotencyKey,
        string $entityUuid,
        array $operation,
        $user,
        Device $device
    ): array {
        // Resolve existing transaction
        $transaction = Transaction::where('uuid', $entityUuid)->first();
        if (! $transaction) {
            $reason = 'Transaction not found.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Check project assignment on the transaction's project
        if (! $this->isAssignedOrAdmin($user, $transaction->project_id)) {
            $reason = 'User not assigned to this project.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Reject update of APPROVED transactions (must use correction)
        if ($transaction->approval_status === Transaction::APPROVAL_APPROVED) {
            $reason = 'Approved transactions cannot be updated. Use correction or void.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Validate payload
        try {
            $payload = $operation['payload'] ?? [];
            $this->validateTransactionPayload($payload);
        } catch (ValidationException $e) {
            $reason = 'Validation failed: ' . implode(', ', array_keys($e->errors()));
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Resolve new project
        $project = Project::where('uuid', $payload['project_uuid'])->first();
        if (! $project) {
            $reason = 'Project not found.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Check project assignment on new project
        if (! $this->isAssignedOrAdmin($user, $project->id)) {
            $reason = 'User not assigned to this project.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Period guard: reject if new date falls in closed period
        try {
            PeriodGuard::guard($payload['date']);
        } catch (ValidationException $e) {
            $reason = $e->errors()['date'][0];
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        $oldValue = $transaction->toArray();

        // Update allowed fields only
        $transaction->update([
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'type' => $payload['type'],
            'date' => $payload['date'],
            'description' => $payload['description'] ?? '',
            'reported_amount' => $payload['reported_amount'],
            'real_amount' => $payload['real_amount'],
            'account_id' => $payload['account_id'] ?? null,
            'category_id' => $payload['category_id'] ?? null,
            'note' => $payload['note'] ?? null,
            'source_text' => $payload['source_text'] ?? null,
            'device_id' => $device->uuid,
            'sync_status' => 'SYNCED',
            'last_synced_at' => now(),
        ]);

        // Write sync_outboxes row
        $this->writeOutbox($operation, $user, $device, 'SYNCED', null);

        // Create audit event
        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'sync_update',
            'old_value' => $oldValue,
            'new_value' => $transaction->fresh()->toArray(),
            'device_id' => $device->uuid,
            'session_id' => $idempotencyKey,
        ]);

        return [
            'idempotency_key' => $idempotencyKey,
            'entity_uuid' => $transaction->uuid,
            'status' => 'ACCEPTED',
            'server_id' => (string) $transaction->id,
            'reason' => null,
        ];
    }

    private function processSoftDelete(
        string $idempotencyKey,
        string $entityUuid,
        array $operation,
        $user,
        Device $device
    ): array {
        // Resolve existing transaction
        $transaction = Transaction::where('uuid', $entityUuid)->first();
        if (! $transaction) {
            $reason = 'Transaction not found.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Check project assignment on the transaction's project
        if (! $this->isAssignedOrAdmin($user, $transaction->project_id)) {
            $reason = 'User not assigned to this project.';
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        // Period guard: reject if transaction date falls in closed period
        try {
            PeriodGuard::guard($transaction->date->format('Y-m-d'));
        } catch (ValidationException $e) {
            $reason = $e->errors()['date'][0];
            $this->writeOutbox($operation, $user, $device, 'REJECTED', $reason);
            return $this->rejectedResult($idempotencyKey, $entityUuid, $reason);
        }

        $oldValue = $transaction->toArray();
        $serverId = (string) $transaction->id;

        // Mark sync fields before soft delete
        $transaction->update([
            'sync_status' => 'SYNCED',
            'last_synced_at' => now(),
            'device_id' => $device->uuid,
        ]);

        // Soft delete
        $transaction->delete();

        // Write sync_outboxes row
        $this->writeOutbox($operation, $user, $device, 'SYNCED', null);

        // Create audit event
        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $entityUuid,
            'action' => 'sync_soft_delete',
            'old_value' => $oldValue,
            'new_value' => null,
            'device_id' => $device->uuid,
            'session_id' => $idempotencyKey,
        ]);

        return [
            'idempotency_key' => $idempotencyKey,
            'entity_uuid' => $entityUuid,
            'status' => 'ACCEPTED',
            'server_id' => $serverId,
            'reason' => null,
        ];
    }

    private function isAssignedOrAdmin($user, int $projectId): bool
    {
        if ($user->hasRole(['OWNER', 'ADMIN'])) {
            return true;
        }

        return ProjectAssignment::where('project_id', $projectId)
            ->where('user_id', $user->id)
            ->exists();
    }

    private function rejectedResult(string $idempotencyKey, string $entityUuid, string $reason): array
    {
        return [
            'idempotency_key' => $idempotencyKey,
            'entity_uuid' => $entityUuid,
            'status' => 'REJECTED',
            'server_id' => null,
            'reason' => $reason,
        ];
    }

    private function writeOutbox(array $operation, $user, Device $device, string $status, ?string $reason): void
    {
        SyncOutbox::create([
            'uuid' => (string) Str::uuid(),
            'user_id' => $user->id,
            'device_id' => $device->uuid,
            'session_id' => $operation['idempotency_key'],
            'entity_type' => $operation['entity_type'],
            'entity_uuid' => $operation['entity_uuid'],
            'operation' => $operation['operation'],
            'payload' => $operation['payload'] ?? [],
            'idempotency_key' => $operation['idempotency_key'],
            'status' => $status,
            'rejection_reason' => $reason,
            'last_synced_at' => now(),
        ]);
    }

    private function validateTransactionPayload(array $payload): void
    {
        $rules = [
            'type' => ['required', 'string', 'in:' . Transaction::TYPE_FUND_IN . ',' . Transaction::TYPE_OFFICE_EXPENSE . ',' . Transaction::TYPE_PERSONAL_EXPENSE],
            'project_uuid' => ['required', 'string', 'uuid'],
            'date' => ['required', 'date_format:Y-m-d'],
            'description' => ['required_if:type,' . Transaction::TYPE_OFFICE_EXPENSE . ',' . Transaction::TYPE_PERSONAL_EXPENSE, 'string', 'max:500'],
            'reported_amount' => ['required', 'integer', 'min:1'],
            'real_amount' => ['required', 'integer', 'min:1'],
            'account_id' => ['nullable', 'integer'],
            'category_id' => ['nullable', 'integer'],
            'note' => ['nullable', 'string', 'max:1000'],
            'source_text' => ['nullable', 'string'],
        ];

        $validator = validator($payload, $rules);

        if ($validator->fails()) {
            throw new ValidationException($validator);
        }
    }
}

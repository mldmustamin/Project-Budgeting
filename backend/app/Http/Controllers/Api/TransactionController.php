<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreTransactionRequest;
use App\Models\AuditEvent;
use App\Models\Project;
use App\Models\Transaction;
use App\Services\PeriodGuard;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

class TransactionController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $query = Transaction::with(['project:id,uuid,name', 'account:id,uuid,name', 'category:id,uuid,name']);

        if ($request->filled('project_uuid')) {
            $query->whereHas('project', fn ($q) => $q->where('uuid', $request->project_uuid));
        }

        if ($request->filled('type')) {
            $query->where('type', $request->type);
        }

        if ($request->filled('approval_status')) {
            $query->where('approval_status', $request->approval_status);
        }

        if ($request->filled('finance_status')) {
            $query->where('finance_status', $request->finance_status);
        }

        $transactions = $query->orderByDesc('date')->orderByDesc('id')->get();

        return response()->json([
            'transactions' => $transactions->map(fn (Transaction $tx) => $this->transactionResponse($tx)),
        ]);
    }

    public function store(StoreTransactionRequest $request): JsonResponse
    {
        $this->authorizeCreate($request);

        $validated = $request->validated();

        PeriodGuard::guard($validated['date']);

        $project = Project::where('uuid', $validated['project_uuid'])->firstOrFail();

        $transaction = Transaction::create([
            'user_id' => $request->user()->id,
            'project_id' => $project->id,
            'project_uuid' => $project->uuid,
            'user_uuid' => $request->user()->uuid,
            'type' => $validated['type'],
            'date' => $validated['date'],
            'description' => $validated['description'] ?? '',
            'reported_amount' => $validated['reported_amount'],
            'real_amount' => $validated['real_amount'],
            'account_id' => $validated['account_id'] ?? null,
            'category_id' => $validated['category_id'] ?? null,
            'note' => $validated['note'] ?? null,
            'source_text' => $validated['source_text'] ?? null,
        ]);

        return response()->json([
            'transaction' => $this->transactionResponse($transaction),
        ], 201);
    }

    public function show(Transaction $transaction): JsonResponse
    {
        return response()->json([
            'transaction' => $this->transactionResponse($transaction->load(['project:id,uuid,name', 'account:id,uuid,name', 'category:id,uuid,name'])),
        ]);
    }

    // ─── Immutability Guard ────────────────────────────────────────────

    private function guardNotApproved(Transaction $transaction): void
    {
        if ($transaction->approval_status === Transaction::APPROVAL_APPROVED) {
            throw ValidationException::withMessages([
                'status' => ['Approved transactions cannot be edited directly. Use correction or void instead.'],
            ]);
        }
    }

    // ─── Approval Endpoints ────────────────────────────────────────────

    public function submit(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        if ($transaction->approval_status !== Transaction::APPROVAL_DRAFT) {
            throw ValidationException::withMessages([
                'status' => ['Only DRAFT transactions can be submitted.'],
            ]);
        }

        $oldStatus = $transaction->approval_status;
        $transaction->update(['approval_status' => Transaction::APPROVAL_PENDING]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'submit',
            'old_value' => ['approval_status' => $oldStatus],
            'new_value' => ['approval_status' => Transaction::APPROVAL_PENDING],
        ]);

        return response()->json([
            'transaction' => $this->transactionResponse($transaction),
            'message' => 'Transaction submitted for approval.',
        ]);
    }

    public function approve(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER, ADMIN, or FINANCE_MANAGER can approve transactions.'],
            ]);
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_PENDING) {
            throw ValidationException::withMessages([
                'status' => ['Only PENDING transactions can be approved.'],
            ]);
        }

        $oldStatus = $transaction->approval_status;
        $transaction->update(['approval_status' => Transaction::APPROVAL_APPROVED]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'approve',
            'old_value' => ['approval_status' => $oldStatus],
            'new_value' => ['approval_status' => Transaction::APPROVAL_APPROVED],
        ]);

        return response()->json([
            'transaction' => $this->transactionResponse($transaction),
            'message' => 'Transaction approved.',
        ]);
    }

    public function reject(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER, ADMIN, or FINANCE_MANAGER can reject transactions.'],
            ]);
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_PENDING) {
            throw ValidationException::withMessages([
                'status' => ['Only PENDING transactions can be rejected.'],
            ]);
        }

        $validated = $request->validate([
            'reason' => ['required', 'string', 'max:1000'],
        ]);

        $oldStatus = $transaction->approval_status;
        $transaction->update(['approval_status' => Transaction::APPROVAL_REJECTED]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'reject',
            'old_value' => ['approval_status' => $oldStatus],
            'new_value' => ['approval_status' => Transaction::APPROVAL_REJECTED],
            'reason' => $validated['reason'],
        ]);

        return response()->json([
            'transaction' => $this->transactionResponse($transaction),
            'message' => 'Transaction rejected.',
        ]);
    }

    // ─── Dispute ────────────────────────────────────────────────────────

    public function dispute(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER, ADMIN, or FINANCE_MANAGER can dispute transactions.'],
            ]);
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_APPROVED) {
            throw ValidationException::withMessages([
                'status' => ['Only APPROVED transactions can be disputed.'],
            ]);
        }

        $validated = $request->validate([
            'disputed_amount' => ['required', 'integer', 'min:1'],
            'dispute_reason' => ['required', 'string', 'max:1000'],
        ]);

        $transaction->update([
            'approval_status' => Transaction::APPROVAL_DISPUTED,
            'disputed_amount' => $validated['disputed_amount'],
            'dispute_reason' => $validated['dispute_reason'],
            'disputed_by' => $user->id,
            'disputed_at' => now(),
        ]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'dispute',
            'old_value' => ['approval_status' => Transaction::APPROVAL_APPROVED, 'real_amount' => $transaction->getOriginal('real_amount')],
            'new_value' => ['approval_status' => Transaction::APPROVAL_DISPUTED, 'disputed_amount' => $validated['disputed_amount']],
            'reason' => $validated['dispute_reason'],
        ]);

        return response()->json([
            'transaction' => $this->transactionResponse($transaction),
            'message' => 'Transaction disputed.',
        ]);
    }

    public function resolveDispute(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER, ADMIN, or FINANCE_MANAGER can resolve disputes.'],
            ]);
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_DISPUTED) {
            throw ValidationException::withMessages([
                'status' => ['Only DISPUTED transactions can be resolved.'],
            ]);
        }

        $validated = $request->validate([
            'action' => ['required', 'string', 'in:accept_dispute,reject_dispute'],
            'resolution_note' => ['nullable', 'string', 'max:1000'],
        ]);

        if ($validated['action'] === 'accept_dispute') {
            // Create correction with disputed amount
            $correction = Transaction::create([
                'user_id' => $user->id,
                'project_id' => $transaction->project_id,
                'project_uuid' => $transaction->project_uuid,
                'user_uuid' => $user->uuid,
                'type' => $transaction->type,
                'date' => $transaction->date,
                'description' => $transaction->description,
                'reported_amount' => $transaction->reported_amount,
                'real_amount' => $transaction->disputed_amount,
                'approval_status' => Transaction::APPROVAL_APPROVED,
                'finance_status' => Transaction::FINANCE_ACTIVE,
                'note' => 'Dispute resolution — correction of ' . $transaction->uuid . ': ' . ($validated['resolution_note'] ?? ''),
            ]);

            $transaction->update([
                'approval_status' => Transaction::APPROVAL_APPROVED,
                'finance_status' => Transaction::FINANCE_CORRECTED,
                'dispute_resolved_by' => $user->id,
                'dispute_resolved_at' => now(),
            ]);

            AuditEvent::create([
                'user_id' => $user->id,
                'entity_type' => 'transaction',
                'entity_uuid' => $correction->uuid,
                'action' => 'dispute_resolved_correction',
                'reason' => $validated['resolution_note'] ?? '',
            ]);

            return response()->json([
                'transaction' => $this->transactionResponse($transaction->fresh()),
                'correction' => $this->transactionResponse($correction),
                'message' => 'Dispute accepted — correction created.',
            ], 201);
        }

        // Reject dispute — restore to APPROVED
        $transaction->update([
            'approval_status' => Transaction::APPROVAL_APPROVED,
            'dispute_resolved_by' => $user->id,
            'dispute_resolved_at' => now(),
        ]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'dispute_resolved_rejected',
            'reason' => $validated['resolution_note'] ?? '',
        ]);

        return response()->json([
            'transaction' => $this->transactionResponse($transaction->fresh()),
            'message' => 'Dispute rejected — transaction restored to APPROVED.',
        ]);
    }

    // ─── Correction & Void ──────────────────────────────────────────────

    public function void(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'FINANCE_MANAGER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER or FINANCE_MANAGER can void transactions.'],
            ]);
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_APPROVED) {
            throw ValidationException::withMessages([
                'status' => ['Only APPROVED transactions can be voided.'],
            ]);
        }

        if ($transaction->finance_status === Transaction::FINANCE_VOIDED) {
            throw ValidationException::withMessages([
                'status' => ['Transaction is already voided.'],
            ]);
        }

        $validated = $request->validate([
            'reason' => ['required', 'string', 'max:1000'],
        ]);

        $oldFinance = $transaction->finance_status;
        $transaction->update(['finance_status' => Transaction::FINANCE_VOIDED]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'void',
            'old_value' => ['finance_status' => $oldFinance],
            'new_value' => ['finance_status' => Transaction::FINANCE_VOIDED],
            'reason' => $validated['reason'],
        ]);

        return response()->json([
            'transaction' => $this->transactionResponse($transaction),
            'message' => 'Transaction voided.',
        ]);
    }

    public function correction(Transaction $transaction, Request $request): JsonResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'FINANCE_MANAGER', 'SUPERVISOR'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER, FINANCE_MANAGER, or SUPERVISOR can create corrections.'],
            ]);
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_APPROVED) {
            throw ValidationException::withMessages([
                'status' => ['Only APPROVED transactions can be corrected.'],
            ]);
        }

        $validated = $request->validate([
            'reported_amount' => ['required', 'integer', 'min:1'],
            'real_amount' => ['required', 'integer', 'min:1'],
            'description' => ['nullable', 'string', 'max:500'],
            'reason' => ['required', 'string', 'max:1000'],
        ]);

        // Mark original as CORRECTED
        $oldFinance = $transaction->finance_status;
        $transaction->update(['finance_status' => Transaction::FINANCE_CORRECTED]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $transaction->uuid,
            'action' => 'corrected',
            'old_value' => ['finance_status' => $oldFinance],
            'new_value' => ['finance_status' => Transaction::FINANCE_CORRECTED],
            'reason' => $validated['reason'],
        ]);

        // Create correction transaction (linked)
        $correction = Transaction::create([
            'user_id' => $user->id,
            'project_id' => $transaction->project_id,
            'project_uuid' => $transaction->project_uuid,
            'user_uuid' => $user->uuid,
            'type' => $transaction->type,
            'date' => $transaction->date,
            'description' => $validated['description'] ?? $transaction->description,
            'reported_amount' => $validated['reported_amount'],
            'real_amount' => $validated['real_amount'],
            'approval_status' => Transaction::APPROVAL_APPROVED,
            'finance_status' => Transaction::FINANCE_ACTIVE,
            'note' => 'Correction of '.$transaction->uuid.': '.$validated['reason'],
        ]);

        AuditEvent::create([
            'user_id' => $user->id,
            'entity_type' => 'transaction',
            'entity_uuid' => $correction->uuid,
            'action' => 'correction_create',
            'new_value' => $correction->toArray(),
            'reason' => $validated['reason'],
        ]);

        return response()->json([
            'original' => $this->transactionResponse($transaction->fresh()),
            'correction' => $this->transactionResponse($correction),
            'message' => 'Correction created.',
        ], 201);
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private function authorizeCreate(Request $request): void
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FIELD_ENGINEER'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER, ADMIN, or FIELD_ENGINEER can create transactions.'],
            ]);
        }
    }

    private function transactionResponse(Transaction $tx): array
    {
        return [
            'id' => $tx->id,
            'uuid' => $tx->uuid,
            'type' => $tx->type,
            'date' => $tx->date?->format('Y-m-d'),
            'description' => $tx->description,
            'reported_amount' => $tx->reported_amount,
            'real_amount' => $tx->real_amount,
            'note' => $tx->note,
            'source_text' => $tx->source_text,
            'approval_status' => $tx->approval_status,
            'finance_status' => $tx->finance_status,
            'sync_status' => $tx->sync_status,
            'project_uuid' => $tx->project_uuid,
            'project_name' => $tx->project?->name,
            'user_uuid' => $tx->user_uuid,
            'account_uuid' => $tx->account?->uuid,
            'account_name' => $tx->account?->name,
            'category_uuid' => $tx->category?->uuid,
            'category_name' => $tx->category?->name,
            'created_at' => $tx->created_at,
            'updated_at' => $tx->updated_at,
        ];
    }
}
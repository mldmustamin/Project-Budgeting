<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreTransactionRequest;
use App\Models\Project;
use App\Models\Transaction;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
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
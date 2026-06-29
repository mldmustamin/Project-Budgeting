<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\ExpenseItem;
use App\Models\MasterLocation;
use App\Models\TaskExpense;
use App\Models\TaskExpenseHistory;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

class TaskExpenseController extends Controller
{
    // ===== CRUD =====

    /**
     * List task expenses (scoped per role).
     * GET /api/task-expenses
     */
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();
        $query = TaskExpense::with(['items', 'submittedBy:id,name', 'location:id,remote_name']);

        // Role-based scoping
        if ($user->hasRole('FIELD_ENGINEER')) {
            $query->where('submitted_by', $user->id);
        } elseif ($user->hasRole('SUPERVISOR')) {
            $query->where(function ($q) use ($user) {
                $q->where('forwarded_by', $user->id)
                  ->orWhereIn('stage', ['ESTIMASI']); // Inbox
            });
        } elseif ($user->hasRole('OWNER')) {
            $query->whereIn('stage', ['FORWARDED', 'APPROVED', 'REALISASI', 'VERIFIED', 'RECONCILED']);
        } elseif ($user->hasRole('ADMIN') || $user->hasRole('FINANCE_MANAGER')) {
            $query->whereIn('stage', ['REALISASI', 'VERIFIED', 'RECONCILED']);
        }
        // AUDITOR can see all

        if ($stage = $request->get('stage')) {
            $query->where('stage', $stage);
        }

        if ($projectId = $request->get('project_id')) {
            $query->where('project_id', $projectId);
        }

        $tasks = $query->orderBy('created_at', 'desc')->paginate(config('budget.pagination_per_page', 20));

        return response()->json($tasks);
    }

    /**
     * Get task expense detail with items and histories.
     * GET /api/task-expenses/{taskExpense}
     */
    public function show(TaskExpense $taskExpense): JsonResponse
    {
        $taskExpense->load([
            'items.template:id,category_name,pagu_type,pagu_amount,requires_bill',
            'histories.actor:id,name',
            'submittedBy:id,name',
            'forwardedBy:id,name',
            'approvedBy:id,name',
            'verifiedBy:id,name',
            'reconciledBy:id,name',
            'location:id,remote_name,address,provinsi,kota_kab',
            'project:id,name',
            'laporanPekerjaan.perangkatTerpasang',
            'laporanPekerjaan.perangkatRusak',
            'laporanPekerjaan.fotos',
        ]);

        return response()->json(['data' => $taskExpense]);
    }

    /**
     * Create draft task expense. (FIELD_ENGINEER only)
     * POST /api/task-expenses
     */
    public function store(Request $request): JsonResponse
    {
        $user = $request->user();

        // Max 5 drafts enforcement
        $maxDrafts = config('budget.max_drafts_per_user', 5);
        if (TaskExpense::draftCountForUser($user) >= $maxDrafts) {
            return response()->json([
                'message' => "Maksimal {$maxDrafts} draft. Harap hapus atau selesaikan draft yang ada.",
            ], 422);
        }

        $validated = $request->validate([
            'project_id' => ['required', 'exists:projects,id'],
            'location_id' => ['nullable', 'exists:master_locations,id'],
            'task_no' => ['required', 'string', 'max:50'],
            'vid' => ['required', 'string', 'max:50'],
            'task_name' => ['nullable', 'string', 'max:500'],
            'job_type' => ['required', 'string', 'in:INSTALASI,RELOKASI,PMCM,DISMANTLE,SURVEY'],
            'notes' => ['nullable', 'string'],
            'deadline_at' => ['nullable', 'date'],
            'items' => ['nullable', 'array'],
            'items.*.template_id' => ['nullable', 'exists:budget_item_templates,id'],
            'items.*.tanggal' => ['nullable', 'date'],
            'items.*.estimated_amount' => ['nullable', 'integer', 'min:0'],
            'items.*.note' => ['nullable', 'string'],
        ]);

        // Auto-fill remote_name from location
        $remoteName = null;
        if (!empty($validated['location_id'])) {
            $location = MasterLocation::find($validated['location_id']);
            $remoteName = $location?->remote_name;
        }

        $task = DB::transaction(function () use ($validated, $user, $remoteName) {
            $task = TaskExpense::create([
                'uuid' => (string) Str::uuid(),
                'project_id' => $validated['project_id'],
                'location_id' => $validated['location_id'] ?? null,
                'task_no' => $validated['task_no'],
                'vid' => $validated['vid'],
                'task_name' => $validated['task_name'] ?? null,
                'remote_name' => $remoteName,
                'job_type' => $validated['job_type'],
                'stage' => TaskExpense::STAGE_DRAFT,
                'submitted_by' => $user->id,
                'notes' => $validated['notes'] ?? null,
                'deadline_at' => $validated['deadline_at'] ?? null,
            ]);

            // Create expense items
            if (!empty($validated['items'])) {
                foreach ($validated['items'] as $index => $itemData) {
                    ExpenseItem::create([
                        'uuid' => (string) Str::uuid(),
                        'task_expense_id' => $task->id,
                        'template_id' => $itemData['template_id'] ?? null,
                        'tanggal' => $itemData['tanggal'] ?? now(),
                        'estimated_amount' => $itemData['estimated_amount'] ?? 0,
                        'note' => $itemData['note'] ?? null,
                        'sort_order' => $index,
                    ]);
                }
                $task->recalculateTotals();
            }

            return $task;
        });

        $task->load('items.template');
        return response()->json(['data' => $task], 201);
    }

    /**
     * Update draft task expense. (FE only, stage=DRAFT)
     * PUT /api/task-expenses/{taskExpense}
     */
    public function update(Request $request, TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_DRAFT) {
            return response()->json(['message' => 'Hanya draft yang bisa diedit'], 422);
        }
        if ($taskExpense->submitted_by !== $request->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $validated = $request->validate([
            'task_name' => ['nullable', 'string', 'max:500'],
            'notes' => ['nullable', 'string'],
            'deadline_at' => ['nullable', 'date'],
            'items' => ['nullable', 'array'],
            'items.*.id' => ['nullable', 'exists:expense_items,id'],
            'items.*.template_id' => ['nullable', 'exists:budget_item_templates,id'],
            'items.*.tanggal' => ['nullable', 'date'],
            'items.*.estimated_amount' => ['nullable', 'integer', 'min:0'],
            'items.*.note' => ['nullable', 'string'],
        ]);

        DB::transaction(function () use ($validated, $taskExpense) {
            $taskExpense->update([
                'task_name' => $validated['task_name'] ?? $taskExpense->task_name,
                'notes' => $validated['notes'] ?? $taskExpense->notes,
                'deadline_at' => $validated['deadline_at'] ?? $taskExpense->deadline_at,
            ]);

            if (isset($validated['items'])) {
                // Delete removed items
                $keptIds = collect($validated['items'])->pluck('id')->filter();
                ExpenseItem::where('task_expense_id', $taskExpense->id)
                    ->whereNotIn('id', $keptIds)
                    ->delete();

                foreach ($validated['items'] as $index => $itemData) {
                    if (!empty($itemData['id'])) {
                        ExpenseItem::where('id', $itemData['id'])
                            ->where('task_expense_id', $taskExpense->id)
                            ->update([
                                'template_id' => $itemData['template_id'] ?? null,
                                'tanggal' => $itemData['tanggal'],
                                'estimated_amount' => $itemData['estimated_amount'] ?? 0,
                                'note' => $itemData['note'] ?? null,
                                'sort_order' => $index,
                            ]);
                    } else {
                        ExpenseItem::create([
                            'uuid' => (string) Str::uuid(),
                            'task_expense_id' => $taskExpense->id,
                            'template_id' => $itemData['template_id'] ?? null,
                            'tanggal' => $itemData['tanggal'] ?? now(),
                            'estimated_amount' => $itemData['estimated_amount'] ?? 0,
                            'note' => $itemData['note'] ?? null,
                            'sort_order' => $index,
                        ]);
                    }
                }
                $taskExpense->recalculateTotals();
            }
        });

        $taskExpense->load('items.template');
        return response()->json(['data' => $taskExpense]);
    }

    /**
     * Delete draft task expense. (FE only, stage=DRAFT)
     * DELETE /api/task-expenses/{taskExpense}
     */
    public function destroy(TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_DRAFT) {
            return response()->json(['message' => 'Hanya draft yang bisa dihapus'], 422);
        }
        if ($taskExpense->submitted_by !== request()->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $taskExpense->delete();
        return response()->json(['message' => 'Task deleted']);
    }

    // ===== STAGE TRANSITIONS =====

    /**
     * FE submits estimate → ESTIMASI
     * POST /api/task-expenses/{taskExpense}/submit
     */
    public function submit(TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_DRAFT) {
            return response()->json(['message' => 'Hanya draft yang bisa disubmit'], 422);
        }
        if ($taskExpense->submitted_by !== request()->user()->id) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $this->transition($taskExpense, TaskExpense::STAGE_ESTIMASI, 'submitted');
        return response()->json(['data' => $taskExpense]);
    }

    /**
     * SUPERVISOR forwards to OWNER → FORWARDED
     * POST /api/task-expenses/{taskExpense}/forward
     */
    public function forward(Request $request, TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_ESTIMASI) {
            return response()->json(['message' => 'Hanya task status ESTIMASI yang bisa diforward'], 422);
        }

        $user = $request->user();

        // SUPERVISOR may edit items before forwarding
        $validated = $request->validate([
            'notes' => ['nullable', 'string'],
            'items' => ['nullable', 'array'],
            'items.*.id' => ['required', 'exists:expense_items,id'],
            'items.*.revised_amount' => ['nullable', 'integer', 'min:0'],
        ]);

        DB::transaction(function () use ($validated, $taskExpense, $user) {
            // Apply SUPERVISOR revisions
            if (!empty($validated['items'])) {
                $revisionSummary = [];
                foreach ($validated['items'] as $itemData) {
                    $item = ExpenseItem::where('id', $itemData['id'])
                        ->where('task_expense_id', $taskExpense->id)
                        ->first();
                    if ($item && ($itemData['revised_amount'] ?? null) !== null) {
                        if ($item->estimated_amount !== (int)$itemData['revised_amount']) {
                            $revisionSummary[] = "{$item->template?->category_name}: {$item->estimated_amount} → {$itemData['revised_amount']}";
                        }
                        $item->update(['revised_amount' => $itemData['revised_amount']]);
                    }
                }
                $taskExpense->recalculateTotals();
            }

            $taskExpense->update([
                'stage' => TaskExpense::STAGE_FORWARDED,
                'forwarded_by' => $user->id,
                'notes' => $validated['notes'] ?? $taskExpense->notes,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $taskExpense->id,
                'actor_id' => $user->id,
                'action' => 'forwarded',
                'old_stage' => TaskExpense::STAGE_ESTIMASI,
                'new_stage' => TaskExpense::STAGE_FORWARDED,
                'notes' => $validated['notes'] ?? null,
            ]);
        });

        $taskExpense->load('items.template');
        return response()->json(['data' => $taskExpense]);
    }

    /**
     * OWNER approves with final amounts → APPROVED
     * POST /api/task-expenses/{taskExpense}/approve
     */
    public function approve(Request $request, TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_FORWARDED) {
            return response()->json(['message' => 'Hanya task status FORWARDED yang bisa diapprove'], 422);
        }

        $validated = $request->validate([
            'notes' => ['nullable', 'string'],
            'items' => ['required', 'array'],
            'items.*.id' => ['required', 'exists:expense_items,id'],
            'items.*.approved_amount' => ['nullable', 'integer', 'min:0'],
            'items.*.item_status' => ['nullable', 'in:APPROVED,REJECTED'],
            'items.*.rejection_reason' => ['nullable', 'string'],
        ]);

        DB::transaction(function () use ($validated, $taskExpense) {
            $user = request()->user();

            foreach ($validated['items'] as $itemData) {
                $item = ExpenseItem::where('id', $itemData['id'])
                    ->where('task_expense_id', $taskExpense->id)
                    ->first();

                if ($item) {
                    $status = $itemData['item_status'] ?? 'APPROVED';
                    $item->update([
                        'approved_amount' => $itemData['approved_amount'] ?? ($item->revised_amount ?? $item->estimated_amount),
                        'item_status' => $status,
                        'rejection_reason' => $itemData['rejection_reason'] ?? null,
                    ]);
                }
            }
            $taskExpense->recalculateTotals();

            $taskExpense->update([
                'stage' => TaskExpense::STAGE_APPROVED,
                'approved_by' => $user->id,
                'notes' => $validated['notes'] ?? $taskExpense->notes,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $taskExpense->id,
                'actor_id' => $user->id,
                'action' => 'approved',
                'old_stage' => TaskExpense::STAGE_FORWARDED,
                'new_stage' => TaskExpense::STAGE_APPROVED,
                'notes' => $validated['notes'] ?? null,
            ]);
        });

        $taskExpense->load('items.template');
        return response()->json(['data' => $taskExpense]);
    }

    /**
     * Reject and cascade back to DRAFT.
     * POST /api/task-expenses/{taskExpense}/reject
     */
    public function reject(Request $request, TaskExpense $taskExpense): JsonResponse
    {
        $allowedStages = config('budget.rejectable_stages', ['ESTIMASI', 'FORWARDED']);
        if (!in_array($taskExpense->stage, $allowedStages)) {
            return response()->json(['message' => 'Hanya task ESTIMASI atau FORWARDED yang bisa direject'], 422);
        }

        $validated = $request->validate([
            'rejection_reason' => ['required', 'string', 'max:1000'],
        ]);

        $oldStage = $taskExpense->stage;

        DB::transaction(function () use ($validated, $taskExpense, $oldStage) {
            $user = request()->user();

            // Reset SUPERVISOR revisions and OWNER approvals
            ExpenseItem::where('task_expense_id', $taskExpense->id)->update([
                'revised_amount' => null,
                'approved_amount' => null,
                'item_status' => 'DRAFT',
                'rejection_reason' => null,
            ]);

            $taskExpense->update([
                'stage' => TaskExpense::STAGE_DRAFT,
                'forwarded_by' => null,
                'approved_by' => null,
                'rejection_reason' => $validated['rejection_reason'],
                'total_revised' => 0,
                'total_approved' => 0,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $taskExpense->id,
                'actor_id' => $user->id,
                'action' => 'rejected',
                'old_stage' => $oldStage,
                'new_stage' => TaskExpense::STAGE_DRAFT,
                'notes' => $validated['rejection_reason'],
            ]);
        });

        $taskExpense->load('items.template', 'histories.actor');
        return response()->json(['data' => $taskExpense]);
    }

    /**
     * FE submits realization → REALISASI
     * POST /api/task-expenses/{taskExpense}/realize
     */
    public function realize(Request $request, TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_APPROVED) {
            return response()->json(['message' => 'Hanya task APPROVED yang bisa direalisasi'], 422);
        }
        if ($taskExpense->submitted_by !== $request->user()->id) {
            return response()->json(['message' => 'Hanya FIELD_ENGINEER yang bisa input realisasi'], 403);
        }

        $validated = $request->validate([
            'notes' => ['nullable', 'string'],
            'items' => ['required', 'array'],
            'items.*.id' => ['required', 'exists:expense_items,id'],
            'items.*.realization_amount' => ['required', 'integer', 'min:0'],
            'items.*.bukti_path' => ['nullable', 'string'],
            'items.*.note' => ['nullable', 'string'],
        ]);

        DB::transaction(function () use ($validated, $taskExpense) {
            $user = request()->user();

            foreach ($validated['items'] as $itemData) {
                $item = ExpenseItem::where('id', $itemData['id'])
                    ->where('task_expense_id', $taskExpense->id)
                    ->first();

                if ($item) {
                    $item->update([
                        'realization_amount' => $itemData['realization_amount'],
                        'bukti_path' => $itemData['bukti_path'] ?? $item->bukti_path,
                        'note' => $itemData['note'] ?? $item->note,
                    ]);
                }
            }
            $taskExpense->recalculateTotals();

            $taskExpense->update([
                'stage' => TaskExpense::STAGE_REALISASI,
                'notes' => $validated['notes'] ?? $taskExpense->notes,
                'completed_at' => now(),
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $taskExpense->id,
                'actor_id' => $user->id,
                'action' => 'realized',
                'old_stage' => TaskExpense::STAGE_APPROVED,
                'new_stage' => TaskExpense::STAGE_REALISASI,
                'notes' => $validated['notes'] ?? null,
            ]);
        });

        $taskExpense->load('items.template');
        return response()->json(['data' => $taskExpense]);
    }

    /**
     * ADMIN/FINANCE verifies → VERIFIED
     * POST /api/task-expenses/{taskExpense}/verify
     */
    public function verify(Request $request, TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_REALISASI) {
            return response()->json(['message' => 'Hanya task REALISASI yang bisa diverifikasi'], 422);
        }

        $validated = $request->validate([
            'notes' => ['nullable', 'string'],
            'items' => ['nullable', 'array'],
            'items.*.id' => ['required', 'exists:expense_items,id'],
            'items.*.bill_verified' => ['nullable', 'boolean'],
        ]);

        DB::transaction(function () use ($validated, $taskExpense) {
            $user = request()->user();

            if (!empty($validated['items'])) {
                foreach ($validated['items'] as $itemData) {
                    $item = ExpenseItem::where('id', $itemData['id'])
                        ->where('task_expense_id', $taskExpense->id)
                        ->first();
                    if ($item && isset($itemData['bill_verified'])) {
                        $item->update(['bill_verified' => $itemData['bill_verified']]);
                    }
                }
            }

            $taskExpense->update([
                'stage' => TaskExpense::STAGE_VERIFIED,
                'verified_by' => $user->id,
                'notes' => $validated['notes'] ?? $taskExpense->notes,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $taskExpense->id,
                'actor_id' => $user->id,
                'action' => 'verified',
                'old_stage' => TaskExpense::STAGE_REALISASI,
                'new_stage' => TaskExpense::STAGE_VERIFIED,
                'notes' => $validated['notes'] ?? null,
            ]);
        });

        return response()->json(['data' => $taskExpense]);
    }

    /**
     * FINANCE reconciles → RECONCILED
     * POST /api/task-expenses/{taskExpense}/reconcile
     */
    public function reconcile(Request $request, TaskExpense $taskExpense): JsonResponse
    {
        if ($taskExpense->stage !== TaskExpense::STAGE_VERIFIED) {
            return response()->json(['message' => 'Hanya task VERIFIED yang bisa direkonsiliasi'], 422);
        }

        $validated = $request->validate([
            'notes' => ['nullable', 'string'],
            'items' => ['nullable', 'array'],
            'items.*.id' => ['required', 'exists:expense_items,id'],
            'items.*.final_amount' => ['nullable', 'integer', 'min:0'], // Finance adjusts tiket without bukti
        ]);

        DB::transaction(function () use ($validated, $taskExpense) {
            $user = request()->user();

            // If finance adjusts amounts (tiket tanpa bukti)
            if (!empty($validated['items'])) {
                foreach ($validated['items'] as $itemData) {
                    if (isset($itemData['final_amount'])) {
                        $item = ExpenseItem::where('id', $itemData['id'])
                            ->where('task_expense_id', $taskExpense->id)
                            ->first();
                        if ($item) {
                            $item->update(['realization_amount' => $itemData['final_amount']]);
                        }
                    }
                }
                $taskExpense->recalculateTotals();
            }

            $taskExpense->update([
                'stage' => TaskExpense::STAGE_RECONCILED,
                'reconciled_by' => $user->id,
                'notes' => $validated['notes'] ?? $taskExpense->notes,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $taskExpense->id,
                'actor_id' => $user->id,
                'action' => 'reconciled',
                'old_stage' => TaskExpense::STAGE_VERIFIED,
                'new_stage' => TaskExpense::STAGE_RECONCILED,
                'notes' => $validated['notes'] ?? null,
            ]);
        });

        return response()->json(['data' => $taskExpense]);
    }

    /**
     * Get audit history for a task expense.
     * GET /api/task-expenses/{taskExpense}/histories
     */
    public function histories(TaskExpense $taskExpense): JsonResponse
    {
        $histories = $taskExpense->histories()
            ->with('actor:id,name')
            ->orderBy('created_at', 'desc')
            ->get();

        return response()->json(['data' => $histories]);
    }

    // ===== PRIVATE HELPERS =====

    private function transition(TaskExpense $taskExpense, string $newStage, string $action): void
    {
        DB::transaction(function () use ($taskExpense, $newStage, $action) {
            $oldStage = $taskExpense->stage;
            $taskExpense->update(['stage' => $newStage]);

            TaskExpenseHistory::create([
                'task_expense_id' => $taskExpense->id,
                'actor_id' => request()->user()->id,
                'action' => $action,
                'old_stage' => $oldStage,
                'new_stage' => $newStage,
            ]);
        });
    }
}

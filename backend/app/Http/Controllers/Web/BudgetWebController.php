<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\TaskExpense;
use App\Models\TaskExpenseHistory;
use App\Models\ExpenseItem;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\View\View;

class BudgetWebController extends Controller
{
    // ──────────────────────────────────────
    // 1. SUPERVISOR INBOX — ESTIMASI tasks
    // ──────────────────────────────────────

    public function inbox(Request $request): View
    {
        $user = $request->user();

        if (! $user->hasRole('SUPERVISOR')) {
            abort(403, 'Akses hanya untuk Supervisor.');
        }

        $tasks = TaskExpense::with(['submittedBy:id,name', 'project:id,name'])
            ->where('stage', TaskExpense::STAGE_ESTIMASI)
            ->whereNull('deleted_at')
            ->orderByDesc('created_at')
            ->paginate(25);

        return view('web.budget.inbox', compact('tasks'));
    }

    public function forward(TaskExpense $task, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole('SUPERVISOR')) {
            return back()->with('error', 'Anda tidak memiliki izin untuk meneruskan estimasi.');
        }

        if ($task->stage !== TaskExpense::STAGE_ESTIMASI) {
            return back()->with('error', 'Hanya task dengan stage ESTIMASI yang dapat diteruskan.');
        }

        $validated = $request->validate([
            'revised_amount' => ['nullable', 'integer', 'min:0'],
            'notes'          => ['nullable', 'string', 'max:2000'],
        ]);

        DB::transaction(function () use ($task, $user, $validated) {
            $oldStage = $task->stage;

            $task->update([
                'stage'        => TaskExpense::STAGE_FORWARDED,
                'forwarded_by' => $user->id,
                'total_revised'=> $validated['revised_amount'] ?? $task->total_estimated,
                'notes'        => $validated['notes'] ?? $task->notes,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $task->id,
                'actor_id'        => $user->id,
                'action'          => 'forward',
                'old_stage'       => $oldStage,
                'new_stage'       => TaskExpense::STAGE_FORWARDED,
                'notes'           => $validated['notes'] ?? null,
            ]);
        });

        return back()->with('success', "Estimasi {$task->task_no} telah diteruskan ke Owner.");
    }

    public function reject(TaskExpense $task, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['SUPERVISOR', 'OWNER'])) {
            return back()->with('error', 'Anda tidak memiliki izin untuk menolak estimasi.');
        }

        $validated = $request->validate([
            'reason' => ['required', 'string', 'max:2000'],
        ]);

        DB::transaction(function () use ($task, $user, $validated) {
            $oldStage = $task->stage;

            $task->update([
                'stage'            => TaskExpense::STAGE_DRAFT,
                'rejection_reason' => $validated['reason'],
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $task->id,
                'actor_id'        => $user->id,
                'action'          => 'reject',
                'old_stage'       => $oldStage,
                'new_stage'       => TaskExpense::STAGE_DRAFT,
                'notes'           => $validated['reason'],
            ]);
        });

        return back()->with('success', "Estimasi {$task->task_no} telah ditolak dan dikembalikan ke DRAFT.");
    }

    // ──────────────────────────────────────
    // 2. OWNER APPROVAL — FORWARDED tasks
    // ──────────────────────────────────────

    public function approvalList(Request $request): View
    {
        $user = $request->user();

        if (! $user->hasRole('OWNER')) {
            abort(403, 'Akses hanya untuk Owner.');
        }

        $tasks = TaskExpense::with([
                'submittedBy:id,name',
                'forwardedBy:id,name',
                'items' => fn ($q) => $q->orderBy('sort_order'),
            ])
            ->where('stage', TaskExpense::STAGE_FORWARDED)
            ->whereNull('deleted_at')
            ->orderByDesc('created_at')
            ->paginate(25);

        return view('web.budget.approval', compact('tasks'));
    }

    public function approve(TaskExpense $task, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole('OWNER')) {
            return back()->with('error', 'Anda tidak memiliki izin untuk menyetujui estimasi.');
        }

        if ($task->stage !== TaskExpense::STAGE_FORWARDED) {
            return back()->with('error', 'Hanya task dengan stage FORWARDED yang dapat disetujui.');
        }

        // Validate per-item approved amounts
        $validated = $request->validate([
            'approved_amounts'             => ['required', 'array'],
            'approved_amounts.*'           => ['required', 'integer', 'min:0'],
        ]);

        DB::transaction(function () use ($task, $user, $validated) {
            $oldStage = $task->stage;

            // Update each item's approved_amount
            foreach ($task->items as $item) {
                if (isset($validated['approved_amounts'][$item->id])) {
                    $item->update([
                        'approved_amount' => $validated['approved_amounts'][$item->id],
                    ]);
                }
            }

            // Recalculate totals
            $task->recalculateTotals();

            $task->update([
                'stage'       => TaskExpense::STAGE_APPROVED,
                'approved_by' => $user->id,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $task->id,
                'actor_id'        => $user->id,
                'action'          => 'approve',
                'old_stage'       => $oldStage,
                'new_stage'       => TaskExpense::STAGE_APPROVED,
                'metadata'        => ['approved_amounts' => $validated['approved_amounts']],
            ]);
        });

        return back()->with('success', "Estimasi {$task->task_no} telah disetujui.");
    }

    // ───────────────────────────────────────────
    // 3. FINANCE VERIFICATION — REALISASI tasks
    // ───────────────────────────────────────────

    public function verificationList(Request $request): View
    {
        $user = $request->user();

        if (! $user->hasRole(['ADMIN', 'FINANCE_MANAGER'])) {
            abort(403, 'Akses hanya untuk Admin / Finance Manager.');
        }

        $tasks = TaskExpense::with([
                'items' => fn ($q) => $q->orderBy('sort_order'),
            ])
            ->where('stage', TaskExpense::STAGE_REALISASI)
            ->whereNull('deleted_at')
            ->orderByDesc('created_at')
            ->paginate(25);

        return view('web.budget.verification', compact('tasks'));
    }

    public function verify(TaskExpense $task, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['ADMIN', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Anda tidak memiliki izin untuk memverifikasi.');
        }

        if ($task->stage !== TaskExpense::STAGE_REALISASI) {
            return back()->with('error', 'Hanya task dengan stage REALISASI yang dapat diverifikasi.');
        }

        $validated = $request->validate([
            'bill_verified'     => ['required', 'array'],
            'bill_verified.*'   => ['boolean'],
        ]);

        DB::transaction(function () use ($task, $user, $validated) {
            $oldStage = $task->stage;

            // Mark items as bill_verified
            foreach ($task->items as $item) {
                $verified = ! empty($validated['bill_verified'][$item->id]);
                $item->update(['bill_verified' => $verified]);
            }

            $task->update([
                'stage'       => TaskExpense::STAGE_VERIFIED,
                'verified_by' => $user->id,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $task->id,
                'actor_id'        => $user->id,
                'action'          => 'verify',
                'old_stage'       => $oldStage,
                'new_stage'       => TaskExpense::STAGE_VERIFIED,
                'metadata'        => ['bill_verified' => $validated['bill_verified']],
            ]);
        });

        return back()->with('success', "Realisasi {$task->task_no} telah diverifikasi.");
    }

    public function reconcile(TaskExpense $task, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole('FINANCE_MANAGER')) {
            return back()->with('error', 'Hanya Finance Manager yang dapat melakukan rekonsiliasi.');
        }

        if ($task->stage !== TaskExpense::STAGE_VERIFIED) {
            return back()->with('error', 'Hanya task dengan stage VERIFIED yang dapat direkonsiliasi.');
        }

        DB::transaction(function () use ($task, $user) {
            $oldStage = $task->stage;

            $task->update([
                'stage'          => TaskExpense::STAGE_RECONCILED,
                'reconciled_by'  => $user->id,
            ]);

            TaskExpenseHistory::create([
                'task_expense_id' => $task->id,
                'actor_id'        => $user->id,
                'action'          => 'reconcile',
                'old_stage'       => $oldStage,
                'new_stage'       => TaskExpense::STAGE_RECONCILED,
            ]);
        });

        return back()->with('success', "Task {$task->task_no} telah direkonsiliasi.");
    }
}

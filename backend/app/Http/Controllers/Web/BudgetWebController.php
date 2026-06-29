<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\TaskExpense;
use App\Models\TaskExpenseHistory;
use App\Models\ExpenseItem;
use App\Models\BudgetItemTemplate;
use App\Models\MasterLocation;
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

    // ──────────────────────────────────────
    // 4. FIELD ENGINEER — Budget Estimate Form
    // ──────────────────────────────────────

    public function create(Request $request): View
    {
        $user = $request->user();

        if (! $user->hasRole('FIELD_ENGINEER')) {
            abort(403, 'Akses hanya untuk Field Engineer.');
        }

        $draftCount = TaskExpense::draftCountForUser($user);
        $locations = MasterLocation::orderBy('remote_name')->get(['id', 'remote_name', 'project_id']);
        $templates = BudgetItemTemplate::active()->with('paguAmounts')->get();

        return view('web.budget.create', compact('draftCount', 'locations', 'templates'));
    }

    public function store(Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole('FIELD_ENGINEER')) {
            return back()->with('error', 'Akses hanya untuk Field Engineer.');
        }

        if (TaskExpense::draftCountForUser($user) >= 5) {
            return back()->with('error', 'Maksimal 5 draft per user. Harap submit atau hapus draft yang ada.');
        }

        $validated = $request->validate([
            'task_no'                    => ['required', 'string', 'max:50'],
            'vid'                        => ['required', 'string', 'max:50'],
            'job_type'                   => ['required', 'in:INSTALASI,RELOKASI,PMCM,DISMANTLE,SURVEY'],
            'location_id'                => ['required', 'exists:master_locations,id'],
            'items'                      => ['required', 'array', 'min:1'],
            'items.*.template_id'        => ['required', 'exists:budget_item_templates,id'],
            'items.*.estimated_amount'   => ['required', 'integer', 'min:0'],
            'items.*.tanggal'            => ['required', 'date'],
        ]);

        $isDraft = $request->input('action') === 'draft';

        $task = DB::transaction(function () use ($validated, $user, $isDraft) {
            $task = TaskExpense::create([
                'task_no'       => $validated['task_no'],
                'vid'           => $validated['vid'],
                'job_type'      => $validated['job_type'],
                'location_id'   => $validated['location_id'],
                'submitted_by'  => $user->id,
                'stage'         => $isDraft ? TaskExpense::STAGE_DRAFT : TaskExpense::STAGE_ESTIMASI,
            ]);

            foreach ($validated['items'] as $i => $item) {
                ExpenseItem::create([
                    'task_expense_id'  => $task->id,
                    'template_id'      => $item['template_id'],
                    'estimated_amount' => $item['estimated_amount'],
                    'tanggal'          => $item['tanggal'],
                    'sort_order'       => $i,
                ]);
            }

            $task->recalculateTotals();

            TaskExpenseHistory::create([
                'task_expense_id' => $task->id,
                'actor_id'        => $user->id,
                'action'          => $isDraft ? 'draft' : 'submit',
                'old_stage'       => null,
                'new_stage'       => $task->stage,
            ]);

            return $task;
        });

        $msg = $isDraft
            ? "Draft {$task->task_no} berhasil disimpan."
            : "Estimasi {$task->task_no} berhasil dikirim ke Kordinator.";

        return redirect()->route('web.budget.index')->with('success', $msg);
    }

    public function edit(TaskExpense $task, Request $request): View
    {
        $user = $request->user();

        if (! $user->hasRole('FIELD_ENGINEER')) {
            abort(403, 'Akses hanya untuk Field Engineer.');
        }

        if ($task->submitted_by !== $user->id) {
            abort(403, 'Anda hanya dapat mengedit draft Anda sendiri.');
        }

        if (! in_array($task->stage, [TaskExpense::STAGE_DRAFT, TaskExpense::STAGE_ESTIMASI])) {
            abort(403, 'Hanya draft / estimasi yang dapat diedit.');
        }

        $task->load(['items' => fn ($q) => $q->orderBy('sort_order'), 'items.template']);
        $locations = MasterLocation::orderBy('remote_name')->get(['id', 'remote_name', 'project_id']);
        $templates = BudgetItemTemplate::active()->with('paguAmounts')->get();

        return view('web.budget.create', compact('task', 'locations', 'templates'));
    }

    // ──────────────────────────────────────
    // 5. FIELD ENGINEER — Realization Form
    // ──────────────────────────────────────

    public function realize(TaskExpense $task, Request $request): View
    {
        $user = $request->user();

        if (! $user->hasRole('FIELD_ENGINEER')) {
            abort(403, 'Akses hanya untuk Field Engineer.');
        }

        if ($task->stage !== TaskExpense::STAGE_APPROVED) {
            abort(403, 'Hanya budget yang sudah APPROVED yang dapat direalisasikan.');
        }

        $task->load(['items' => fn ($q) => $q->orderBy('sort_order'), 'items.template']);

        return view('web.budget.realize', compact('task'));
    }

    public function storeRealization(TaskExpense $task, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole('FIELD_ENGINEER')) {
            return back()->with('error', 'Akses hanya untuk Field Engineer.');
        }

        if ($task->stage !== TaskExpense::STAGE_APPROVED) {
            return back()->with('error', 'Hanya budget yang sudah APPROVED yang dapat direalisasikan.');
        }

        $validated = $request->validate([
            'items'                       => ['required', 'array'],
            'items.*.id'                  => ['required', 'exists:expense_items,id'],
            'items.*.realization_amount'  => ['required', 'integer', 'min:0'],
            'items.*.note'                => ['nullable', 'string', 'max:500'],
        ]);

        DB::transaction(function () use ($task, $user, $validated) {
            $oldStage = $task->stage;

            foreach ($validated['items'] as $itemData) {
                $item = ExpenseItem::find($itemData['id']);
                if ($item && $item->task_expense_id === $task->id) {
                    $item->update([
                        'realization_amount' => $itemData['realization_amount'],
                        'note'               => $itemData['note'] ?? null,
                    ]);
                }
            }

            $task->recalculateTotals();
            $task->update(['stage' => TaskExpense::STAGE_REALISASI]);

            TaskExpenseHistory::create([
                'task_expense_id' => $task->id,
                'actor_id'        => $user->id,
                'action'          => 'realize',
                'old_stage'       => $oldStage,
                'new_stage'       => TaskExpense::STAGE_REALISASI,
                'metadata'        => [
                    'total_realization' => $task->total_realization,
                ],
            ]);
        });

        return redirect()->route('web.budget.index')->with('success', "Realisasi {$task->task_no} berhasil disimpan.");
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

    // === FIELD ENGINEER — Laporan Pekerjaan ===

    public function laporanForm(): View
    {
        return view('web.laporan.create');
    }

    public function storeLaporan(Request $request): RedirectResponse
    {
        $validated = $request->validate([
            'nama_teknisi' => ['nullable', 'string'], 'no_hp_teknisi' => ['nullable', 'string'],
            'koordinator' => ['nullable', 'string'], 'tgl_berangkat' => ['nullable', 'date'],
            'tgl_tiba' => ['nullable', 'date'], 'tgl_mulai' => ['nullable', 'date'],
            'tgl_selesai' => ['nullable', 'date'], 'nama_customer' => ['nullable', 'string'],
            'alamat_customer' => ['nullable', 'string'], 'pic_lokasi' => ['nullable', 'string'],
            'ip_lan' => ['nullable', 'string'], 'hub_satelite' => ['nullable', 'string'],
            'sqf_awal' => ['nullable', 'string'], 'sqf_pointing' => ['nullable', 'string'],
            'target_esno' => ['nullable', 'string'], 'signal_telkomsel' => ['nullable', 'string'],
            'signal_indosat' => ['nullable', 'string'], 'kondisi_ac' => ['nullable', 'string'],
            'sumber_elektrikal' => ['nullable', 'string'], 'tindakan_teknisi' => ['nullable', 'string'],
            'tindakan_flm' => ['nullable', 'string'], 'penyebab_gangguan' => ['nullable', 'string'],
            'catatan' => ['nullable', 'string'],
        ]);

        \App\Models\LaporanPekerjaan::create([...$validated, 'created_by' => auth()->id()]);

        return redirect()->route('web.dashboard')->with('success', 'Laporan berhasil disimpan');
    }
}

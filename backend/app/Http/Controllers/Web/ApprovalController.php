<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\AuditEvent;
use App\Models\Transaction;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Illuminate\View\View;

class ApprovalController extends Controller
{
    public function index(Request $request): View
    {
        $tab = $request->get('tab', 'pending');
        $projectFilter = function ($query) use ($request) {
            if ($request->filled('project_uuid')) {
                $query->whereHas('project', fn ($q) => $q->where('uuid', $request->project_uuid));
            }
        };

        $pendingQuery = Transaction::with(['project:id,uuid,name', 'user:id,name'])
            ->whereNull('deleted_at')
            ->where('approval_status', Transaction::APPROVAL_PENDING);
        $projectFilter($pendingQuery);

        $disputedQuery = Transaction::with(['project:id,uuid,name', 'user:id,name'])
            ->whereNull('deleted_at')
            ->where('approval_status', Transaction::APPROVAL_DISPUTED);
        $projectFilter($disputedQuery);

        $pendingTransactions = $pendingQuery->orderByDesc('date')->paginate(25, ['*'], 'pending_page');
        $disputedTransactions = $disputedQuery->orderByDesc('disputed_at')->paginate(25, ['*'], 'disputed_page');
        $projects = \App\Models\Project::orderBy('name')->get(['uuid', 'name']);

        return view('web.approval.queue', compact('pendingTransactions', 'disputedTransactions', 'projects', 'tab'));
    }

    public function approve(Transaction $transaction, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Anda tidak memiliki izin untuk menyetujui transaksi.');
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_PENDING) {
            return back()->with('error', 'Hanya transaksi PENDING yang dapat disetujui.');
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

        return back()->with('success', "Transaksi {$transaction->uuid} telah disetujui.");
    }

    public function reject(Transaction $transaction, Request $request): RedirectResponse
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Anda tidak memiliki izin untuk menolak transaksi.');
        }

        if ($transaction->approval_status !== Transaction::APPROVAL_PENDING) {
            return back()->with('error', 'Hanya transaksi PENDING yang dapat ditolak.');
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

        return back()->with('success', "Transaksi {$transaction->uuid} telah ditolak.");
    }

    public function dispute(Transaction $tx, Request $req): RedirectResponse
    {
        $user = $req->user();
        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $v = $req->validate(['disputed_amount' => 'required|integer|min:1', 'dispute_reason' => 'required|string|max:1000']);
        $tx->update(['approval_status' => Transaction::APPROVAL_DISPUTED, 'disputed_amount' => $v['disputed_amount'], 'dispute_reason' => $v['dispute_reason'], 'disputed_by' => $user->id, 'disputed_at' => now()]);
        AuditEvent::create(['user_id' => $user->id, 'entity_type' => 'transaction', 'entity_uuid' => $tx->uuid, 'action' => 'dispute', 'reason' => $v['dispute_reason']]);
        return redirect()->route('web.approval.index', ['tab' => 'disputed'])->with('success', 'Transaksi disanggah.');
    }

    public function resolveDispute(Transaction $tx, Request $req): RedirectResponse
    {
        $user = $req->user();
        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $v = $req->validate(['action' => 'required|in:accept_dispute,reject_dispute', 'resolution_note' => 'nullable|string|max:500']);
        if ($v['action'] === 'accept_dispute') {
            Transaction::create(['user_id' => $user->id, 'project_id' => $tx->project_id, 'project_uuid' => $tx->project_uuid, 'user_uuid' => $user->uuid, 'type' => $tx->type, 'date' => $tx->date, 'description' => $tx->description, 'reported_amount' => $tx->reported_amount, 'real_amount' => $tx->disputed_amount, 'approval_status' => Transaction::APPROVAL_APPROVED, 'finance_status' => Transaction::FINANCE_ACTIVE, 'note' => 'Sanggahan diterima dari '.$tx->uuid]);
            $tx->update(['approval_status' => Transaction::APPROVAL_APPROVED, 'finance_status' => Transaction::FINANCE_CORRECTED, 'dispute_resolved_by' => $user->id, 'dispute_resolved_at' => now()]);
            return back()->with('success', 'Sanggahan diterima — koreksi dibuat.');
        }
        $tx->update(['approval_status' => Transaction::APPROVAL_APPROVED, 'dispute_resolved_by' => $user->id, 'dispute_resolved_at' => now()]);
        return back()->with('success', 'Sanggahan ditolak — kembali ke APPROVED.');
    }
}

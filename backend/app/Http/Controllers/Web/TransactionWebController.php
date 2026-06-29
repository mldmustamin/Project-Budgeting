<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\Account;
use App\Models\AuditEvent;
use App\Models\Category;
use App\Models\Project;
use App\Models\Transaction;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\View\View;

class TransactionWebController extends Controller
{
    public function index(Request $request): View
    {
        $query = Transaction::with(['project:id,uuid,name', 'user:id,name'])
            ->whereNull('deleted_at');

        if ($request->filled('project_uuid')) {
            $query->whereHas('project', fn ($q) => $q->where('uuid', $request->project_uuid));
        }
        if ($request->filled('type')) {
            $query->where('type', $request->type);
        }
        if ($request->filled('approval_status')) {
            $query->where('approval_status', $request->approval_status);
        }
        if ($request->filled('date_from')) {
            $query->where('date', '>=', $request->date_from);
        }
        if ($request->filled('date_to')) {
            $query->where('date', '<=', $request->date_to);
        }

        $transactions = $query->orderByDesc('date')->orderByDesc('id')->paginate(25);
        $projects = Project::orderBy('name')->get(['uuid', 'name']);

        return view('web.transactions.index', compact('transactions', 'projects'));
    }

    public function show(Transaction $transaction): View
    {
        $transaction->load(['project:id,uuid,name', 'user:id,name', 'account:id,uuid,name', 'category:id,uuid,name']);
        return view('web.transactions.show', compact('transaction'));
    }

    public function create(): View
    {
        $projects = Project::orderBy('name')->get(['id', 'uuid', 'name']);
        $categories = Category::orderBy('name')->get(['id', 'uuid', 'name']);
        $accounts = Account::orderBy('name')->get(['id', 'uuid', 'name']);

        return view('web.transactions.form', [
            'transaction' => null,
            'projects' => $projects,
            'categories' => $categories,
            'accounts' => $accounts,
        ]);
    }

    public function store(Request $request): RedirectResponse
    {
        $validated = $request->validate([
            'project_id' => ['required', 'exists:projects,id'],
            'category_id' => ['nullable', 'exists:categories,id'],
            'account_id' => ['nullable', 'exists:accounts,id'],
            'type' => ['required', 'string', 'in:FUND_IN,OFFICE_EXPENSE,PERSONAL_EXPENSE'],
            'date' => ['required', 'date'],
            'reported_amount' => ['required', 'integer', 'min:0'],
            'real_amount' => ['nullable', 'integer', 'min:0'],
            'description' => ['nullable', 'string', 'max:500'],
            'receipt_path' => ['nullable', 'string'],
        ]);

        $transaction = Transaction::create([
            ...$validated,
            'uuid' => (string) Str::uuid(),
            'user_id' => auth()->id(),
            'approval_status' => 'DRAFT',
            'finance_status' => 'DRAFT',
        ]);

        AuditEvent::log(auth()->id(), 'create', 'transaction', $transaction->uuid, $validated);

        return redirect()->route('web.transactions.show', $transaction)
            ->with('success', 'Transaksi berhasil dibuat');
    }

    public function edit(Transaction $transaction): View
    {
        $projects = Project::orderBy('name')->get(['id', 'uuid', 'name']);
        $categories = Category::orderBy('name')->get(['id', 'uuid', 'name']);
        $accounts = Account::orderBy('name')->get(['id', 'uuid', 'name']);

        return view('web.transactions.form', compact('transaction', 'projects', 'categories', 'accounts'));
    }

    public function update(Request $request, Transaction $transaction): RedirectResponse
    {
        $validated = $request->validate([
            'project_id' => ['required', 'exists:projects,id'],
            'category_id' => ['nullable', 'exists:categories,id'],
            'account_id' => ['nullable', 'exists:accounts,id'],
            'type' => ['required', 'string', 'in:FUND_IN,OFFICE_EXPENSE,PERSONAL_EXPENSE'],
            'date' => ['required', 'date'],
            'reported_amount' => ['required', 'integer', 'min:0'],
            'real_amount' => ['nullable', 'integer', 'min:0'],
            'description' => ['nullable', 'string', 'max:500'],
            'receipt_path' => ['nullable', 'string'],
        ]);

        $transaction->update($validated);
        AuditEvent::log(auth()->id(), 'update', 'transaction', $transaction->uuid, $validated);

        return redirect()->route('web.transactions.show', $transaction)
            ->with('success', 'Transaksi berhasil diupdate');
    }

    public function destroy(Transaction $transaction): RedirectResponse
    {
        $transaction->delete();
        AuditEvent::log(auth()->id(), 'delete', 'transaction', $transaction->uuid);

        return redirect()->route('web.transactions.index')
            ->with('success', 'Transaksi berhasil dihapus');
    }
}

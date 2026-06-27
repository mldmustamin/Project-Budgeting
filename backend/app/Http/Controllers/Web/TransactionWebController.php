<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\Transaction;
use Illuminate\Http\Request;
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
        $projects = \App\Models\Project::orderBy('name')->get(['uuid', 'name']);

        return view('web.transactions.index', compact('transactions', 'projects'));
    }

    public function show(Transaction $transaction): View
    {
        $transaction->load(['project:id,uuid,name', 'user:id,name', 'account:id,uuid,name', 'category:id,uuid,name']);

        return view('web.transactions.show', compact('transaction'));
    }
}

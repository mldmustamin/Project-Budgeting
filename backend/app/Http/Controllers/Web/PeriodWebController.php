<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\AccountingPeriod;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class PeriodWebController extends Controller
{
    public function index(): View
    {
        $periods = AccountingPeriod::orderByDesc('period_start')->paginate(20);
        return view('web.periods.index', compact('periods'));
    }

    public function store(Request $request): RedirectResponse
    {
        if (! $request->user()->hasRole(['OWNER', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $v = $request->validate([
            'period_start' => 'required|date',
            'period_end' => 'required|date|after_or_equal:period_start',
        ]);
        AccountingPeriod::create($v);
        return back()->with('success', 'Periode dibuat.');
    }

    public function close(AccountingPeriod $period, Request $request): RedirectResponse
    {
        if (! $request->user()->hasRole(['OWNER', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $period->update(['status' => 'CLOSED', 'closed_by' => $request->user()->id, 'closed_at' => now()]);
        \App\Models\AuditEvent::create(['user_id' => $request->user()->id, 'entity_type' => 'accounting_period', 'entity_uuid' => $period->uuid, 'action' => 'close_period']);
        return back()->with('success', 'Periode ditutup.');
    }

    public function reopen(AccountingPeriod $period, Request $request): RedirectResponse
    {
        if (! $request->user()->hasRole(['OWNER', 'FINANCE_MANAGER'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $period->update(['status' => 'OPEN', 'reopened_by' => $request->user()->id, 'reopened_at' => now()]);
        \App\Models\AuditEvent::create(['user_id' => $request->user()->id, 'entity_type' => 'accounting_period', 'entity_uuid' => $period->uuid, 'action' => 'reopen_period']);
        return back()->with('success', 'Periode dibuka kembali.');
    }
}

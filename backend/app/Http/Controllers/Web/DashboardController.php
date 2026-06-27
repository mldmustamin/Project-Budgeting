<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\Transaction;
use App\Services\TransactionSummaryService;
use Illuminate\Http\Request;
use Illuminate\View\View;

class DashboardController extends Controller
{
    public function index(): View
    {
        $transactions = Transaction::whereNull('deleted_at')->get();
        $service = new TransactionSummaryService();
        $summary = $service->calculate($transactions);

        $totalFundIn = $summary['total_fund_in'];
        $totalOfficeReal = $summary['total_office_real'];
        $totalPersonal = $summary['total_personal_expense'];
        $totalCashOut = $summary['total_cash_out'];
        $netPosition = $summary['net_position'];

        $pendingApprovalCount = Transaction::where('approval_status', Transaction::APPROVAL_PENDING)
            ->whereNull('deleted_at')
            ->count();

        $recentAuditEvents = \App\Models\AuditEvent::with('user:id,name')
            ->orderByDesc('created_at')
            ->take(10)
            ->get();

        return view('web.dashboard', compact(
            'totalFundIn',
            'totalOfficeReal',
            'totalPersonal',
            'totalCashOut',
            'netPosition',
            'pendingApprovalCount',
            'recentAuditEvents'
        ));
    }
}

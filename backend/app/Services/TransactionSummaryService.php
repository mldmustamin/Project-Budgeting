<?php

namespace App\Services;

use App\Models\Transaction;
use Illuminate\Support\Collection;

class TransactionSummaryService
{
    /**
     * Calculate LOCAL_VIEW summary from a collection of transactions.
     * Formula matches Android CalculateProjectSummaryUseCase exactly.
     *
     * Filters: excludes soft-deleted transactions (deleted_at IS NOT NULL).
     * All math uses integer arithmetic (BIGINT compatible).
     */
    public function calculate(Collection $transactions): array
    {
        $totalFundIn = 0;
        $totalOfficeReported = 0;
        $totalOfficeReal = 0;
        $totalPersonalExpense = 0;

        foreach ($transactions as $tx) {
            // Ignore soft-deleted transactions
            if ($tx->deleted_at !== null) {
                continue;
            }

            switch ($tx->type) {
                case Transaction::TYPE_FUND_IN:
                    $totalFundIn += $tx->reported_amount;
                    break;
                case Transaction::TYPE_OFFICE_EXPENSE:
                    $totalOfficeReported += $tx->reported_amount;
                    $totalOfficeReal += $tx->real_amount;
                    break;
                case Transaction::TYPE_PERSONAL_EXPENSE:
                    $totalPersonalExpense += $tx->real_amount;
                    break;
            }
        }

        $saving = $totalOfficeReported - $totalOfficeReal;
        $remainingReported = $totalFundIn - $totalOfficeReported;
        $remainingReal = $totalFundIn - $totalOfficeReal;
        $totalCashOut = $totalOfficeReal + $totalPersonalExpense;
        $netPosition = $totalFundIn - $totalCashOut;

        return [
            'total_fund_in' => $totalFundIn,
            'total_office_reported' => $totalOfficeReported,
            'total_office_real' => $totalOfficeReal,
            'total_personal_expense' => $totalPersonalExpense,
            'saving' => $saving,
            'remaining_reported' => $remainingReported,
            'remaining_real' => $remainingReal,
            'total_cash_out' => $totalCashOut,
            'net_position' => $netPosition,
        ];
    }
}
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\BudgetItemTemplate;
use Illuminate\Http\JsonResponse;

class BudgetItemTemplateController extends Controller
{
    /**
     * Get all active budget item templates.
     * Usage: GET /api/budget-templates
     */
    public function index(): JsonResponse
    {
        $templates = BudgetItemTemplate::active()->get();

        return response()->json([
            'data' => $templates,
            'meta' => [
                'total' => $templates->count(),
                'fixed_pagu' => $templates->where('pagu_type', 'FIXED_PAGU')->count(),
                'ticket' => $templates->where('pagu_type', 'TICKET')->count(),
                'manager_approval' => $templates->where('pagu_type', 'MANAGER_APPROVAL')->count(),
            ],
        ]);
    }
}

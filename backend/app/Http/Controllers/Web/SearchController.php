<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\Project;
use App\Models\Transaction;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class SearchController extends Controller
{
    public function search(Request $request): JsonResponse
    {
        $q = $request->get('q', '');
        if (mb_strlen($q) < 2) return response()->json([]);

        $results = [];

        // Transactions
        Transaction::whereNull('deleted_at')
            ->where(function ($sql) use ($q) {
                $sql->where('description', 'like', "%{$q}%")
                    ->orWhere('uuid', 'like', "%{$q}%")
                    ->orWhere('note', 'like', "%{$q}%");
            })
            ->with('project:id,uuid,name')
            ->orderByDesc('date')
            ->take(5)
            ->each(function ($tx) use (&$results) {
                $results[] = [
                    'type' => 'transaction',
                    'label' => $tx->description ?: 'Transaksi',
                    'sub' => $tx->project?->name.' — Rp'.number_format($tx->reported_amount,0,',','.'),
                    'url' => route('web.transactions.show', $tx),
                ];
            });

        // Projects
        Project::where('name', 'like', "%{$q}%")
            ->orderBy('name')
            ->take(3)
            ->each(function ($p) use (&$results) {
                $results[] = [
                    'type' => 'project',
                    'label' => $p->name,
                    'sub' => 'Project',
                    'url' => route('web.projects.show', $p),
                ];
            });

        // Users
        User::where('name', 'like', "%{$q}%")
            ->orWhere('employee_id', 'like', "%{$q}%")
            ->orWhere('email', 'like', "%{$q}%")
            ->orderBy('name')
            ->take(3)
            ->each(function ($u) use (&$results) {
                $results[] = [
                    'type' => 'user',
                    'label' => $u->name,
                    'sub' => $u->employee_id ?? $u->email,
                    'url' => route('web.users.index'),
                ];
            });

        return response()->json($results);
    }
}

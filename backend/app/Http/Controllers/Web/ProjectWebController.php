<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\Project;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\View\View;

class ProjectWebController extends Controller
{
    public function index(): View
    {
        $projects = Project::with('user:id,name')
            ->withCount(['transactions'])
            ->orderByDesc('created_at')
            ->paginate(20);

        return view('web.projects.index', compact('projects'));
    }

    public function store(Request $request): RedirectResponse
    {
        $user = $request->user();
        if (! $user->hasRole(['OWNER', 'ADMIN'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $v = $request->validate([
            'name' => 'required|string|max:255',
            'description' => 'nullable|string|max:1000',
            'start_at' => 'nullable|date',
            'completed_at' => 'nullable|date|after_or_equal:start_at',
        ]);
        Project::create([
            'uuid' => (string) Str::uuid(),
            'user_id' => $user->id,
            'name' => $v['name'],
            'description' => $v['description'] ?? null,
            'start_at' => $v['start_at'] ?? now(),
            'completed_at' => $v['completed_at'] ?? null,
        ]);
        return back()->with('success', "Project '{$v['name']}' dibuat.");
    }

    public function update(Request $request, Project $project): RedirectResponse
    {
        $user = $request->user();
        if (! $user->hasRole(['OWNER', 'ADMIN'])) {
            return back()->with('error', 'Tidak memiliki izin.');
        }
        $v = $request->validate([
            'name' => 'required|string|max:255',
            'description' => 'nullable|string|max:1000',
            'is_archived' => 'boolean',
            'start_at' => 'nullable|date',
            'completed_at' => 'nullable|date|after_or_equal:start_at',
        ]);
        $project->update($v);
        return back()->with('success', "Project '{$v['name']}' diupdate.");
    }

    public function show(Project $project): View
    {
        $txs = $project->transactions()->whereNull('deleted_at')->orderByDesc('date')->get();
        $svc = new \App\Services\TransactionSummaryService();
        $summary = $svc->calculate($txs);
        $summary['transaction_count'] = $txs->count();

        return view('web.projects.show', compact('project', 'summary', 'txs'));
    }
}

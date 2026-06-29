<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Project;
use App\Models\ProjectAssignment;
use App\Models\Transaction;
use App\Services\TransactionSummaryService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

class ProjectController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $user = $request->user();

        if ($user->hasRole(['OWNER', 'ADMIN'])) {
            $projects = Project::with('user:id,name')->get();
        } else {
            $assignedProjectIds = ProjectAssignment::where('user_id', $user->id)
                ->pluck('project_id');
            $projects = Project::with('user:id,name')
                ->whereIn('id', $assignedProjectIds)
                ->get();
        }

        return response()->json([
            'projects' => $projects->map(fn (Project $project) => [
                'id' => $project->id,
                'uuid' => $project->uuid,
                'name' => $project->name,
                'description' => $project->description,
                'is_archived' => $project->is_archived,
                'start_at' => $project->start_at,
                'completed_at' => $project->completed_at,
                'owner_name' => $project->user?->name,
                'sync_status' => $project->sync_status,
                'created_at' => $project->created_at,
                'updated_at' => $project->updated_at,
            ]),
        ]);
    }

    public function store(Request $request): JsonResponse
    {
        $this->authorizeCreate($request);

        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'description' => 'nullable|string',
            'start_at' => 'nullable|date',
            'completed_at' => 'nullable|date|after_or_equal:start_at',
        ]);

        $project = Project::create([
            'uuid' => (string) Str::uuid(),
            'user_id' => $request->user()->id,
            'name' => $validated['name'],
            'description' => $validated['description'] ?? null,
            'start_at' => $validated['start_at'] ?? now(),
            'completed_at' => $validated['completed_at'] ?? null,
        ]);

        return response()->json([
            'project' => $this->projectResponse($project),
        ], 201);
    }

    public function update(Request $request, Project $project): JsonResponse
    {
        $this->authorizeCreate($request);

        $validated = $request->validate([
            'name' => 'sometimes|string|max:255',
            'description' => 'nullable|string',
            'is_archived' => 'sometimes|boolean',
            'start_at' => 'sometimes|date',
            'completed_at' => 'nullable|date|after_or_equal:start_at',
        ]);

        $project->update($validated);

        return response()->json([
            'project' => $this->projectResponse($project->fresh()),
        ]);
    }

    public function assign(Request $request, Project $project): JsonResponse
    {
        $this->authorizeCreate($request);

        $validated = $request->validate([
            'user_id' => ['required', 'integer', 'exists:users,id'],
            'role_on_project' => ['required', 'string', Rule::in(['PIC', 'MEMBER', 'FIELD_ENGINEER'])],
            'active_from' => ['nullable', 'date'],
            'active_until' => ['nullable', 'date', 'after:active_from'],
        ]);

        $assignment = ProjectAssignment::create([
            'uuid' => (string) Str::uuid(),
            'project_id' => $project->id,
            'user_id' => $validated['user_id'],
            'role_on_project' => $validated['role_on_project'],
            'active_from' => $validated['active_from'] ?? now(),
            'active_until' => $validated['active_until'] ?? null,
        ]);

        return response()->json([
            'assignment' => [
                'id' => $assignment->id,
                'uuid' => $assignment->uuid,
                'project_uuid' => $project->uuid,
                'user_id' => $assignment->user_id,
                'role_on_project' => $assignment->role_on_project,
                'active_from' => $assignment->active_from,
                'active_until' => $assignment->active_until,
                'created_at' => $assignment->created_at,
            ],
        ], 201);
    }

    // ─── Summary & Export ────────────────────────────────────────────────

    public function summary(Project $project): JsonResponse
    {
        $transactions = Transaction::where('project_id', $project->id)->get();
        $service = new TransactionSummaryService();
        $summary = $service->calculate($transactions);

        $summary['transaction_count'] = $transactions->whereNull('deleted_at')->count();
        $summary['project_uuid'] = $project->uuid;
        $summary['project_name'] = $project->name;

        return response()->json(['summary' => $summary]);
    }

    public function export(Project $project): JsonResponse
    {
        $transactions = Transaction::where('project_id', $project->id)
            ->whereNull('deleted_at')
            ->orderByDesc('date')
            ->orderByDesc('id')
            ->get(['uuid', 'type', 'date', 'description', 'reported_amount', 'real_amount',
                   'approval_status', 'finance_status', 'created_at']);

        $service = new TransactionSummaryService();
        $summary = $service->calculate($transactions);

        return response()->json([
            'project' => ['uuid' => $project->uuid, 'name' => $project->name],
            'summary' => $summary,
            'transactions' => $transactions->map(fn ($tx) => [
                'uuid' => $tx->uuid,
                'type' => $tx->type,
                'date' => $tx->date?->format('Y-m-d'),
                'description' => $tx->description,
                'reported_amount' => $tx->reported_amount,
                'real_amount' => $tx->real_amount,
                'approval_status' => $tx->approval_status,
                'finance_status' => $tx->finance_status,
            ]),
        ]);
    }

    private function authorizeCreate(Request $request): void
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN', 'FINANCE_MANAGER', 'SUPERVISOR'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER, ADMIN, FINANCE_MANAGER, or SUPERVISOR can create or update projects.'],
            ]);
        }
    }

    private function projectResponse(Project $project): array
    {
        return [
            'id' => $project->id,
            'uuid' => $project->uuid,
            'name' => $project->name,
            'description' => $project->description,
            'is_archived' => $project->is_archived,
            'start_at' => $project->start_at,
            'completed_at' => $project->completed_at,
            'user_id' => $project->user_id,
            'sync_status' => $project->sync_status,
            'created_at' => $project->created_at,
            'updated_at' => $project->updated_at,
        ];
    }
}
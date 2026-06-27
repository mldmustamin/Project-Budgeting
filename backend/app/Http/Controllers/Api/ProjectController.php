<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Project;
use App\Models\ProjectAssignment;
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

    private function authorizeCreate(Request $request): void
    {
        $user = $request->user();

        if (! $user->hasRole(['OWNER', 'ADMIN'])) {
            throw ValidationException::withMessages([
                'role' => ['Only OWNER or ADMIN can create or update projects.'],
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
<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\MasterLocation;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class MasterLocationController extends Controller
{
    /**
     * List locations for a project.
     * GET /api/projects/{project}/locations
     */
    public function index($projectId): JsonResponse
    {
        $locations = MasterLocation::where('project_id', $projectId)
            ->with(['createdBy:id,name', 'updatedBy:id,name'])
            ->orderBy('remote_name')
            ->get();

        return response()->json(['data' => $locations]);
    }

    /**
     * Get single location detail.
     * GET /api/locations/{location}
     */
    public function show(MasterLocation $location): JsonResponse
    {
        $location->load(['createdBy:id,name', 'updatedBy:id,name', 'project:id,name']);
        return response()->json(['data' => $location]);
    }

    /**
     * Create new location. (ADMIN, SUPERVISOR only)
     * POST /api/projects/{project}/locations
     */
    public function store(Request $request, $projectId): JsonResponse
    {
        $user = $request->user();

        // Only ADMIN or SUPERVISOR can create locations
        if (!$user->hasRole('ADMIN') && !$user->hasRole('SUPERVISOR')) {
            return response()->json(['message' => 'Hanya ADMIN atau SUPERVISOR yang bisa menambah lokasi'], 403);
        }
        $validated = $request->validate([
            'remote_name' => ['required', 'string', 'max:255'],
            'address' => ['required', 'string'],
            'provinsi' => ['nullable', 'string', 'max:100'],
            'kota_kab' => ['nullable', 'string', 'max:100'],
            'latitude' => ['nullable', 'numeric', 'between:-90,90'],
            'longitude' => ['nullable', 'numeric', 'between:-180,180'],
        ]);

        $location = MasterLocation::create([
            'uuid' => (string) Str::uuid(),
            'project_id' => $projectId,
            'remote_name' => $validated['remote_name'],
            'address' => $validated['address'],
            'provinsi' => $validated['provinsi'] ?? null,
            'kota_kab' => $validated['kota_kab'] ?? null,
            'latitude' => $validated['latitude'] ?? null,
            'longitude' => $validated['longitude'] ?? null,
            'created_by' => $request->user()->id,
        ]);

        return response()->json(['data' => $location], 201);
    }

    /**
     * Update location. (ADMIN, SUPERVISOR only)
     * PUT /api/locations/{location}
     */
    public function update(Request $request, MasterLocation $location): JsonResponse
    {
        $user = $request->user();

        // Only ADMIN or SUPERVISOR can update locations
        if (!$user->hasRole('ADMIN') && !$user->hasRole('SUPERVISOR')) {
            return response()->json(['message' => 'Hanya ADMIN atau SUPERVISOR yang bisa mengupdate lokasi'], 403);
        }
        $validated = $request->validate([
            'remote_name' => ['sometimes', 'string', 'max:255'],
            'address' => ['sometimes', 'string'],
            'provinsi' => ['nullable', 'string', 'max:100'],
            'kota_kab' => ['nullable', 'string', 'max:100'],
            'latitude' => ['nullable', 'numeric', 'between:-90,90'],
            'longitude' => ['nullable', 'numeric', 'between:-180,180'],
        ]);

        $validated['updated_by'] = $request->user()->id;
        $location->update($validated);

        return response()->json(['data' => $location]);
    }

    /**
     * Delete location. (ADMIN, SUPERVISOR only)
     * DELETE /api/locations/{location}
     */
    public function destroy(Request $request, MasterLocation $location): JsonResponse
    {
        $user = $request->user();

        // Only ADMIN or SUPERVISOR can delete locations
        if (!$user->hasRole('ADMIN') && !$user->hasRole('SUPERVISOR')) {
            return response()->json(['message' => 'Hanya ADMIN atau SUPERVISOR yang bisa menghapus lokasi'], 403);
        }
        $location->delete();
        return response()->json(['message' => 'Location deleted'], 200);
    }

    /**
     * Get budget history for a location.
     * GET /api/locations/{location}/history
     */
    public function history(MasterLocation $location): JsonResponse
    {
        $history = $location->taskExpenses()
            ->whereIn('stage', ['APPROVED', 'REALISASI', 'VERIFIED', 'RECONCILED'])
            ->with(['submittedBy:id,name', 'approvedBy:id,name'])
            ->orderBy('created_at', 'desc')
            ->limit(config('budget.location_history_limit', 10))
            ->get(['id', 'uuid', 'task_no', 'job_type', 'stage', 'total_approved',
                   'total_realization', 'submitted_by', 'approved_by', 'created_at']);

        return response()->json(['data' => $history]);
    }
}

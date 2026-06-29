<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\MasterLocation;
use App\Models\Project;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class LocationWebController extends Controller
{
    public function index(Request $request): View
    {
        $query = MasterLocation::with('project:id,name');

        if ($request->filled('project_id')) {
            $query->where('project_id', $request->project_id);
        }
        if ($request->filled('search')) {
            $q = $request->search;
            $query->where(function ($qry) use ($q) {
                $qry->where('remote_name', 'ilike', "%{$q}%")
                    ->orWhere('address', 'ilike', "%{$q}%");
            });
        }

        $locations = $query->orderBy('remote_name')->paginate(20);
        $projects = Project::orderBy('name')->get(['id', 'name']);

        return view('web.locations.index', compact('locations', 'projects'));
    }

    public function store(Request $request): RedirectResponse
    {
        $validated = $request->validate([
            'project_id' => ['required', 'exists:projects,id'],
            'remote_name' => ['required', 'string', 'max:100'],
            'address' => ['nullable', 'string', 'max:255'],
            'city' => ['nullable', 'string', 'max:100'],
            'province' => ['nullable', 'string', 'max:100'],
            'latitude' => ['nullable', 'numeric'],
            'longitude' => ['nullable', 'numeric'],
        ]);

        MasterLocation::create($validated);

        return redirect()->route('web.locations.index')->with('success', 'Lokasi berhasil ditambahkan');
    }

    public function update(Request $request, MasterLocation $location): RedirectResponse
    {
        $validated = $request->validate([
            'project_id' => ['required', 'exists:projects,id'],
            'remote_name' => ['required', 'string', 'max:100'],
            'address' => ['nullable', 'string', 'max:255'],
            'city' => ['nullable', 'string', 'max:100'],
            'province' => ['nullable', 'string', 'max:100'],
        ]);

        $location->update($validated);

        return redirect()->route('web.locations.index')->with('success', 'Lokasi berhasil diupdate');
    }

    public function destroy(MasterLocation $location): RedirectResponse
    {
        $location->delete();
        return redirect()->route('web.locations.index')->with('success', 'Lokasi berhasil dihapus');
    }
}

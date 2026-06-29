<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\MasterEquipmentOption;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class EquipmentWebController extends Controller
{
    public function index(Request $request): View
    {
        $query = MasterEquipmentOption::query();

        if ($request->filled('field_key')) {
            $query->where('field_key', $request->field_key);
        }

        $options = $query->orderBy('field_key')->orderBy('sort_order')->paginate(25);
        $fieldKeys = MasterEquipmentOption::distinct()->pluck('field_key');

        return view('web.equipment.index', compact('options', 'fieldKeys'));
    }

    public function store(Request $request): RedirectResponse
    {
        $validated = $request->validate([
            'field_key'  => ['required', 'string', 'max:100'],
            'label'      => ['required', 'string', 'max:255'],
            'sort_order' => ['nullable', 'integer', 'min:0'],
        ]);

        MasterEquipmentOption::create([
            'field_key'  => $validated['field_key'],
            'label'      => $validated['label'],
            'sort_order' => $validated['sort_order'] ?? 0,
            'is_active'  => true,
        ]);

        return redirect()->route('web.equipment.index')->with('success', 'Equipment option berhasil ditambahkan.');
    }

    public function destroy(MasterEquipmentOption $option): RedirectResponse
    {
        $option->delete();

        return redirect()->route('web.equipment.index')->with('success', 'Equipment option berhasil dihapus.');
    }
}

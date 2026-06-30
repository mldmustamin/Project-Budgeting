<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\BudgetItemTemplate;
use App\Models\MasterLocation;
use App\Models\MasterEquipmentOption;
use Illuminate\Http\Request;

class MasterDataController extends Controller
{
    public function index(Request $request)
    {
        $locations = MasterLocation::with(['project:id,name', 'createdBy:id,name'])
            ->orderBy('remote_name')
            ->paginate(20, page: $request->integer('loc_page', 1));

        $templates = BudgetItemTemplate::active()
            ->with('paguAmounts')
            ->orderBy('name')
            ->get();

        $equipment = MasterEquipmentOption::orderBy('field_key')
            ->orderBy('value')
            ->paginate(30, page: $request->integer('equip_page', 1));

        // ponytail: minimal tab state via Alpine, no router needed
        return view('web.master-data.index', compact('locations', 'templates', 'equipment'));
    }
}

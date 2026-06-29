<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\MasterEquipmentOption;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class MasterEquipmentOptionController extends Controller
{
    /**
     * Get all equipment options, optionally filtered by field_key.
     * Usage: GET /api/equipment-options?field_key=JENIS_ANTENNA
     */
    public function index(Request $request): JsonResponse
    {
        $query = MasterEquipmentOption::query()->where('is_active', true);

        if ($fieldKey = $request->get('field_key')) {
            $query->forField($fieldKey);
        }

        $options = $query->get()->groupBy('field_key');

        return response()->json(['data' => $options]);
    }
}

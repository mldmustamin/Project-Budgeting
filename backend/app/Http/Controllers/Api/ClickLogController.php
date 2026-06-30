<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreClickLogRequest;
use App\Models\ClickLog;
use App\Services\RedactSensitiveData;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ClickLogController extends Controller
{
    public function store(StoreClickLogRequest $request): JsonResponse
    {
        $sessionId = $request->header('X-Log-Session-Id', '');
        $userId = $request->user()?->id;
        $redactor = new RedactSensitiveData(config('click_logger.redact_keys', []));

        $rows = array_map(function (array $log) use ($sessionId, $userId, $redactor) {
            return [
                'session_id' => $sessionId,
                'user_id'    => $userId,
                'url'        => $log['url'],
                'selector'   => $log['selector'],
                'tag'        => $log['tag'] ?? null,
                'text'       => $redactor->handle($log['text'] ?? null),
                'meta'       => isset($log['meta']) ? json_encode($log['meta']) : null,
                'created_at' => $log['timestamp'],
            ];
        }, $request->input('logs'));

        ClickLog::insert($rows); // ponytail: single bulk insert, no per-row create()

        return response()->json(['stored' => count($rows)], 201);
    }

    public function index(Request $request): JsonResponse
    {
        $query = ClickLog::query();

        if ($sessionId = $request->query('session_id')) {
            $query->where('session_id', $sessionId);
        }
        if ($userId = $request->query('user_id')) {
            $query->where('user_id', $userId);
        }
        if ($url = $request->query('url')) {
            $query->where('url', 'like', "%{$url}%");
        }

        return response()->json([
            'data' => $query->orderBy('created_at')->paginate(50),
        ]);
    }
}

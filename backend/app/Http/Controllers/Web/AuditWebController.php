<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Models\AuditEvent;
use Illuminate\Http\Request;
use Illuminate\View\View;

class AuditWebController extends Controller
{
    public function index(Request $request): View
    {
        $query = AuditEvent::with('user:id,name')->orderByDesc('created_at');

        if ($request->filled('action')) $query->where('action', $request->action);
        if ($request->filled('entity_type')) $query->where('entity_type', $request->entity_type);
        if ($request->filled('user_id')) $query->where('user_id', $request->user_id);
        if ($request->filled('date_from')) $query->whereDate('created_at', '>=', $request->date_from);
        if ($request->filled('date_to')) $query->whereDate('created_at', '<=', $request->date_to);

        $events = $query->paginate(30);
        $users = \App\Models\User::orderBy('name')->get(['id', 'name']);
        $actions = AuditEvent::select('action')->distinct()->orderBy('action')->pluck('action');

        return view('web.audit.index', compact('events', 'users', 'actions'));
    }
}

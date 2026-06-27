@extends('layouts.app')
@section('title', 'Audit Trail')
@section('content')

<div class="bg-white rounded-xl border border-gray-200 p-4 mb-6">
    <form method="GET" class="flex flex-wrap gap-3 items-end">
        <div><label class="text-xs font-semibold text-gray-500 mb-1 block">Aksi</label><select name="action" class="border rounded-lg px-3 py-2 text-sm"><option value="">Semua</option>@foreach($actions as $a)<option value="{{$a}}" @selected(request('action')===$a)>{{$a}}</option>@endforeach</select></div>
        <div><label class="text-xs font-semibold text-gray-500 mb-1 block">User</label><select name="user_id" class="border rounded-lg px-3 py-2 text-sm"><option value="">Semua</option>@foreach($users as $u)<option value="{{$u->id}}" @selected(request('user_id')==$u->id)>{{$u->name}}</option>@endforeach</select></div>
        <div><label class="text-xs font-semibold text-gray-500 mb-1 block">Entity</label><select name="entity_type" class="border rounded-lg px-3 py-2 text-sm"><option value="">Semua</option><option value="transaction" @selected(request('entity_type')==='transaction')>Transaction</option><option value="accounting_period" @selected(request('entity_type')==='accounting_period')>Period</option></select></div>
        <div><label class="text-xs font-semibold text-gray-500 mb-1 block">Dari</label><input type="date" name="date_from" value="{{request('date_from')}}" class="border rounded-lg px-3 py-2 text-sm"></div>
        <div><label class="text-xs font-semibold text-gray-500 mb-1 block">Sampai</label><input type="date" name="date_to" value="{{request('date_to')}}" class="border rounded-lg px-3 py-2 text-sm"></div>
        <div class="flex gap-2"><button class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg">Filter</button><a href="{{route('web.audit.index')}}" class="px-4 py-2 bg-gray-100 text-sm rounded-lg">Reset</a></div>
    </form>
</div>

<div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
    <div class="px-5 py-3 border-b"><h3 class="text-sm font-semibold text-gray-700">Audit Events ({{$events->total()}})</h3></div>
    <table class="w-full text-sm">
        <thead class="bg-gray-50"><tr>
            <th class="px-5 py-3 text-xs text-gray-400 uppercase">Waktu</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">User</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">Aksi</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">Entity</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">UUID</th><th class="px-5 py-3 text-xs text-gray-400 uppercase">Alasan</th>
        </tr></thead>
        <tbody class="divide-y">
            @forelse($events as $e)
            <tr class="hover:bg-gray-50">
                <td class="px-5 py-3 text-gray-500 text-xs whitespace-nowrap">{{$e->created_at->format('d M Y H:i')}}</td>
                <td class="px-5 py-3 text-gray-800 text-xs font-medium">{{$e->user?->name ?? '-'}}</td>
                <td class="px-5 py-3"><span class="px-2 py-0.5 rounded-full text-xs font-medium @if(in_array($e->action,['approve','dispute_resolved_correction']))bg-green-50 text-green-700 @elseif(in_array($e->action,['reject','void']))bg-red-50 text-red-700 @elseif($e->action==='dispute')bg-orange-50 text-orange-700 @elseif($e->action==='submit')bg-blue-50 text-blue-700 @else bg-gray-50 text-gray-600 @endif">{{$e->action}}</span></td>
                <td class="px-5 py-3 text-gray-500 text-xs">{{$e->entity_type}}</td>
                <td class="px-5 py-3 text-gray-400 font-mono text-xs">{{ \Illuminate\Support\Str::limit($e->entity_uuid, 10) }}</td>
                <td class="px-5 py-3 text-gray-400 text-xs max-w-xs truncate">{{$e->reason ?: '-'}}</td>
            </tr>
            @empty
            <tr><td colspan="6" class="px-5 py-8 text-center text-gray-400">Tidak ada audit event.</td></tr>
            @endforelse
        </tbody>
    </table>
    @if($events->hasPages())<div class="px-5 py-3 border-t">{{$events->links()}}</div>@endif
</div>
@endsection

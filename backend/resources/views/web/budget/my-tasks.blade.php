@extends('layouts.app')
@section('title', 'My Tasks')
@section('content')
<h2 class="text-lg font-semibold text-gray-800 mb-4">My Tasks</h2>

@php
$drafts = $tasks->where('stage', 'DRAFT');
$pending = $tasks->whereIn('stage', ['ESTIMASI','FORWARDED']);
$active = $tasks->where('stage', 'APPROVED');
$completed = $tasks->whereIn('stage', ['REALISASI','VERIFIED','RECONCILED']);
$rejected = $tasks->where('stage', 'REJECTED');
@endphp

<div class="space-y-4">
    @if($drafts->isNotEmpty())
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-5 py-3 bg-gray-50 border-b border-gray-100 font-semibold text-sm text-gray-600">📝 Draft ({{ $drafts->count() }})</div>
            @foreach($drafts as $task)
                <div class="p-4 border-b border-gray-100 flex items-center justify-between">
                    <div>
                        <span class="font-medium">#{{ $task->task_no }}</span>
                        <span class="ml-2 text-xs text-gray-500">{{ $task->job_type }}</span>
                        <span class="ml-2 px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-600">{{ $task->stage }}</span>
                        <div class="text-xs text-gray-400 mt-1">{{ $task->location?->remote_name ?? '-' }} · Rp {{ number_format($task->total_estimated, 0, ',', '.') }}</div>
                    </div>
                    <a href="{{ route('web.budget.edit', $task) }}" class="px-3 py-1.5 bg-brand-600 text-white text-xs rounded-lg">Edit</a>
                </div>
            @endforeach
        </div>
    @endif

    @if($pending->isNotEmpty())
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-5 py-3 bg-blue-50 border-b border-blue-100 font-semibold text-sm text-blue-700">⏳ Menunggu Review ({{ $pending->count() }})</div>
            @foreach($pending as $task)
                <div class="p-4 border-b border-gray-100">
                    <span class="font-medium">#{{ $task->task_no }}</span>
                    <span class="ml-2 px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-700">{{ $task->stage }}</span>
                    <span class="ml-2 text-xs text-gray-500">{{ $task->job_type }}</span>
                    <div class="text-xs text-gray-400 mt-1">Rp {{ number_format($task->total_estimated, 0, ',', '.') }} · {{ $task->created_at->diffForHumans() }}</div>
                </div>
            @endforeach
        </div>
    @endif

    @if($active->isNotEmpty())
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-5 py-3 bg-green-50 border-b border-green-100 font-semibold text-sm text-green-700">✅ Siap Dikerjakan ({{ $active->count() }})</div>
            @foreach($active as $task)
                <div class="p-4 border-b border-gray-100 flex items-center justify-between">
                    <div>
                        <span class="font-medium">#{{ $task->task_no }}</span>
                        <span class="ml-2 px-2 py-0.5 rounded-full text-xs bg-green-100 text-green-700">{{ $task->stage }}</span>
                        <div class="text-xs text-gray-400 mt-1">Approved: Rp {{ number_format($task->total_approved, 0, ',', '.') }}</div>
                    </div>
                    <a href="{{ route('web.budget.realize', $task) }}" class="px-3 py-1.5 bg-brand-600 text-white text-xs rounded-lg">Realisasi</a>
                </div>
            @endforeach
        </div>
    @endif

    @if($completed->isNotEmpty())
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="px-5 py-3 bg-purple-50 border-b border-purple-100 font-semibold text-sm text-purple-700">🏁 Selesai ({{ $completed->count() }})</div>
            @foreach($completed as $task)
                <div class="p-4 border-b border-gray-100">
                    <span class="font-medium">#{{ $task->task_no }}</span>
                    <span class="ml-2 px-2 py-0.5 rounded-full text-xs bg-purple-100 text-purple-700">{{ $task->stage }}</span>
                    <div class="text-xs text-gray-400 mt-1">Realisasi: Rp {{ number_format($task->total_realization, 0, ',', '.') }}</div>
                </div>
            @endforeach
        </div>
    @endif

    @if($tasks->isEmpty())
        <p class="text-gray-400 text-center py-8">Belum ada task. Buat task baru?</p>
    @endif
</div>

<div class="mt-4">
    <a href="{{ route('web.budget.create') }}" class="px-4 py-2 bg-brand-600 text-white text-sm rounded-lg">+ Buat Estimasi Budget</a>
    <a href="{{ route('web.laporan.form') }}" class="ml-2 px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg">📋 Laporan Pekerjaan</a>
</div>
@endsection

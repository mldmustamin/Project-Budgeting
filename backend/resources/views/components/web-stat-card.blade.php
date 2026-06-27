@props(['label' => '', 'value' => 0, 'color' => 'indigo', 'isCount' => false, 'icon' => null])

@php
$palette = [
    'green'  => ['bg' => 'bg-brand-50', 'icon' => 'text-brand-600', 'badge' => 'bg-brand-100 text-brand-700', 'border' => 'border-brand-100'],
    'red'    => ['bg' => 'bg-red-50', 'icon' => 'text-red-600', 'badge' => 'bg-red-100 text-red-700', 'border' => 'border-red-100'],
    'indigo' => ['bg' => 'bg-brand-50', 'icon' => 'text-brand-600', 'badge' => 'bg-brand-100 text-brand-700', 'border' => 'border-brand-100'],
    'amber'  => ['bg' => 'bg-amber-50', 'icon' => 'text-amber-600', 'badge' => 'bg-amber-100 text-amber-700', 'border' => 'border-amber-100'],
][$color] ?? ['bg' => 'bg-gray-50', 'icon' => 'text-gray-600', 'badge' => 'bg-gray-100 text-gray-700', 'border' => 'border-gray-100'];
@endphp

<div class="bg-white rounded-2xl border {{ $palette['border'] }} p-5 shadow-sm hover:shadow-md transition-all group">
    <div class="flex items-start justify-between mb-4">
        <div class="w-10 h-10 {{ $palette['bg'] }} rounded-xl flex items-center justify-center">
            @if($icon)
                <svg class="w-5 h-5 {{ $palette['icon'] }}" fill="none" stroke="currentColor" viewBox="0 0 24 24">{{ $icon }}</svg>
            @endif
        </div>
        @if($isCount && $value > 0)
            <span class="px-2 py-0.5 text-[11px] font-semibold rounded-full {{ $palette['badge'] }}">{{ $value }} pending</span>
        @endif
    </div>
    <h4 class="text-[11px] font-semibold text-gray-400 uppercase tracking-widest mb-1">{{ $label }}</h4>
    <p class="text-2xl font-extrabold text-gray-900 tracking-tight">
        @if($isCount)
            {{ number_format($value, 0, ',', '.') }}
        @else
            Rp{{ number_format($value, 0, ',', '.') }}
        @endif
    </p>
</div>

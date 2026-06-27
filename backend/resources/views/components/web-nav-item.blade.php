@props(['href' => '#', 'icon' => '', 'label' => '', 'badge' => null, 'badgeColor' => 'bg-red-500'])

<a href="{{ $href }}"
   class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors
          {{ request()->url() === $href ? 'bg-gray-800 text-white' : 'text-gray-400 hover:bg-gray-800 hover:text-white' }}"
   x-data="{ expanded: true }">
    <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="{{ $icon }}"/>
    </svg>
    <span x-show="sidebarOpen" class="flex-1">{{ $label }}</span>
    @if($badge && $badge > 0)
        <span x-show="sidebarOpen" class="px-2 py-0.5 rounded-full text-xs font-bold {{ $badgeColor }} text-white">{{ $badge }}</span>
    @endif
</a>

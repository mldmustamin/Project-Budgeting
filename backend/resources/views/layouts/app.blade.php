<!DOCTYPE html>
<html lang="id" class="h-full bg-gray-50">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Funds Manager — @yield('title', 'Dashboard')</title>
    @if(file_exists(public_path('build/manifest.json')) || file_exists(public_path('hot')))
        @vite(['resources/css/app.css', 'resources/js/app.js'])
    @endif
    <script src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js" defer></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        [x-cloak] { display: none !important; }
        body { font-family: 'Inter', system-ui, sans-serif; }
    </style>
</head>
<body class="h-full">

<div x-data="{ sidebarOpen: true }" class="flex h-full">
    {{-- Sidebar --}}
    <aside :class="sidebarOpen ? 'w-64' : 'w-16'"
           class="flex flex-col bg-gray-900 text-gray-300 transition-all duration-200 shrink-0">
        <div class="flex items-center gap-3 px-4 h-16 border-b border-gray-700">
            <div class="w-8 h-8 bg-brand-600 rounded-lg flex items-center justify-center text-white font-bold text-sm shrink-0">FM</div>
            <span x-show="sidebarOpen" class="font-semibold text-white text-sm">Funds Manager</span>
        </div>

        <nav class="flex-1 py-4 space-y-1 px-2 overflow-y-auto">
            <x-web-nav-item href="{{ route('web.dashboard') }}" icon="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-4 0a1 1 0 01-1-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 01-1 1" label="Dashboard" />

            <x-web-nav-item href="{{ route('web.projects.index') }}" icon="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" label="Project" />

            <x-web-nav-item href="{{ route('web.transactions.index') }}" icon="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" label="Transaksi" />

            @if(auth()->user()?->hasRole(['OWNER','ADMIN','FINANCE_MANAGER']))
            <x-web-nav-item href="{{ route('web.approval.index') }}" icon="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" label="Approval" badge="{{ \App\Models\Transaction::where('approval_status','PENDING')->whereNull('deleted_at')->count() }}" badgeColor="bg-amber-500" />
            @endif

            @if(auth()->user()?->hasRole(['OWNER','ADMIN','AUDITOR','FINANCE_MANAGER']))
            <x-web-nav-item href="{{ route('web.audit.index') }}" icon="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" label="Audit Trail" />
            @endif
            @if(auth()->user()?->hasRole(['OWNER','FINANCE_MANAGER']))
            <x-web-nav-item href="{{ route('web.periods.index') }}" icon="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" label="Periods" />
            @endif
            @if(auth()->user()?->hasRole(['OWNER','ADMIN']))
            <x-web-nav-item href="{{ route('web.users.index') }}" icon="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" label="Users" />
            <x-web-nav-item href="{{ route('web.sync.monitor') }}" icon="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" label="Sync Monitor" />
            @endif
        </nav>

        <div class="border-t border-gray-700 p-4">
            <div class="flex items-center gap-3" x-show="sidebarOpen">
                <div class="w-8 h-8 bg-gray-600 rounded-full flex items-center justify-center text-white text-xs font-bold">
                    {{ strtoupper(substr(auth()->user()?->name ?? 'U', 0, 2)) }}
                </div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-white truncate">{{ auth()->user()?->name ?? 'User' }}</p>
                    <p class="text-xs text-gray-400 truncate">{{ auth()->user()?->roles->first()?->name ?? '' }}</p>
                </div>
            </div>
            <form method="POST" action="{{ route('logout') }}" class="mt-2">
                @csrf
                <button type="submit" class="w-full text-left text-xs text-gray-400 hover:text-white transition-colors">
                    <span x-show="sidebarOpen">Keluar</span>
                    <span x-show="!sidebarOpen" class="flex justify-center">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>
                    </span>
                </button>
            </form>
        </div>
    </aside>

    {{-- Main Content --}}
    <main class="flex-1 overflow-y-auto">
        <header class="bg-white border-b border-gray-200 h-16 flex items-center px-6 gap-4 shrink-0">
            <button @click="sidebarOpen = !sidebarOpen" class="text-gray-500 hover:text-gray-700">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/></svg>
            </button>
            <h1 class="text-lg font-semibold text-gray-800">@yield('title', 'Dashboard')</h1>

            {{-- Universal Search --}}
            <div class="flex-1 max-w-md mx-4" x-data="{ query: '', results: [], open: false, loading: false, idx: -1 }" @click.away="open=false">
                <div class="relative">
                    <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                    <input type="text" x-model="query"
                           @input.debounce.250ms="if(query.length >= 2) { loading = true; fetch(`/search?q=${encodeURIComponent(query)}`).then(r=>r.json()).then(d=>{ results = d; open = d.length > 0; loading = false }) } else { results = []; open = false }"
                           @keydown.escape="open=false" @keydown.arrow-down.prevent="idx = Math.min(idx + 1, results.length - 1)"
                           @keydown.arrow-up.prevent="idx = Math.max(idx - 1, 0)"
                           @keydown.enter.prevent="if(results[idx]) window.location=results[idx].url"
                           placeholder="Cari transaksi, project, user..." 
                           class="w-full pl-9 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:bg-white transition-colors">
                    <div x-show="loading" class="absolute right-3 top-1/2 -translate-y-1/2"><div class="w-3 h-3 border-2 border-brand-500 border-t-transparent rounded-full animate-spin"></div></div>
                </div>
                <div x-show="open" x-transition class="absolute z-50 mt-2 w-full max-w-md bg-white rounded-xl border border-gray-100 shadow-xl overflow-hidden" style="max-height: 360px; overflow-y: auto">
                    <template x-for="(r, i) in results" :key="i">
                        <a :href="r.url" @click="open=false"
                           class="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0"
                           :class="{ 'bg-gray-50': i === idx }">
                            <span class="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold shrink-0"
                                  :class="r.type === 'transaction' ? 'bg-blue-50 text-blue-600' : r.type === 'project' ? 'bg-brand-50 text-brand-600' : 'bg-purple-50 text-purple-600'">
                                <span x-text="r.type === 'transaction' ? 'TX' : r.type === 'project' ? 'P' : 'U'"></span>
                            </span>
                            <div class="flex-1 min-w-0">
                                <p class="text-sm font-medium text-gray-800 truncate" x-text="r.label"></p>
                                <p class="text-xs text-gray-400 truncate" x-text="r.sub"></p>
                            </div>
                            <span class="text-[10px] text-gray-300 uppercase font-semibold" x-text="r.type"></span>
                        </a>
                    </template>
                    <div x-show="results.length === 0 && query.length >= 2" class="px-4 py-8 text-center text-gray-400 text-sm">Tidak ditemukan.</div>
                </div>
            </div>
        </header>

        <div class="p-6">
            @if(session('success'))
                <div class="mb-4 p-4 bg-green-50 border border-green-200 text-green-800 rounded-lg text-sm">
                    {{ session('success') }}
                </div>
            @endif
            @if(session('error'))
                <div class="mb-4 p-4 bg-red-50 border border-red-200 text-red-800 rounded-lg text-sm">
                    {{ session('error') }}
                </div>
            @endif
            @if($errors->any())
                <div class="mb-4 p-4 bg-red-50 border border-red-200 text-red-800 rounded-lg text-sm">
                    <ul class="list-disc list-inside">
                        @foreach($errors->all() as $error)
                            <li>{{ $error }}</li>
                        @endforeach
                    </ul>
                </div>
            @endif

            @yield('content')
        </div>
    </main>
</div>

</body>
</html>

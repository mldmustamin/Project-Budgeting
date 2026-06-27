<!DOCTYPE html>
<html lang="id" class="h-full bg-gray-50">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Funds Manager — Login</title>
    @if(file_exists(public_path('build/manifest.json')) || file_exists(public_path('hot')))
        @vite(['resources/css/app.css', 'resources/js/app.js'])
    @endif
</head>
<body class="h-full">
<div class="min-h-full flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
        <div>
            <div class="mx-auto w-16 h-16 bg-brand-600 rounded-2xl flex items-center justify-center">
                <span class="text-white font-bold text-xl">FM</span>
            </div>
            <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">Funds Manager</h2>
            <p class="mt-2 text-center text-sm text-gray-600">Masuk ke dashboard web</p>
        </div>

        <form class="mt-8 space-y-6" action="{{ route('login.post') }}" method="POST">
            @csrf

            @if($errors->any())
                <div class="rounded-md bg-red-50 p-4">
                    <div class="flex">
                        <div class="text-sm text-red-700">
                            @foreach($errors->all() as $error)
                                <p>{{ $error }}</p>
                            @endforeach
                        </div>
                    </div>
                </div>
            @endif

            <div class="space-y-4">
                <div>
                    <label for="login" class="block text-sm font-medium text-gray-700">ID Karyawan / Email</label>
                    <input id="login" name="login" type="text" autocomplete="username" required
                           value="{{ old('login') }}"
                           class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm">
                </div>

                <div>
                    <label for="password" class="block text-sm font-medium text-gray-700">Password</label>
                    <input id="password" name="password" type="password" autocomplete="current-password" required
                           class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 text-sm">
                </div>

                <div class="flex items-center">
                    <input id="remember" name="remember" type="checkbox"
                           class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded">
                    <label for="remember" class="ml-2 block text-sm text-gray-700">Ingat saya</label>
                </div>
            </div>

            <button type="submit"
                    class="w-full py-2.5 px-4 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-lg focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500 transition-colors">
                Masuk
            </button>
        </form>
    </div>
</div>
</body>
</html>

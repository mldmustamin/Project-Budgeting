<!DOCTYPE html>
<html lang="id" class="h-full bg-gray-50">
<head>
    <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Funds Manager — Ganti Password</title>
    @if(file_exists(public_path('build/manifest.json'))) @vite(['resources/css/app.css', 'resources/js/app.js']) @endif
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>body{font-family:'Inter',system-ui,sans-serif}</style>
</head>
<body class="h-full">
<div class="min-h-full flex items-center justify-center py-12 px-4">
    <div class="max-w-md w-full space-y-8">
        <div>
            <div class="mx-auto w-16 h-16 bg-brand-600 rounded-2xl flex items-center justify-center"><span class="text-white font-bold text-xl">FM</span></div>
            <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">Ganti Password</h2>
            <p class="mt-2 text-center text-sm text-gray-600">Anda harus mengganti password sebelum melanjutkan.</p>
        </div>
        <form class="mt-8 space-y-4" method="POST" action="{{ route('password.update') }}">
            @csrf
            @if($errors->any())
                <div class="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm">@foreach($errors->all() as $e)<p>{{$e}}</p>@endforeach</div>
            @endif
            <div>
                <label class="block text-sm font-medium text-gray-700">Password Baru</label>
                <input name="password" type="password" required class="mt-1 block w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-brand-500 text-sm">
            </div>
            <div>
                <label class="block text-sm font-medium text-gray-700">Konfirmasi Password</label>
                <input name="password_confirmation" type="password" required class="mt-1 block w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-brand-500 text-sm">
            </div>
            <button type="submit" class="w-full py-2.5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-lg transition-colors">Simpan Password</button>
        </form>
    </div>
</div>
</body>
</html>

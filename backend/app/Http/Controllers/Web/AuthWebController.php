<?php

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\View\View;

class AuthWebController extends Controller
{
    public function showLogin(): View
    {
        return view('web.login');
    }

    public function login(Request $request): RedirectResponse
    {
        $v = $request->validate([
            'login' => ['required', 'string'],
            'password' => ['required'],
        ]);

        $login = $v['login'];
        $field = filter_var($login, FILTER_VALIDATE_EMAIL) ? 'email' : (is_numeric($login) ? 'employee_id' : 'email');

        if (Auth::attempt([$field => $login, 'password' => $v['password']], $request->boolean('remember'))) {
            $request->session()->regenerate();
            $user = Auth::user();

            if ($user->password_change_required) {
                return redirect()->route('password.change');
            }

            return redirect()->intended(route('web.dashboard'));
        }

        return back()->withErrors(['login' => 'ID karyawan/email atau password tidak cocok.'])->onlyInput('login');
    }

    public function showChangePassword(): View
    {
        return view('web.password-change');
    }

    public function changePassword(Request $request): RedirectResponse
    {
        $v = $request->validate([
            'password' => 'required|min:6|confirmed',
        ]);
        $user = Auth::user();
        $user->update(['password' => bcrypt($v['password']), 'password_change_required' => false]);
        return redirect()->route('web.dashboard')->with('success', 'Password berhasil diubah.');
    }

    public function logout(Request $request): RedirectResponse
    {
        Auth::logout();

        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return redirect()->route('login');
    }
}

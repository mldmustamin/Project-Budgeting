<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\ValidationException;

class AuthController extends Controller
{
    public function login(Request $request): JsonResponse
    {
        $request->validate([
            'login' => 'required|string',
            'password' => 'required|string',
            'device_id' => 'nullable|string|max:255',
            'device_name' => 'nullable|string|max:255',
        ]);

        $login = $request->login;
        $field = filter_var($login, FILTER_VALIDATE_EMAIL) ? 'email' : (is_numeric($login) ? 'employee_id' : 'email');

        $user = User::where($field, $login)->first();

        if (! $user || ! Hash::check($request->password, $user->password)) {
            throw ValidationException::withMessages([
                'login' => ['ID karyawan/email atau password tidak cocok.'],
            ]);
        }

        $tokenName = $request->device_name ?? $request->device_id ?? 'mobile';
        $token = $user->createToken($tokenName);

        return response()->json([
            'user' => $this->userResponse($user),
            'access_token' => $token->plainTextToken,
            'token_type' => 'Bearer',
        ]);
    }

    private function userResponse(User $user): array
    {
        return [
            'id' => $user->id,
            'uuid' => $user->uuid,
            'name' => $user->name,
            'email' => $user->email,
            'employee_id' => $user->employee_id,
            'password_change_required' => $user->password_change_required,
            'roles' => $user->getRoleNames()->values()->toArray(),
        ];
    }

    public function me(Request $request): JsonResponse
    {
        return response()->json([
            'user' => $this->userResponse($request->user()),
        ]);
    }

    public function logout(Request $request): JsonResponse
    {
        $request->user()->tokens()->delete();

        return response()->json([
            'message' => 'Token revoked.',
        ]);
    }

}
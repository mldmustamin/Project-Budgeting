<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StoreClickLogRequest extends FormRequest
{
    public function authorize(): bool
    {
        return config('click_logger.enabled', true);
    }

    public function rules(): array
    {
        return [
            'logs' => ['required', 'array', 'max:50'],
            'logs.*.timestamp' => ['required', 'date'],
            'logs.*.action' => ['required', 'string', 'in:click'],
            'logs.*.selector' => ['required', 'string', 'max:255'],
            'logs.*.url' => ['required', 'string', 'max:2048'],
            'logs.*.text' => ['nullable', 'string', 'max:255'],
            'logs.*.tag' => ['nullable', 'string', 'max:50'],
            'logs.*.meta' => ['nullable', 'array'],
        ];
    }

    protected function failedAuthorization(): void
    {
        abort(403, 'Click logger is disabled.');
    }
}

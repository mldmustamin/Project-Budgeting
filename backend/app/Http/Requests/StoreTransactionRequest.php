<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class StoreTransactionRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true; // Handled by auth:sanctum middleware
    }

    public function rules(): array
    {
        return [
            'type' => ['required', 'string', Rule::in([
                \App\Models\Transaction::TYPE_FUND_IN,
                \App\Models\Transaction::TYPE_OFFICE_EXPENSE,
                \App\Models\Transaction::TYPE_PERSONAL_EXPENSE,
            ])],
            'project_uuid' => ['required', 'string', 'exists:projects,uuid'],
            'date' => ['required', 'date_format:Y-m-d'],
            'description' => ['required_if:type,OFFICE_EXPENSE,PERSONAL_EXPENSE', 'string', 'max:500'],
            'reported_amount' => ['required', 'integer', 'min:1'],
            'real_amount' => ['required', 'integer', 'min:1'],
            'account_id' => ['nullable', 'integer', 'exists:accounts,id'],
            'category_id' => ['nullable', 'integer', 'exists:categories,id'],
            'note' => ['nullable', 'string', 'max:1000'],
            'source_text' => ['nullable', 'string'],
        ];
    }
}
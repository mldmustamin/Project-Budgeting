<?php

// Config: app budget rules
// All hardcoded parameters centralized here

return [
    // Field Engineer
    'max_drafts_per_user' => (int) env('BUDGET_MAX_DRAFTS', 5),

    // Pagination
    'pagination_per_page' => (int) env('BUDGET_PAGINATION', 20),

    // Location history
    'location_history_limit' => (int) env('BUDGET_HISTORY_LIMIT', 10),

    // Stages (ordered)
    'stages' => [
        'DRAFT',
        'ESTIMASI',
        'FORWARDED',
        'APPROVED',
        'REALISASI',
        'VERIFIED',
        'RECONCILED',
        'REJECTED',
    ],

    // Stages that can be rejected (cascade back to DRAFT)
    'rejectable_stages' => ['ESTIMASI', 'FORWARDED'],

    // Job types
    'job_types' => [
        'INSTALASI',
        'RELOKASI',
        'PMCM',
        'DISMANTLE',
        'SURVEY',
    ],

    // Pagu types
    'pagu_types' => [
        'FIXED_PAGU' => 'Nominal absolut, tidak boleh lebih',
        'TICKET' => 'Wajib bukti fisik. Tanpa bukti = Finance tentukan nominal saat rekonsiliasi',
        'MANAGER_APPROVAL' => 'OWNER tentukan nominal saat approval',
    ],

    // Ticket rules
    'ticket_requires_bill' => true,
    'ticket_bill_note' => 'Tiket resmi WAJIB. Tidak boleh dicoret. Tanpa bukti = Finance tentukan nominal saat rekonsiliasi',

    // Hotel rules
    'hotel_can_exceed_pagu' => true,
    'hotel_exceed_requires_bill' => true,
];

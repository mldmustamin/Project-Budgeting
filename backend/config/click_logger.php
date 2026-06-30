<?php

return [

    'enabled' => env('CLICK_LOGGER_ENABLED', true),

    'batch_size' => env('CLICK_LOGGER_BATCH_SIZE', 10),

    'redact_keys' => explode(',', env('CLICK_LOGGER_REDACT_KEYS', 'password,token,card,secret,key,credential')),

    'retention_days' => env('CLICK_LOGGER_RETENTION_DAYS', 14),

];

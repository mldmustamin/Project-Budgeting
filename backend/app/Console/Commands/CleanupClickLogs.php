<?php

namespace App\Console\Commands;

use App\Models\ClickLog;
use Illuminate\Console\Command;

class CleanupClickLogs extends Command
{
    protected $signature = 'click-logs:cleanup';
    protected $description = 'Delete click logs older than retention period';

    public function handle(): int
    {
        $days = config('click_logger.retention_days', 14);
        $cutoff = now()->subDays($days);

        $deleted = ClickLog::where('created_at', '<', $cutoff)->delete();

        $this->info("Deleted {$deleted} click logs older than {$days} days.");

        return self::SUCCESS;
    }
}

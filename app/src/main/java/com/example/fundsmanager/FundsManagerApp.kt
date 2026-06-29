package com.example.fundsmanager

import android.app.Application
import androidx.work.*
import com.example.fundsmanager.data.sync.SyncWorker
import com.example.fundsmanager.util.CrashReporter
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.FileAppLogger
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class FundsManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appLogger = FileAppLogger(this)

        // Crash Reporter — saves crashes for later review
        CrashReporter.init(this)

        // Also log to file logger
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            CrashReporter.init(this) // ensure captured
            appLogger.crash(
                screen = "Application",
                action = "uncaught_exception",
                message = "Unhandled crash on thread ${thread.name}",
                throwable = throwable
            )
            originalHandler?.uncaughtException(thread, throwable)
        }

        appLogger.info(
            category = AppLogCategory.APP,
            screen = "Application",
            action = "app_opened",
            message = "Funds Manager app opened",
            details = "packageName=$packageName, crashes_pending=${CrashReporter.hasPendingCrashes()}"
        )

        scheduleSync()
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "fundsmanager_sync", ExistingPeriodicWorkPolicy.KEEP, syncRequest
        )
    }
}

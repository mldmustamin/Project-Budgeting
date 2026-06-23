package com.example.fundsmanager

import android.app.Application
import com.example.fundsmanager.util.logging.AppLogCategory
import com.example.fundsmanager.util.logging.FileAppLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FundsManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appLogger = FileAppLogger(this)
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
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
            details = "packageName=$packageName"
        )
    }
}

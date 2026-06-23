package com.example.fundsmanager.util.logging

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileAppLogger(
    context: Context
) : AppLogger {
    private val appContext = context.applicationContext
    private val logFile = File(appContext.filesDir, LOG_FILE_NAME)
    private val oldLogFile = File(appContext.filesDir, OLD_LOG_FILE_NAME)
    private val lock = Any()
    private val appVersion: String by lazy { resolveAppVersion() }
    private val appBuildType: String by lazy { if (isDebuggable()) "debug" else "release" }

    override fun debug(category: AppLogCategory, screen: String?, action: String?, message: String, details: String?) {
        log(AppLogLevel.DEBUG, category, screen, action, message, details, null)
    }

    override fun info(category: AppLogCategory, screen: String?, action: String?, message: String, details: String?) {
        log(AppLogLevel.INFO, category, screen, action, message, details, null)
    }

    override fun warning(category: AppLogCategory, screen: String?, action: String?, message: String, details: String?) {
        log(AppLogLevel.WARNING, category, screen, action, message, details, null)
    }

    override fun error(category: AppLogCategory, screen: String?, action: String?, message: String, throwable: Throwable?, details: String?) {
        log(AppLogLevel.ERROR, category, screen, action, message, details, throwable)
    }

    override fun crash(screen: String?, action: String?, message: String, throwable: Throwable) {
        log(AppLogLevel.CRASH, AppLogCategory.APP, screen, action, message, null, throwable)
    }

    private fun log(
        level: AppLogLevel,
        category: AppLogCategory,
        screen: String?,
        action: String?,
        message: String,
        details: String?,
        throwable: Throwable?
    ) {
        val line = formatLine(level, category, screen, action, message, details, throwable)
        writeLogcat(level, line, throwable)
        writeFileSafely(line)
    }

    private fun formatLine(
        level: AppLogLevel,
        category: AppLogCategory,
        screen: String?,
        action: String?,
        message: String,
        details: String?,
        throwable: Throwable?
    ): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
        val safeDetails = details?.take(MAX_DETAILS_LENGTH)
        return buildString {
            append("[").append(timestamp).append("] ")
            append("[").append(level.name).append("] ")
            append("[").append(category.name).append("] ")
            append("screen=").append(screen ?: "-").append(' ')
            append("action=").append(action ?: "-").append('\n')
            append("message=").append(message).append('\n')
            append("details=").append(safeDetails ?: "-").append('\n')
            append("thread=").append(Thread.currentThread().name).append(' ')
            append("version=").append(appVersion).append(' ')
            append("buildType=").append(appBuildType).append('\n')
            if ((level == AppLogLevel.ERROR || level == AppLogLevel.CRASH) && throwable != null) {
                append("exception=").append(throwable::class.java.simpleName).append(": ").append(throwable.message ?: "-").append('\n')
                append(stackTraceOf(throwable))
            }
            append('\n')
        }
    }

    private fun writeLogcat(level: AppLogLevel, line: String, throwable: Throwable?) {
        try {
            when (level) {
                AppLogLevel.DEBUG -> Log.d(TAG, line)
                AppLogLevel.INFO -> Log.i(TAG, line)
                AppLogLevel.WARNING -> Log.w(TAG, line)
                AppLogLevel.ERROR -> Log.e(TAG, line, throwable)
                AppLogLevel.CRASH -> Log.wtf(TAG, line, throwable)
            }
        } catch (_: Exception) {
            // Logging must never crash the app.
        }
    }

    private fun writeFileSafely(line: String) {
        try {
            synchronized(lock) {
                rotateIfNeeded(line.length)
                logFile.appendText(line)
            }
        } catch (e: Exception) {
            try {
                Log.e(TAG, "Failed to write app log file", e)
            } catch (_: Exception) {
                // Ignore all logging failures.
            }
        }
    }

    private fun rotateIfNeeded(incomingLength: Int) {
        if (logFile.exists() && logFile.length() + incomingLength > MAX_FILE_SIZE_BYTES) {
            if (oldLogFile.exists()) oldLogFile.delete()
            logFile.renameTo(oldLogFile)
            logFile.writeText("")
        }
    }

    private fun stackTraceOf(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun resolveAppVersion(): String {
        return try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun isDebuggable(): Boolean {
        return (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    companion object {
        const val LOG_FILE_NAME = "app_logs.txt"
        const val OLD_LOG_FILE_NAME = "app_logs_old.txt"
        private const val TAG = "FundsManager"
        private const val MAX_DETAILS_LENGTH = 1000
        private const val MAX_FILE_SIZE_BYTES = 1_048_576L
    }
}

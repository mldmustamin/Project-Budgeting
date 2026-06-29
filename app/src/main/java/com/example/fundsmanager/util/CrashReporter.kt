package com.example.fundsmanager.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crash reporter that captures uncaught exceptions and saves them to local storage.
 * On next app launch, pending crash reports are available for review/sending.
 */
object CrashReporter {

    private const val CRASH_DIR = "crash_reports"
    private const val MAX_CRASH_FILES = 20
    private lateinit var appContext: Context
    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    fun hasPendingCrashes(): Boolean {
        return getCrashDir().listFiles()?.isNotEmpty() == true
    }

    fun getCrashFiles(): List<File> {
        return getCrashDir().listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.take(MAX_CRASH_FILES)
            ?: emptyList()
    }

    fun clearCrashes() {
        getCrashDir().listFiles()?.forEach { it.delete() }
    }

    fun deleteCrash(file: File) {
        file.delete()
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        try {
            val crashFile = File(getCrashDir(), "crash_${System.currentTimeMillis()}.txt")
            crashFile.parentFile?.mkdirs()
            crashFile.writeText(buildCrashReport(thread, throwable))
        } catch (_: Exception) {
            // Last resort — can't do much if even crash reporting fails
        }
    }

    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)

        pw.println("========================================================================")
        pw.println("CRASH REPORT — FundManager V2")
        pw.println("========================================================================")
        pw.println("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")
        pw.println("Thread: ${thread.name} (${thread.state})")
        pw.println()
        pw.println("--- Device Info ---")
        pw.println("Model: ${Build.MODEL}")
        pw.println("Manufacturer: ${Build.MANUFACTURER}")
        pw.println("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        pw.println("App Version: ${getAppVersion()}")
        pw.println()
        pw.println("--- Stack Trace ---")
        throwable.printStackTrace(pw)
        pw.println()
        pw.println("--- Cause Chain ---")
        var cause = throwable.cause
        var depth = 1
        while (cause != null) {
            pw.println("Cause #$depth:")
            cause.printStackTrace(pw)
            pw.println()
            cause = cause.cause
            depth++
        }
        pw.println("========================================================================")
        pw.close()
        return sw.toString()
    }

    private fun getAppVersion(): String {
        return try {
            val pkgInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            "${pkgInfo.versionName ?: "unknown"} (${pkgInfo.versionCode})"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun getCrashDir(): File {
        return File(appContext.filesDir, CRASH_DIR)
    }
}

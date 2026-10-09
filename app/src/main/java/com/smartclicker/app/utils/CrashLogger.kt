package com.smartclicker.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val CRASH_FILE_NAME = "crash_log.txt"
    private const val LOGS_FILE_NAME = "app_events.log"

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(context, thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logEvent(context: Context, tag: String, message: String) {
        try {
            val file = File(context.filesDir, LOGS_FILE_NAME)
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            file.appendText("[$time] [$tag] $message\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write event log", e)
        }
    }

    fun logException(context: Context, tag: String, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        logEvent(context, tag, "EXCEPTION: $stackTrace")
    }

    private fun handleUncaughtException(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val report = buildString {
                appendLine("=== SMART CLICKER CRASH REPORT ===")
                appendLine("Timestamp: $time")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("Thread: ${thread.name} (ID: ${thread.id})")
                appendLine("Crash Message: ${throwable.message}")
                appendLine("--- Stack Trace ---")
                appendLine(stackTrace)
                appendLine("==================================")
            }

            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(report)

            // Also append to full events log
            val allLogs = File(context.filesDir, LOGS_FILE_NAME)
            allLogs.appendText("\n" + report + "\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }

    fun getLatestCrashLog(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

    fun getFullDiagnosticLog(context: Context): String {
        return buildString {
            appendLine("=== SMART CLICKER SYSTEM DIAGNOSTICS ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Product: ${Build.PRODUCT})")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("----------------------------------------\n")

            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (crashFile.exists()) {
                appendLine("[LATEST CRASH REPORT]")
                appendLine(crashFile.readText())
                appendLine("----------------------------------------\n")
            } else {
                appendLine("[CRASH STATUS]: No crashes recorded.\n")
            }

            val logFile = File(context.filesDir, LOGS_FILE_NAME)
            if (logFile.exists()) {
                appendLine("[APPLICATION EVENT LOGS (Last 50 lines)]")
                val lines = logFile.readLines().takeLast(50)
                lines.forEach { appendLine(it) }
            } else {
                appendLine("[EVENT LOGS]: No event logs yet.")
            }
        }
    }

    fun copyLogsToClipboard(context: Context) {
        val logs = getFullDiagnosticLog(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SmartClicker Diagnostic Logs", logs)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareLogs(context: Context) {
        val logs = getFullDiagnosticLog(context)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, logs)
            putExtra(Intent.EXTRA_SUBJECT, "Smart Clicker Diagnostic Report")
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Diagnostic Logs").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(shareIntent)
    }

    fun clearLogs(context: Context) {
        File(context.filesDir, CRASH_FILE_NAME).delete()
        File(context.filesDir, LOGS_FILE_NAME).delete()
        Toast.makeText(context, "Log files cleared", Toast.LENGTH_SHORT).show()
    }
}

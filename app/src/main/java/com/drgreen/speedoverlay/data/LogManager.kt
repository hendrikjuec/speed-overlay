/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages debug logging for the application.
 * Writes diagnostic information to a persistent file on the internal storage.
 * Used for investigating head-unit specific issues and crashes.
 */
class LogManager(private val context: Context) {
    private val debugLogFile = File(context.filesDir, "debug_log.txt")

    /**
     * Writes a message to the debug log file on internal storage.
     * Includes a timestamp and optional throwable stack trace.
     */
    fun logDebug(message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] $message\n" + (throwable?.let { Log.getStackTraceString(it) + "\n" } ?: "")

        try {
            FileOutputStream(debugLogFile, true).use {
                it.write(logLine.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to write to debug log file", e)
        }
    }

    /**
     * Returns the contents of the debug log file.
     */
    fun getDebugLogs(): String {
        return if (debugLogFile.exists()) debugLogFile.readText() else "No logs found."
    }

    /**
     * Deletes the debug log file.
     */
    fun clearDebugLogs() {
        if (debugLogFile.exists()) debugLogFile.delete()
    }
}

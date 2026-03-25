/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.drgreen.speedoverlay.data.LogManager
import com.drgreen.speedoverlay.data.SettingsManager

/**
 * Globaler Exception Handler, der Abstürze abfängt und persistente Debug-Logs schreibt.
 * Hilft bei der Diagnose von Head-Unit-spezifischen Crashes ohne ADB-Zugriff.
 */
class CrashReporter(
    private val context: Context,
    private val logManager: LogManager,
    private val settingsManager: SettingsManager
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(this)
        logManager.logDebug("CrashReporter initialized. Device: ${Build.MODEL} (${Build.MANUFACTURER}), Android: ${Build.VERSION.RELEASE}")
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val message = "FATAL CRASH on thread ${thread.name}: ${throwable.message}"
        Log.e("CrashReporter", message, throwable)

        // Immer loggen, auch wenn Debug-Mode aus ist (bei Fatal Crashes)
        logManager.logDebug(message, throwable)

        // Falls wir im Debug-Modus sind, versuchen wir die Logs noch zu flushen
        if (settingsManager.isDebugModeEnabled) {
            logManager.logDebug("Debug Context: [OverlayVisible: ${settingsManager.overlayX},${settingsManager.overlayY}]")
        }

        // An den originalen Handler weitergeben (zeigt "App has stopped" Dialog)
        defaultHandler?.uncaughtException(thread, throwable)
    }
}

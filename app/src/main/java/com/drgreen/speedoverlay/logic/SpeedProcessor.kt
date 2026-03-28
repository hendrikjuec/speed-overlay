/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import android.location.Location
import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.KalmanFilter
import kotlin.math.roundToInt

/**
 * Processes speed data, performs smoothing, and handles unit conversion.
 */
class SpeedProcessor(private val smoothingWindow: Int = Config.SMOOTHING_WINDOW) {

    private companion object {
        const val MS_TO_KMH = 3.6f
        const val MS_TO_MPH = 2.23694f
    }

    private val speedHistory = FloatArray(smoothingWindow)
    private var historyIndex = 0
    private var historySize = 0
    private val kalmanFilter = KalmanFilter()

    var lastSpeedKmh: Float = 0f
        private set

    /**
     * Smoothes raw GPS speed and converts it to the target unit.
     */
    fun getSmoothedSpeed(location: Location, useMph: Boolean, isPhysicallyMoving: Boolean = true): Int {
        if (!isPhysicallyMoving) {
            clearHistory()
            return 0
        }

        // GPS Accuracy Filter
        if (location.hasAccuracy() && location.accuracy > Config.MIN_LOCATION_ACCURACY_METERS) {
            return calculateFinalSpeed(useMph)
        }

        val rawSpeedMs = location.speed
        val safeRawSpeed = if (rawSpeedMs < Config.JITTER_THRESHOLD_MS) 0f else rawSpeedMs

        // Kalman Filtering
        val kalmanSpeed = kalmanFilter.filter(safeRawSpeed)

        updateHistory(kalmanSpeed)

        return calculateFinalSpeed(useMph)
    }

    private fun calculateFinalSpeed(useMph: Boolean): Int {
        if (historySize <= 0) {
            lastSpeedKmh = 0f
            return 0
        }

        var sum = 0f
        for (i in 0 until historySize) {
            sum += speedHistory[i]
        }
        val avgSpeedMs = sum / historySize
        lastSpeedKmh = avgSpeedMs * MS_TO_KMH

        if (avgSpeedMs * MS_TO_KMH < Config.MIN_DISPLAY_SPEED) return 0

        val factor = if (useMph) MS_TO_MPH else MS_TO_KMH
        return (avgSpeedMs * factor).roundToInt().coerceAtLeast(0)
    }

    fun isSpeeding(currentSpeed: Int, limit: Int?, tolerance: Int): Boolean {
        // Strictness: No fallback to urban icon code (already removed from parser)
        val effectiveLimit = limit ?: return false
        if (effectiveLimit <= 0) return false
        return currentSpeed > (effectiveLimit + tolerance)
    }

    fun clearHistory() {
        historySize = 0
        historyIndex = 0
        kalmanFilter.reset()
        lastSpeedKmh = 0f
    }

    private fun updateHistory(speed: Float) {
        speedHistory[historyIndex] = speed
        historyIndex = (historyIndex + 1) % smoothingWindow
        if (historySize < smoothingWindow) historySize++
    }
}

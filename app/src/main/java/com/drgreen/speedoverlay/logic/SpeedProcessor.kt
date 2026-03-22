/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import android.location.Location
import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.KalmanFilter
import java.util.*
import kotlin.math.roundToInt

/**
 * Handles speed smoothing and unit conversion.
 */
class SpeedProcessor(private val smoothingWindow: Int = Config.SMOOTHING_WINDOW) {

    private companion object {
        const val MS_TO_KMH = 3.6f
        const val MS_TO_MPH = 2.23694f
    }

    private val speedHistory = LinkedList<Float>()
    private val kalmanFilter = KalmanFilter()

    /**
     * Smoothes raw GPS speed and converts it to the target unit.
     * Incorporates Kalman filtering (Point 4) and Sensor Fusion.
     */
    fun getSmoothedSpeed(location: Location, useMph: Boolean, isPhysicallyMoving: Boolean = true): Int {
        // --- 🛡 Sensor Fusion & Stillstand Detection (Point 1) ---
        if (!isPhysicallyMoving) {
            speedHistory.clear()
            kalmanFilter.reset()
            return 0
        }

        // --- 📡 GPS Accuracy Filter (Point 3) ---
        // If accuracy is very poor, we don't trust the update
        if (location.hasAccuracy() && location.accuracy > Config.MIN_LOCATION_ACCURACY_METERS) {
            // Keep previous history, but don't add this "noisy" value
            return calculateFinalSpeed(useMph)
        }

        val rawSpeedMs = location.speed
        val safeRawSpeed = if (rawSpeedMs < Config.JITTER_THRESHOLD_MS) 0f else rawSpeedMs

        // --- 📐 Kalman Filtering (Point 4) ---
        val kalmanSpeed = kalmanFilter.filter(safeRawSpeed)

        updateHistory(kalmanSpeed)

        return calculateFinalSpeed(useMph)
    }

    private fun calculateFinalSpeed(useMph: Boolean): Int {
        if (speedHistory.all { it == 0f }) {
            return 0
        }

        val avgSpeedMs = if (speedHistory.isEmpty()) 0f else speedHistory.average().toFloat()

        if (avgSpeedMs < Config.MIN_DISPLAY_SPEED) return 0

        val factor = if (useMph) MS_TO_MPH else MS_TO_KMH
        val result = (avgSpeedMs * factor).roundToInt()

        return if (result < 0) 0 else result
    }

    /**
     * Checks if current speed exceeds limit plus tolerance.
     */
    fun isSpeeding(currentSpeed: Int, limit: Int?, tolerance: Int): Boolean {
        if (limit == null || limit <= 0) return false
        return currentSpeed > limit + tolerance
    }

    fun clearHistory() {
        speedHistory.clear()
        kalmanFilter.reset()
    }

    private fun updateHistory(speed: Float) {
        speedHistory.add(speed)
        if (speedHistory.size > smoothingWindow) {
            speedHistory.removeFirst()
        }
    }
}

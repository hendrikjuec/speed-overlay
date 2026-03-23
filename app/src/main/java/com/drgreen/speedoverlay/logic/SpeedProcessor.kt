/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import android.location.Location
import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.KalmanFilter
import java.util.LinkedList
import kotlin.math.roundToInt

/**
 * Processes speed data, performs smoothing, and handles unit conversion.
 * Uses a Kalman filter and a moving average for a stable display.
 *
 * @property smoothingWindow The number of values for the moving average.
 */
class SpeedProcessor(private val smoothingWindow: Int = Config.SMOOTHING_WINDOW) {

    private companion object {
        const val MS_TO_KMH = 3.6f
        const val MS_TO_MPH = 2.23694f
    }

    private val speedHistory = LinkedList<Float>()
    private val kalmanFilter = KalmanFilter()

    /**
     * The last calculated speed in km/h.
     */
    var lastSpeedKmh: Float = 0f
        private set

    /**
     * Smoothes raw GPS speed and converts it to the target unit.
     * Integrates Kalman filtering and sensor fusion (standstill detection).
     *
     * @param location The current GPS position with speed information.
     * @param useMph True for miles per hour, False for km/h.
     * @param isPhysicallyMoving Indicates if physical movement was detected via sensors.
     * @return The smoothed speed as an integer.
     */
    fun getSmoothedSpeed(location: Location, useMph: Boolean, isPhysicallyMoving: Boolean = true): Int {
        // --- Sensor Fusion & Standstill Detection ---
        if (!isPhysicallyMoving) {
            clearHistory()
            return 0
        }

        // --- GPS Accuracy Filter ---
        // If accuracy is very poor, we don't trust the update.
        if (location.hasAccuracy() && location.accuracy > Config.MIN_LOCATION_ACCURACY_METERS) {
            return calculateFinalSpeed(useMph)
        }

        val rawSpeedMs = location.speed
        val safeRawSpeed = if (rawSpeedMs < Config.JITTER_THRESHOLD_MS) 0f else rawSpeedMs

        // --- Kalman Filtering ---
        val kalmanSpeed = kalmanFilter.filter(safeRawSpeed)

        updateHistory(kalmanSpeed)

        return calculateFinalSpeed(useMph)
    }

    private fun calculateFinalSpeed(useMph: Boolean): Int {
        if (speedHistory.isEmpty() || speedHistory.all { it == 0f }) {
            lastSpeedKmh = 0f
            return 0
        }

        val avgSpeedMs = speedHistory.average().toFloat()
        lastSpeedKmh = avgSpeedMs * MS_TO_KMH

        if (avgSpeedMs < Config.MIN_DISPLAY_SPEED) return 0

        val factor = if (useMph) MS_TO_MPH else MS_TO_KMH
        val result = (avgSpeedMs * factor).roundToInt()

        return result.coerceAtLeast(0)
    }

    /**
     * Checks if the current speed exceeds the limit plus tolerance.
     *
     * @param currentSpeed Current smoothed speed.
     * @param limit The detected speed limit (null if unknown).
     * @param tolerance The user-configured tolerance.
     * @return True if speeding.
     */
    fun isSpeeding(currentSpeed: Int, limit: Int?, tolerance: Int): Boolean {
        if (limit == null) return false
        val effectiveLimit = if (limit == OsmParser.URBAN_ICON_CODE) 50 else limit
        if (effectiveLimit <= 0) return false
        return currentSpeed > effectiveLimit + tolerance
    }

    /**
     * Resets internal state and history.
     */
    fun clearHistory() {
        speedHistory.clear()
        kalmanFilter.reset()
        lastSpeedKmh = 0f
    }

    private fun updateHistory(speed: Float) {
        speedHistory.add(speed)
        if (speedHistory.size > smoothingWindow) {
            speedHistory.removeFirst()
        }
    }
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import com.drgreen.speedoverlay.util.Config
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

    /**
     * Smoothes raw GPS speed and converts it to the target unit.
     * Takes an external motion trigger (e.g. from sensors) into account.
     */
    fun getSmoothedSpeed(rawSpeedMs: Float, useMph: Boolean, isPhysicallyMoving: Boolean = true): Int {
        if (!isPhysicallyMoving) {
            speedHistory.clear()
            return 0
        }

        val safeRawSpeed = if (rawSpeedMs < Config.JITTER_THRESHOLD_MS) 0f else rawSpeedMs

        updateHistory(safeRawSpeed)

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
     * Returns false if limit is 0 (unlimited) or -1 (variable).
     */
    fun isSpeeding(currentSpeed: Int, limit: Int?, tolerance: Int): Boolean {
        if (limit == null || limit <= 0) return false
        return currentSpeed > limit + tolerance
    }

    fun clearHistory() {
        speedHistory.clear()
    }

    private fun updateHistory(speed: Float) {
        speedHistory.add(speed)
        if (speedHistory.size > smoothingWindow) {
            speedHistory.removeFirst()
        }
    }
}

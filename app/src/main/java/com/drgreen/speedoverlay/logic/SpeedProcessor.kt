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
     */
    fun getSmoothedSpeed(rawSpeedMs: Float, useMph: Boolean): Int {
        val safeRawSpeed = if (rawSpeedMs < 0f) 0f else rawSpeedMs

        // Jitter filter
        val filteredSpeed = if (safeRawSpeed < Config.JITTER_THRESHOLD_MS) 0f else safeRawSpeed

        updateHistory(filteredSpeed)

        val avgSpeedMs = if (speedHistory.isEmpty()) 0f else speedHistory.average().toFloat()

        if (avgSpeedMs < Config.MIN_DISPLAY_SPEED) return 0

        val factor = if (useMph) MS_TO_MPH else MS_TO_KMH
        val result = (avgSpeedMs * factor).roundToInt()

        return if (result < 0) 0 else result
    }

    fun isSpeeding(currentSpeed: Int, limit: Int?, tolerance: Int): Boolean {
        return limit?.let { currentSpeed > it + tolerance } ?: false
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

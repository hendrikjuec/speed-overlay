/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

/**
 * Ein einfacher 1D Kalman-Filter zur Glättung der Geschwindigkeit.
 * Er hilft dabei, GPS-Rauschen (Jitter) zu reduzieren, ohne eine zu große Verzögerung einzuführen.
 */
class KalmanFilter(
    private val processNoise: Float = Config.KALMAN_PROCESS_NOISE,
    private val measurementNoise: Float = Config.KALMAN_MEASUREMENT_NOISE
) {
    private var x = 0f // Geschätzte Geschwindigkeit
    private var p = 1f // Schätzfehler-Kovarianz
    private var initialized = false

    /**
     * Verarbeitet einen neuen Messwert und gibt die gefilterte Geschwindigkeit zurück.
     */
    fun filter(measurement: Float): Float {
        if (!initialized) {
            x = measurement
            initialized = true
            return x
        }

        // Prediction Step
        p += processNoise

        // Measurement Update Step (Kalman Gain)
        val k = p / (p + measurementNoise)
        x += k * (measurement - x)
        p *= (1 - k)

        return if (x < 0) 0f else x
    }

    fun reset() {
        x = 0f
        p = 1f
        initialized = false
    }
}

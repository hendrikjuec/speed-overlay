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
 * Verarbeitet Geschwindigkeitsdaten, führt Glättungen durch und übernimmt die Einheitenumrechnung.
 * Nutzt einen Kalman-Filter und einen gleitenden Durchschnitt für eine stabile Anzeige.
 *
 * @property smoothingWindow Die Anzahl der Werte für den gleitenden Durchschnitt.
 */
class SpeedProcessor(private val smoothingWindow: Int = Config.SMOOTHING_WINDOW) {

    private companion object {
        const val MS_TO_KMH = 3.6f
        const val MS_TO_MPH = 2.23694f
    }

    private val speedHistory = LinkedList<Float>()
    private val kalmanFilter = KalmanFilter()

    /**
     * Glättet die rohe GPS-Geschwindigkeit und rechnet sie in die Ziel-Einheit um.
     * Integriert Kalman-Filterung und Sensor-Fusion (Stillstandserkennung).
     *
     * @param location Die aktuelle GPS-Position mit Geschwindigkeitsangabe.
     * @param useMph True für Meilen pro Stunde, False für km/h.
     * @param isPhysicallyMoving Gibt an, ob über Sensoren eine physische Bewegung erkannt wurde.
     * @return Die geglättete Geschwindigkeit als Ganzzahl.
     */
    fun getSmoothedSpeed(location: Location, useMph: Boolean, isPhysicallyMoving: Boolean = true): Int {
        // --- 🛡 Sensor Fusion & Stillstandserkennung ---
        if (!isPhysicallyMoving) {
            speedHistory.clear()
            kalmanFilter.reset()
            return 0
        }

        // --- 📡 GPS Genauigkeitsfilter ---
        // Wenn die Genauigkeit sehr schlecht ist, vertrauen wir dem Update nicht.
        if (location.hasAccuracy() && location.accuracy > Config.MIN_LOCATION_ACCURACY_METERS) {
            return calculateFinalSpeed(useMph)
        }

        val rawSpeedMs = location.speed
        val safeRawSpeed = if (rawSpeedMs < Config.JITTER_THRESHOLD_MS) 0f else rawSpeedMs

        // --- 📐 Kalman-Filterung ---
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
     * Prüft, ob die aktuelle Geschwindigkeit das Limit plus Toleranz überschreitet.
     *
     * @param currentSpeed Aktuelle geglättete Geschwindigkeit.
     * @param limit Das erkannte Tempolimit (null, wenn unbekannt).
     * @param tolerance Die vom Benutzer eingestellte Toleranz.
     * @return True, wenn eine Geschwindigkeitsüberschreitung vorliegt.
     */
    fun isSpeeding(currentSpeed: Int, limit: Int?, tolerance: Int): Boolean {
        if (limit == null || limit <= 0) return false
        return currentSpeed > limit + tolerance
    }

    /**
     * Setzt den internen Status und die Historie zurück.
     */
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

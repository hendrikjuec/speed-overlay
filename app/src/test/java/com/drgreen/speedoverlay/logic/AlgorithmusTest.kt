/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import com.drgreen.speedoverlay.util.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

/**
 * Validiert die Kern-Algorithmen für Smart Road Filtering und Junction Mode.
 * Diese Tests simulieren die Entscheidungslogik des SpeedRepository.
 */
class AlgorithmusTest {

    @Test
    fun testCalculateTypeScore() {
        // High Speed (> 80 km/h) -> Autobahn bevorzugt
        val highSpeed = 120.0f
        assertEquals(1.0, calculateTypeScore("motorway", highSpeed), 0.01)
        assertEquals(0.1, calculateTypeScore("residential", highSpeed), 0.01)

        // Low Speed (< 30 km/h) -> Wohngebiete bevorzugt
        val lowSpeed = 20.0f
        assertEquals(1.0, calculateTypeScore("residential", lowSpeed), 0.01)
        assertEquals(0.4, calculateTypeScore("motorway", lowSpeed), 0.01)
    }

    @Test
    fun testJunctionModeTolerance() {
        val headingUser = 90.0f // Osten
        val wayHeadingNorth = 0.0f // Norden (90 Grad Differenz)

        // Normal Mode (35 Grad Toleranz) -> Darf nicht matchen
        val diff = abs(headingUser - wayHeadingNorth)
        val normalizedDiff = if (diff > 180) 360 - diff else diff
        assertTrue(normalizedDiff > Config.HEADING_TOLERANCE_DEG)

        // Junction Mode (65 Grad Toleranz) -> Matcht immer noch nicht,
        // aber wir prüfen hier die Schwellenwert-Logik
        assertTrue(Config.JUNCTION_HEADING_TOLERANCE_DEG > Config.HEADING_TOLERANCE_DEG)
    }

    @Test
    fun testDynamicRadiusCalculation() {
        // Formel: Radius = 100m + (Speed_m/s * 12s)

        // 50 km/h = 13.88 m/s -> Radius = 100 + 166.6 = 266m
        val speed50 = 50f
        val radius50 = (Config.BASE_SEARCH_RADIUS_METERS + (speed50 / 3.6f * Config.LOOKAHEAD_TIME_SECONDS))
        assertEquals(266.66, radius50, 0.1)

        // 130 km/h = 36.11 m/s -> Radius = 100 + 433.3 = 533m (Limit 600m)
        val speed130 = 130f
        val radius130 = (Config.BASE_SEARCH_RADIUS_METERS + (speed130 / 3.6f * Config.LOOKAHEAD_TIME_SECONDS))
        assertTrue(radius130 > 500)
        assertTrue(radius130 <= Config.MAX_SEARCH_RADIUS_METERS)
    }

    // --- Hilfsfunktionen Kopie aus SpeedRepository für isolierten Test ---
    private fun calculateTypeScore(type: String, speedKmh: Float): Double {
        return when {
            speedKmh > Config.HIGH_SPEED_THRESHOLD_KMH -> {
                when (type) {
                    "motorway", "motorway_link" -> 1.0
                    "trunk", "trunk_link" -> 0.9
                    "primary" -> 0.7
                    "secondary" -> 0.3
                    else -> 0.1
                }
            }
            speedKmh < Config.LOW_SPEED_THRESHOLD_KMH -> {
                when (type) {
                    "residential", "living_street" -> 1.0
                    "service", "unclassified" -> 0.8
                    "tertiary", "secondary" -> 0.6
                    else -> 0.4
                }
            }
            else -> 0.7
        }
    }
}

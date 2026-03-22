/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import com.drgreen.speedoverlay.util.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmusTest {

    @Test
    fun testHeadingLogic() {
        val carHeading = 45f
        val wayHeading = 50f

        val diff = Math.abs(carHeading - wayHeading)
        val normalizedDiff = if (diff > 180) 360 - diff else diff

        assertTrue(normalizedDiff <= Config.HEADING_TOLERANCE_DEG)

        // Gegenbeispiel
        val wayHeadingWrong = 100f
        val diffWrong = Math.abs(carHeading - wayHeadingWrong)
        val normalizedDiffWrong = if (diffWrong > 180) 360 - diffWrong else diffWrong
        assertTrue(normalizedDiffWrong > Config.HEADING_TOLERANCE_DEG)

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
        assertEquals(266.66, radius50.toDouble(), 0.1)

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
                    else -> 0.1
                }
            }
            else -> 0.5
        }
    }
}

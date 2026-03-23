/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgorithmusTest {

    private companion object {
        const val MS_TO_KMH = 3.6f
    }

    @Test
    fun testHeadingLogicUsingGeoUtils() {
        val carHeading = 45f
        val wayHeading = 50f

        val diff = GeoUtils.getHeadingDifference(carHeading, wayHeading)

        assertTrue("Heading difference should be within tolerance", diff <= Config.HEADING_TOLERANCE_DEG)

        // Gegenbeispiel
        val wayHeadingWrong = 100f
        val diffWrong = GeoUtils.getHeadingDifference(carHeading, wayHeadingWrong)
        assertTrue("Heading difference should be outside tolerance", diffWrong > Config.HEADING_TOLERANCE_DEG)
    }

    @Test
    fun testDynamicRadiusCalculation() {
        // Formel: Radius = Base + (Speed_m/s * Lookahead)

        // 50 km/h = 13.88 m/s -> Radius = 150 + (13.88 * 20) = 150 + 277 = 427m (basierend auf aktuellen Config Werten)
        // Hinweis: Die Werte in Config wurden im vorigen Schritt erhöht.
        val speed50 = 50f
        val speedMs = speed50 / MS_TO_KMH
        val radius50 = (Config.BASE_SEARCH_RADIUS_METERS + (speedMs * Config.LOOKAHEAD_TIME_SECONDS))

        val expected = 150 + (13.888889f * 20) // 427.77
        assertEquals(expected.toDouble(), radius50.toDouble(), 0.1)

        // 130 km/h
        val speed130 = 130f
        val radius130 = (Config.BASE_SEARCH_RADIUS_METERS + (speed130 / MS_TO_KMH * Config.LOOKAHEAD_TIME_SECONDS))
        assertTrue(radius130 > 500)
        assertTrue(radius130 <= Config.MAX_SEARCH_RADIUS_METERS)
    }

    @Test
    fun testConfigInvariants() {
        // Sicherstellen, dass die Junction-Toleranz immer größer ist
        assertTrue(Config.JUNCTION_HEADING_TOLERANCE_DEG > Config.HEADING_TOLERANCE_DEG)

        // Sicherstellen, dass Radien plausibel sind
        assertTrue(Config.BASE_SEARCH_RADIUS_METERS < Config.MAX_SEARCH_RADIUS_METERS)
    }
}

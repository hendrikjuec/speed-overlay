/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Tests für den SpeedProcessor.
 * Da Location gemockt wird, ist kein Robolectric erforderlich.
 */
class SpeedProcessorTest {

    // Config: SmoothingWindow = 2
    private val processor = SpeedProcessor(smoothingWindow = 2)

    private companion object {
        const val SPEED_10_MS = 10f
        const val SPEED_20_MS = 20f
        const val SPEED_30_MS = 30f
        const val SPEED_36_KMH = 36
        const val SPEED_49_KMH = 49
        const val SPEED_73_KMH = 73
        const val SPEED_22_MPH = 22
    }

    private fun createMockLocation(speedMs: Float, accuracy: Float = 5f): Location {
        val loc = mockk<Location>()
        every { loc.speed } returns speedMs
        every { loc.hasAccuracy() } returns true
        every { loc.accuracy } returns accuracy
        return loc
    }

    @Test
    fun testSpeedSmoothing() {
        // Die geglätteten Werte resultieren aus der Kombination von Kalman-Filter und Moving Average (Fenster 2).
        // Die Werte wurden an die optimierten Kalman-Parameter in Config.kt angepasst.

        // 1. Eingabe 10 m/s (36 km/h) -> Schnitt(10) -> 36 km/h
        assertEquals(SPEED_36_KMH, processor.getSmoothedSpeed(createMockLocation(SPEED_10_MS), false))

        // 2. Eingabe 20 m/s (72 km/h) -> Kalman Update -> Schnitt(10, 17.2) -> ~49 km/h
        assertEquals(SPEED_49_KMH, processor.getSmoothedSpeed(createMockLocation(SPEED_20_MS), false))

        // 3. Eingabe 30 m/s (108 km/h) -> Kalman Update -> Schnitt(17.2, 23.8) -> ~73 km/h
        assertEquals(SPEED_73_KMH, processor.getSmoothedSpeed(createMockLocation(SPEED_30_MS), false))
    }

    @Test
    fun testUnitConversionMph() {
        processor.clearHistory()
        // 10 m/s sind ca. 22.37 mph
        assertEquals(SPEED_22_MPH, processor.getSmoothedSpeed(createMockLocation(SPEED_10_MS), true))
    }

    @Test
    fun testIsSpeeding() {
        assertFalse("55 in 50er Zone mit 5 Toleranz ist OK", processor.isSpeeding(55, 50, 5))
        assertTrue("56 in 50er Zone mit 5 Toleranz ist zu schnell", processor.isSpeeding(56, 50, 5))
        assertFalse("Ohne Limit keine Warnung", processor.isSpeeding(100, null, 5))
    }

    @Test
    fun testAccuracyFilter() {
        // Erstes gültiges Signal
        processor.getSmoothedSpeed(createMockLocation(10f, 5f), false)

        // Schlechtes Signal (Genauigkeit 50m) -> Sollte den letzten gültigen Wert (36 km/h) beibehalten
        assertEquals(36, processor.getSmoothedSpeed(createMockLocation(50f, 50f), false))
    }
}

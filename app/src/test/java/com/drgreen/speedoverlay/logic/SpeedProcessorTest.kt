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

    private val processor = SpeedProcessor(smoothingWindow = 3)

    private companion object {
        const val SPEED_10_MS = 10f
        const val SPEED_20_MS = 20f
        const val SPEED_30_MS = 30f
        const val SPEED_36_KMH = 36
        const val SPEED_46_KMH = 46
        const val SPEED_57_KMH = 57
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
        // Die geglätteten Werte resultieren aus der Kombination von Kalman-Filter und Moving Average (Fenster 3).

        // 1. Eingabe 10 m/s (36 km/h) -> Kalman initialisiert auf 10 -> Schnitt(10) -> 36 km/h
        assertEquals(SPEED_36_KMH, processor.getSmoothedSpeed(createMockLocation(SPEED_10_MS), false))

        // 2. Eingabe 20 m/s (72 km/h) -> Kalman Update auf ~15.8 -> Schnitt(10, 15.8) -> ~46 km/h
        assertEquals(SPEED_46_KMH, processor.getSmoothedSpeed(createMockLocation(SPEED_20_MS), false))

        // 3. Eingabe 30 m/s (108 km/h) -> Kalman Update auf ~21.8 -> Schnitt(10, 15.8, 21.8) -> ~57 km/h
        assertEquals(SPEED_57_KMH, processor.getSmoothedSpeed(createMockLocation(SPEED_30_MS), false))
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
    fun testJitterFilter() {
        // Minimale Geschwindigkeiten (Jitter) sollten als 0 gewertet werden
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(0.2f), false))

        // Echte Bewegung sollte erkannt werden
        assertTrue(processor.getSmoothedSpeed(createMockLocation(2.0f), false) > 0)
    }

    @Test
    fun testMotionDetectorIntegration() {
        // Wenn der MotionDetector STILL meldet, muss die Geschwindigkeit 0 sein, egal was GPS sagt
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(100f), false, isPhysicallyMoving = false))

        // Wenn er sich wieder bewegt, sollte die Historie zurückgesetzt sein
        processor.clearHistory()
        assertTrue(processor.getSmoothedSpeed(createMockLocation(10f), false, isPhysicallyMoving = true) > 0)
    }

    @Test
    fun testClearHistory() {
        processor.getSmoothedSpeed(createMockLocation(100f), false)
        processor.clearHistory()
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(0f), false))
    }

    @Test
    fun testExtremeValues() {
        // Schallgeschwindigkeit (343 m/s)
        assertEquals(1235, processor.getSmoothedSpeed(createMockLocation(343f), false))

        processor.clearHistory()
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(-10f), false))

        processor.clearHistory()
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(0.1f), false))
    }

    @Test
    fun testAccuracyFilter() {
        // Erstes gültiges Signal
        processor.getSmoothedSpeed(createMockLocation(10f, 5f), false)

        // Schlechtes Signal (Genauigkeit 50m) -> Sollte den letzten gültigen Wert (36 km/h) beibehalten
        assertEquals(36, processor.getSmoothedSpeed(createMockLocation(50f, 50f), false))
    }
}

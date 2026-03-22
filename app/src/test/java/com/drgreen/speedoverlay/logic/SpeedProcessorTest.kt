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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeedProcessorTest {
    private val processor = SpeedProcessor(smoothingWindow = 3)

    private fun createMockLocation(speedMs: Float, accuracy: Float = 5f): Location {
        val loc = mockk<Location>()
        every { loc.speed } returns speedMs
        every { loc.hasAccuracy() } returns true
        every { loc.accuracy } returns accuracy
        return loc
    }

    @Test
    fun testSpeedSmoothing() {
        // Expected values are influenced by Kalman Filter + Moving Average (Window 3)
        // 1. Input 10 m/s (36 km/h) -> Kalman init to 10 -> Avg(10) -> 36 km/h
        assertEquals(36, processor.getSmoothedSpeed(createMockLocation(10f), false))

        // 2. Input 20 m/s (72 km/h) -> Kalman update ~15.8 -> Avg(10, 15.8) -> ~46 km/h
        assertEquals(46, processor.getSmoothedSpeed(createMockLocation(20f), false))

        // 3. Input 30 m/s (108 km/h) -> Kalman update ~21.8 -> Avg(10, 15.8, 21.8) -> ~57 km/h
        assertEquals(57, processor.getSmoothedSpeed(createMockLocation(30f), false))

        // Test MPH: 10 m/s ~ 22 mph
        processor.clearHistory()
        assertEquals(22, processor.getSmoothedSpeed(createMockLocation(10f), true))
    }

    @Test
    fun testIsSpeeding() {
        assertFalse(processor.isSpeeding(55, 50, 5))
        assertTrue(processor.isSpeeding(56, 50, 5))
        assertFalse(processor.isSpeeding(100, null, 5))
    }

    @Test
    fun testJitterFilter() {
        // Jitter should be 0 based on current Config
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(0.2f), false))

        // Real movement should trigger
        assertTrue(processor.getSmoothedSpeed(createMockLocation(2.0f), false) > 0)
    }

    @Test
    fun testMotionDetectorIntegration() {
        // Test: MotionDetector reports STILL (isMoving = false)
        // Must force 0 km/h even if GPS reports movement
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(100f), false, isPhysicallyMoving = false))

        // Test: MotionDetector reports MOVING (isMoving = true)
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
        // Speed of sound test
        assertEquals(1235, processor.getSmoothedSpeed(createMockLocation(343f), false))

        processor.clearHistory()
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(-10f), false))

        processor.clearHistory()
        assertEquals(0, processor.getSmoothedSpeed(createMockLocation(0.1f), false))
    }

    @Test
    fun testAccuracyFilter() {
        // First good value: 10 m/s (36 km/h)
        processor.getSmoothedSpeed(createMockLocation(10f, 5f), false)

        // Bad accuracy value (50m > 25m threshold) - should return previous valid speed
        assertEquals(36, processor.getSmoothedSpeed(createMockLocation(50f, 50f), false))
    }
}

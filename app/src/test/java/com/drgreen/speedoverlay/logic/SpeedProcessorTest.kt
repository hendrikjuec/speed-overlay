/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedProcessorTest {
    private val processor = SpeedProcessor(smoothingWindow = 3)

    @Test
    fun testSpeedSmoothing() {
        // 10 m/s = 36 km/h
        assertEquals(36, processor.getSmoothedSpeed(10f, false))
        // (10 + 20) / 2 = 15 m/s = 54 km/h
        assertEquals(54, processor.getSmoothedSpeed(20f, false))
        // (10 + 20 + 30) / 3 = 20 m/s = 72 km/h
        assertEquals(72, processor.getSmoothedSpeed(30f, false))

        // Test MPH: 10 m/s ~ 22 mph
        processor.clearHistory()
        assertEquals(22, processor.getSmoothedSpeed(10f, true))
    }

    @Test
    fun testIsSpeeding() {
        assertFalse(processor.isSpeeding(55, 50, 5))
        assertTrue(processor.isSpeeding(56, 50, 5))
        assertFalse(processor.isSpeeding(100, null, 5))
    }

    @Test
    fun testJitterFilter() {
        // Jitter (desk movement) should be 0
        assertEquals(0, processor.getSmoothedSpeed(0.2f, false))
        assertEquals(0, processor.getSmoothedSpeed(0.4f, false))

        // Real movement should trigger
        assertTrue(processor.getSmoothedSpeed(1.0f, false) > 0)
    }

    @Test
    fun testClearHistory() {
        processor.getSmoothedSpeed(100f, false)
        processor.clearHistory()
        assertEquals(0, processor.getSmoothedSpeed(0f, false))
    }

    @Test
    fun testExtremeValues() {
        // Test supersonic speed (sanity check)
        assertEquals(1235, processor.getSmoothedSpeed(343f, false)) // Speed of sound in km/h

        processor.clearHistory()
        // Test negative speed (should be treated as jitter/0)
        assertEquals(0, processor.getSmoothedSpeed(-10f, false))

        processor.clearHistory()
        // Test very high jitter
        assertEquals(0, processor.getSmoothedSpeed(0.49f, false))
    }
}

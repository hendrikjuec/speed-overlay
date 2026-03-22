/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KalmanFilterTest {

    @Test
    fun testKalmanConvergence() {
        val filter = KalmanFilter(processNoise = 0.1f, measurementNoise = 0.5f)

        // Konstante Eingabe 50.0
        var lastResult = 0f
        repeat(20) {
            lastResult = filter.filter(50f)
        }

        // Muss gegen 50 konvergieren (Toleranz 1.0)
        assertEquals(50f, lastResult, 1.0f)
    }

    @Test
    fun testNoiseReduction() {
        val filter = KalmanFilter(processNoise = 0.05f, measurementNoise = 1.0f)

        val first = filter.filter(10f)
        val second = filter.filter(100f) // Starker Sprung

        // Der Filter sollte den Sprung massiv dämpfen
        assertTrue("Filter sollte Sprung dämpfen: $second", second < 60f)
    }
}

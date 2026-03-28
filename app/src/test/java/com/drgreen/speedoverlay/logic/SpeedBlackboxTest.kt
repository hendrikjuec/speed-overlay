/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Blackbox-Tests für die Kernlogik des Speed-Overlays.
 */
class SpeedBlackboxTest {

    private val parser = OsmParser()
    private val processor = SpeedProcessor(smoothingWindow = 5)

    @Test
    fun `Testfall Tempolimit Erkennung - Verschiedene Laender und Formate`() {
        val testCases = listOf(
            mapOf("maxspeed" to "50") to 50,
            mapOf("maxspeed" to "120 km/h") to 120,
            mapOf("maxspeed" to "none") to 0,
            mapOf("maxspeed" to "unlimited") to 0,
            mapOf("maxspeed" to "") to null,
            null to null
        )

        testCases.forEach { (input, expected) ->
            val result = parser.parseSpeedLimit(input)
            assertEquals("Fehler bei Input: $input", expected, result.limit)
        }
    }

    @Test
    fun `Testfall Warnlogik - Toleranz-Grenzwerte`() {
        val limit = 50
        val tolerance = 5

        val scenarios = listOf(
            Triple(50, limit, tolerance) to false,
            Triple(55, limit, tolerance) to false,
            Triple(56, limit, tolerance) to true,
            Triple(40, limit, tolerance) to false,
            Triple(100, null as Int?, tolerance) to false,
            Triple(200, 0, tolerance) to false // Unbegrenzt -> Keine Warnung
        )

        scenarios.forEach { (params, shouldWarn) ->
            val (speed, l, t) = params
            assertEquals(
                "Fehler bei Speed: $speed, Limit: $l, Tol: $t",
                shouldWarn,
                processor.isSpeeding(speed, l, t)
            )
        }
    }
}

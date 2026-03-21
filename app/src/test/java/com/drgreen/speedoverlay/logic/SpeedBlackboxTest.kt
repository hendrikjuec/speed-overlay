/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Blackbox-Tests für die Kernlogik des Speed-Overlays.
 */
class SpeedBlackboxTest {

    private val parser = OsmParser()
    private val processor = SpeedProcessor(smoothingWindow = 5)

    @Test
    fun `Testfall Tempolimit Erkennung - Verschiedene Länder und Formate`() {
        val testCases = listOf(
            mapOf("maxspeed" to "50") to 50,
            mapOf("maxspeed" to "120 km/h") to 120,
            mapOf("maxspeed" to "DE:urban") to 50,
            mapOf("maxspeed" to "DE:rural") to 100,
            mapOf("maxspeed" to "DE:motorway") to 0,
            mapOf("maxspeed" to "walk") to 5,
            mapOf("highway" to "living_street") to 7,
            mapOf("maxspeed" to "none") to 0,
            mapOf("maxspeed" to "signals") to -1,
            mapOf("maxspeed" to "") to null,
            null to null
        )

        testCases.forEach { (input, expected) ->
            assertEquals("Fehler bei Input: $input", expected, parser.parseSpeedLimit(input))
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
            Triple(100, null, tolerance) to false,
            Triple(200, 0, tolerance) to false, // Unbegrenzt -> Keine Warnung
            Triple(200, -1, tolerance) to false  // Variabel -> Keine Warnung
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

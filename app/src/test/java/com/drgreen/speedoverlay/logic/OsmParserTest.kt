/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmParserTest {
    private val parser = OsmParser()

    @Test
    fun `test explicit maxspeed tag`() {
        val tags = mapOf("maxspeed" to "30")
        val result = parser.parseSpeedLimit(tags)
        assertEquals(30, result.limit)
        assertTrue(result.isConfidenceHigh)
    }

    @Test
    fun `test explicit none or unlimited`() {
        val resNone = parser.parseSpeedLimit(mapOf("maxspeed" to "none"))
        assertEquals(0, resNone.limit)
        assertTrue(resNone.isConfidenceHigh)

        val resUnl = parser.parseSpeedLimit(mapOf("maxspeed" to "unlimited"))
        assertEquals(0, resUnl.limit)
        assertTrue(resUnl.isConfidenceHigh)
    }

    @Test
    fun `test implicit motorway by country`() {
        // Deutschland: Unbegrenzt
        val resDe = parser.parseSpeedLimit(mapOf("highway" to "motorway", "zone:maxspeed" to "DE:motorway"))
        assertEquals(0, resDe.limit)
        assertTrue(resDe.isConfidenceHigh) // Zonen-Tag ist sicher

        // Frankreich: 130
        val resFr = parser.parseSpeedLimit(mapOf("highway" to "motorway", "zone:maxspeed" to "FR:motorway"))
        assertEquals(130, resFr.limit)
        assertTrue(resFr.isConfidenceHigh)
    }

    @Test
    fun `test implicit rural roads (France special case)`() {
        // Standard EU (Default fallback to DE): 100
        val resDef = parser.parseSpeedLimit(mapOf("highway" to "primary"))
        assertEquals(100, resDef.limit)
        assertFalse(resDef.isConfidenceHigh) // Schätzung ist unsicher

        // Frankreich: 80
        val resFr = parser.parseSpeedLimit(mapOf("highway" to "primary", "zone:maxspeed" to "FR:rural"))
        assertEquals(80, resFr.limit)
        assertTrue(resFr.isConfidenceHigh)
    }

    @Test
    fun `test implicit residential urban`() {
        val tags = mapOf("highway" to "residential", "lit" to "yes")
        val result = parser.parseSpeedLimit(tags)
        assertEquals(50, result.limit)
        assertFalse(result.isConfidenceHigh)
    }

    @Test
    fun `test living street by country`() {
        // Deutschland: 7
        val resDe = parser.parseSpeedLimit(mapOf("highway" to "living_street", "zone:maxspeed" to "DE:urban"))
        assertEquals(7, resDe.limit)
        assertTrue(resDe.isConfidenceHigh)

        // Österreich: 5
        val resAt = parser.parseSpeedLimit(mapOf("highway" to "living_street", "zone:maxspeed" to "AT:urban"))
        assertEquals(5, resAt.limit)
        assertTrue(resAt.isConfidenceHigh)
    }

    @Test
    fun `test zone maxspeed priority`() {
        // Residential road with zone tag DE:30
        val tags = mapOf("highway" to "residential", "zone:maxspeed" to "DE:30")
        val result = parser.parseSpeedLimit(tags)
        assertEquals(30, result.limit)
        assertTrue(result.isConfidenceHigh)
    }

    @Test
    fun `test confidence mapping`() {
        // Schild: Hoch
        assertTrue(parser.parseSpeedLimit(mapOf("maxspeed" to "50")).isConfidenceHigh)

        // Zone: Hoch
        assertTrue(parser.parseSpeedLimit(mapOf("zone:maxspeed" to "DE:30")).isConfidenceHigh)

        // Highway-Fallback: Niedrig
        assertFalse(parser.parseSpeedLimit(mapOf("highway" to "primary")).isConfidenceHigh)
    }
}

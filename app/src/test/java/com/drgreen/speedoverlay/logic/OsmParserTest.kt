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
    fun `test signals or walk`() {
        val resSignals = parser.parseSpeedLimit(mapOf("maxspeed" to "signals"))
        assertEquals(-1, resSignals.limit)
        assertTrue(resSignals.isConfidenceHigh)

        val resWalk = parser.parseSpeedLimit(mapOf("maxspeed" to "walk"))
        assertEquals(7, resWalk.limit)
        assertTrue(resWalk.isConfidenceHigh)
    }

    @Test
    fun `test country specific urban rural tags`() {
        val resUrban = parser.parseSpeedLimit(mapOf("maxspeed" to "DE:urban"))
        assertEquals(50, resUrban.limit)
        assertTrue(resUrban.isConfidenceHigh)

        val resRural = parser.parseSpeedLimit(mapOf("maxspeed" to "DE:rural"))
        assertEquals(100, resRural.limit)
        assertTrue(resRural.isConfidenceHigh)

        val resMotorway = parser.parseSpeedLimit(mapOf("maxspeed" to "DE:motorway"))
        assertEquals(0, resMotorway.limit)
        assertTrue(resMotorway.isConfidenceHigh)
    }

    @Test
    fun `test ambiguous data - show nothing`() {
        // Multiple speeds in one tag -> Ambiguous -> null
        val resAmbiguous = parser.parseSpeedLimit(mapOf("maxspeed" to "30; 50"))
        assertNull(resAmbiguous.limit)
        assertFalse(resAmbiguous.isConfidenceHigh)

        val resMixed = parser.parseSpeedLimit(mapOf("maxspeed" to "30 mph; 50 km/h"))
        assertNull(resMixed.limit)
        assertFalse(resMixed.isConfidenceHigh)
    }

    @Test
    fun `test missing maxspeed - show nothing`() {
        // Even if we know it's a motorway, we show nothing without the maxspeed tag
        val resHighway = parser.parseSpeedLimit(mapOf("highway" to "motorway"))
        assertNull(resHighway.limit)
        assertFalse(resHighway.isConfidenceHigh)

        val resZone = parser.parseSpeedLimit(mapOf("zone:maxspeed" to "30"))
        assertNull(resZone.limit)
        assertFalse(resZone.isConfidenceHigh)
    }

    @Test
    fun `test numeric filtering`() {
        val resUnit = parser.parseSpeedLimit(mapOf("maxspeed" to "50 km/h"))
        assertEquals(50, resUnit.limit)
        assertTrue(resUnit.isConfidenceHigh)
    }
}

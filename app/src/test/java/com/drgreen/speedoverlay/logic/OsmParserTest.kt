/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OsmParserTest {
    private val parser = OsmParser()

    @Test
    fun testParseMaxSpeed() {
        assertEquals(50, parser.parseSpeedLimit(mapOf("maxspeed" to "50")))
        assertEquals(120, parser.parseSpeedLimit(mapOf("maxspeed" to "120")))
        assertEquals(30, parser.parseSpeedLimit(mapOf("maxspeed" to "30 mph")))
    }

    @Test
    fun testParseImplicitLimits() {
        assertEquals(7, parser.parseSpeedLimit(mapOf("highway" to "living_street")))
        assertEquals(5, parser.parseSpeedLimit(mapOf("highway" to "pedestrian")))
        assertNull(parser.parseSpeedLimit(mapOf("highway" to "residential")))
    }

    @Test
    fun testParseCountrySpecific() {
        assertEquals(50, parser.parseSpeedLimit(mapOf("maxspeed" to "DE:urban")))
        assertEquals(100, parser.parseSpeedLimit(mapOf("maxspeed" to "DE:rural")))
        assertNull(parser.parseSpeedLimit(mapOf("maxspeed" to "DE:motorway"))) // 130 is advisory, so it returns null
    }

    @Test
    fun testMalformedTags() {
        assertNull(parser.parseSpeedLimit(null))
        assertNull(parser.parseSpeedLimit(emptyMap()))
        assertEquals(50, parser.parseSpeedLimit(mapOf("maxspeed" to "50; 30"))) // Should pick first
        assertNull(parser.parseSpeedLimit(mapOf("maxspeed" to "unknown")))
    }
}

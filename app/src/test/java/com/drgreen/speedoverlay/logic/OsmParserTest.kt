/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmParserTest {
    private val parser = OsmParser()

    @Test
    fun testParseMaxSpeed() {
        assertEquals(50, parser.parseSpeedLimit(mapOf("maxspeed" to "50")))
        assertEquals(120, parser.parseSpeedLimit(mapOf("maxspeed" to "120")))
        assertEquals(30, parser.parseSpeedLimit(mapOf("maxspeed" to "30 mph")))

        // Unbegrenzt & Variabel
        assertEquals(0, parser.parseSpeedLimit(mapOf("maxspeed" to "none")))
        assertEquals(-1, parser.parseSpeedLimit(mapOf("maxspeed" to "signals")))
    }

    @Test
    fun testParseImplicitLimits() {
        assertEquals(7, parser.parseSpeedLimit(mapOf("highway" to "living_street")))
        assertEquals(5, parser.parseSpeedLimit(mapOf("highway" to "pedestrian")))
    }

    @Test
    fun testParseAdditionalInfo() {
        val tags = mapOf(
            "hazard" to "yes",
            "highway" to "speed_camera",
            "amenity" to "school"
        )

        val infoWithCameras = parser.parseAdditionalInfo(tags, true)
        assertTrue(infoWithCameras.contains("Gefahr"))
        assertTrue(infoWithCameras.contains("Blitzer"))
        assertTrue(infoWithCameras.contains("Schule"))

        val infoWithoutCameras = parser.parseAdditionalInfo(tags, false)
        assertTrue(infoWithoutCameras.contains("Gefahr"))
        assertTrue(infoWithoutCameras.contains("Schule"))
        assertEquals(2, infoWithoutCameras.size)
    }

    @Test
    fun testParseCountrySpecific() {
        assertEquals(50, parser.parseSpeedLimit(mapOf("maxspeed" to "DE:urban")))
        assertEquals(0, parser.parseSpeedLimit(mapOf("maxspeed" to "DE:motorway"))) // Jetzt 0 (unbegrenzt)
    }
}

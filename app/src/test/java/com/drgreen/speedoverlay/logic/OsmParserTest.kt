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
        // Deutschland: Unbegrenzt (0)
        val resDe = parser.parseSpeedLimit(mapOf("highway" to "motorway"), "DE")
        assertEquals(0, resDe.limit)
        assertTrue(resDe.isConfidenceHigh)

        // Frankreich: 130
        val resFr = parser.parseSpeedLimit(mapOf("highway" to "motorway"), "FR")
        assertEquals(130, resFr.limit)
        assertTrue(resFr.isConfidenceHigh)
    }

    @Test
    fun `test implicit rural roads (No Info Policy)`() {
        // Gemäß neuer Policy: Ohne Schild auf Landstraßen -> null
        val resDef = parser.parseSpeedLimit(mapOf("highway" to "primary"), "DE")
        assertNull(resDef.limit)
        assertFalse(resDef.isConfidenceHigh)
    }

    @Test
    fun `test implicit residential urban (Icon Code)`() {
        val tags = mapOf("highway" to "residential")
        val result = parser.parseSpeedLimit(tags)
        // Gemäß neuer Policy: Innerorts -> URBAN_ICON_CODE
        assertEquals(OsmParser.URBAN_ICON_CODE, result.limit)
        assertFalse(result.isConfidenceHigh)
    }

    @Test
    fun `test zone maxspeed priority`() {
        // Schild/Zone hat Vorrang vor Highway-Typ
        val tags = mapOf("highway" to "residential", "zone:maxspeed" to "DE:30")
        val result = parser.parseSpeedLimit(tags)
        assertEquals(30, result.limit)
        assertTrue(result.isConfidenceHigh)
    }

    @Test
    fun `test source maxspeed parsing`() {
        val tagsNumeric = mapOf("source:maxspeed" to "DE:50")
        assertEquals(50, parser.parseSpeedLimit(tagsNumeric).limit)
    }

    @Test
    fun `test additional info parsing`() {
        val tags = mapOf(
            "hazard" to "yes",
            "highway" to "speed_camera",
            "amenity" to "school"
        )
        val info = parser.parseAdditionalInfo(tags, showSpeedCamerasEnabled = true)
        assertTrue(info.contains(OsmParser.INFO_HAZARD))
        assertTrue(info.contains(OsmParser.INFO_CAMERA))
        assertTrue(info.contains(OsmParser.INFO_SCHOOL))

        val infoNoCam = parser.parseAdditionalInfo(tags, showSpeedCamerasEnabled = false)
        assertFalse(infoNoCam.contains(OsmParser.INFO_CAMERA))
    }
}

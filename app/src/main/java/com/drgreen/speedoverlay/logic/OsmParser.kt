/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

/**
 * Analyzes OpenStreetMap data to determine speed limits.
 * Data source priority:
 * 1. Explicit 'maxspeed' tag (traffic signs) -> 100% Reliable
 * 2. 'zone:maxspeed' or 'source:maxspeed' (zone signage) -> 100% Reliable
 * 3. Highway standards (country-specific) -> Acceptable
 * 4. Urban detection (city icon instead of number)
 * 5. Rural/Outside (No sign -> No info)
 */
class OsmParser {

    companion object {
        const val URBAN_ICON_CODE = -2
        const val INFO_HAZARD = "Hazard"
        const val INFO_CAMERA = "Camera"
        const val INFO_SCHOOL = "School"
    }

    data class SpeedResult(
        val limit: Int?,
        val isConfidenceHigh: Boolean
    )

    private data class CountryDefaults(
        val urban: Int = 50,
        val rural: Int = 100,
        val trunk: Int = 100,
        val motorway: Int = 130
    )

    private val countryMap = mapOf(
        "AT" to CountryDefaults(rural = 100, motorway = 130),
        "DE" to CountryDefaults(rural = 100, motorway = 0), // 0 = Unlimited
        "CH" to CountryDefaults(rural = 80, motorway = 120),
        "IT" to CountryDefaults(rural = 90, motorway = 130),
        "FR" to CountryDefaults(rural = 80, motorway = 130),
        "NL" to CountryDefaults(rural = 80, motorway = 100)
    )

    /**
     * Parses the speed limit from OSM tags.
     * @param tags The metadata of the road segment.
     * @param countryCode The country code for determining defaults (e.g., "DE").
     */
    fun parseSpeedLimit(tags: Map<String, String>?, countryCode: String = "DE"): SpeedResult {
        if (tags == null) return SpeedResult(null, false)

        // 1. PRIORITY: Explicit signs (maxspeed tag)
        val maxspeed = tags["maxspeed"]
        if (!maxspeed.isNullOrBlank()) {
            val normalized = maxspeed.lowercase().trim()
            if (normalized == "none" || normalized == "unlimited") return SpeedResult(0, true)
            if (normalized == "signals") return SpeedResult(-1, true)

            // Handle common country prefixes like DE:urban
            if (normalized.startsWith("de:")) {
                val value = when (normalized.substring(3)) {
                    "urban" -> 50
                    "rural" -> 100
                    "motorway" -> 0
                    else -> parseNumericOrNull(normalized)
                }
                return SpeedResult(value, true)
            }

            val parsed = parseMaxSpeedTag(normalized)
            if (parsed != null) return SpeedResult(parsed, true)
        }

        // 2. PRIORITY: Zone signage
        val zoneTag = tags["zone:maxspeed"] ?: tags["source:maxspeed"]
        if (!zoneTag.isNullOrBlank()) {
            val normalizedZone = zoneTag.lowercase()
            if (normalizedZone.contains("urban")) return SpeedResult(50, true)
            if (normalizedZone.contains("rural")) return SpeedResult(100, true)

            val numericInZone = zoneTag.split(':', ' ', ';').firstOrNull { it.any { c -> c.isDigit() } }
                ?.filter { it.isDigit() }?.toIntOrNull()
            if (numericInZone != null) return SpeedResult(numericInZone, true)
        }

        // 3. PRIORITY: Special cases (Motorway vs Urban vs Rural)
        val highway = tags["highway"] ?: return SpeedResult(null, false)

        return when (highway) {
            "motorway" -> {
                val motorwayDefault = countryMap[countryCode]?.motorway ?: 130
                SpeedResult(motorwayDefault, true)
            }
            "residential", "living_street" -> {
                // Urban (without explicit sign): Display city icon
                SpeedResult(URBAN_ICON_CODE, false)
            }
            else -> {
                SpeedResult(null, false)
            }
        }
    }

    private fun parseMaxSpeedTag(normalized: String): Int? {
        if (normalized.contains("walk")) return 7 // Typical "Schrittgeschwindigkeit" in DE is ~7
        return parseNumericOrNull(normalized)
    }

    private fun parseNumericOrNull(text: String): Int? {
        val firstPart = text.split(';', ' ', ':').firstOrNull { part ->
            part.any { it.isDigit() }
        }
        return firstPart?.filter { it.isDigit() }?.toIntOrNull()
    }

    /**
     * Parses additional information such as hazards or speed cameras.
     */
    fun parseAdditionalInfo(tags: Map<String, String>?, showSpeedCamerasEnabled: Boolean): List<String> {
        if (tags == null) return emptyList()
        val info = mutableListOf<String>()

        if (tags.containsKey("hazard")) info.add(INFO_HAZARD)
        if (showSpeedCamerasEnabled && tags["highway"] == "speed_camera") info.add(INFO_CAMERA)
        if (tags["school"] == "yes" || tags["amenity"] == "school") info.add(INFO_SCHOOL)

        return info
    }
}

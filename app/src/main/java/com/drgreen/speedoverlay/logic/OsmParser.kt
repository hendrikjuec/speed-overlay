/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

/**
 * Analyzes OpenStreetMap data to determine speed limits.
 * STRICT POLICY: Adheres ONLY to OpenStreetMap 'maxspeed' tags.
 * If data is missing, ambiguous, or purely implicit (based on highway type), it returns null.
 */
class OsmParser {

    data class SpeedResult(
        val limit: Int?,
        val isConfidenceHigh: Boolean
    )

    /**
     * Parses the speed limit from OSM tags.
     * @param tags The metadata of the road segment.
     */
    fun parseSpeedLimit(tags: Map<String, String>?): SpeedResult {
        if (tags == null) return SpeedResult(null, false)

        // STRICTNESS: Only consider the "maxspeed" key.
        // We ignore "zone:maxspeed" and "source:maxspeed" to comply with the "ONLY maxspeed tags" rule.
        val maxspeed = tags["maxspeed"]
        if (maxspeed.isNullOrBlank()) {
            return SpeedResult(null, false)
        }

        val normalized = maxspeed.lowercase().trim()

        // 1. Handle special string values
        if (normalized == "none" || normalized == "unlimited") return SpeedResult(0, true)
        if (normalized == "signals") return SpeedResult(-1, true)
        if (normalized.contains("walk")) return SpeedResult(7, true)

        // 2. Handle country-specific urban/rural tags in maxspeed (e.g., "DE:urban")
        if (normalized.contains(":")) {
            val value = when (normalized.substringAfter(":")) {
                "urban" -> 50
                "rural" -> 100
                "motorway" -> 0 // Unlimited in DE
                else -> null
            }
            if (value != null) return SpeedResult(value, true)
        }

        // 3. Handle numeric values and check for ambiguity (e.g., "30; 50")
        // If there are multiple different numeric values, it's ambiguous -> Show Nothing.
        val parts = normalized.split(';', ' ').filter { it.any { c -> c.isDigit() } }
        val numericValues = parts.mapNotNull { part ->
            part.filter { it.isDigit() }.toIntOrNull()
        }.distinct()

        return if (numericValues.size == 1) {
            SpeedResult(numericValues[0], true)
        } else {
            // Ambiguous data or no numeric value found
            SpeedResult(null, false)
        }
    }
}

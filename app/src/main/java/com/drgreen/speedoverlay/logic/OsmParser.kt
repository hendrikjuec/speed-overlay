/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

class OsmParser {
    /**
     * Parsed das Tempolimit.
     * Rückgabewerte:
     * > 0: Konkretes Limit in km/h
     * 0: Unbegrenzt (Schild 282)
     * -1: Variables Limit (Schilderbrücke)
     * null: Unbekannt
     */
    fun parseSpeedLimit(tags: Map<String, String>?): Int? {
        if (tags == null) return null

        val maxspeed = tags["maxspeed"]
        if (!maxspeed.isNullOrBlank()) {
            val normalized = maxspeed.lowercase().trim()
            if (normalized == "none" || normalized == "unlimited") return 0
            if (normalized == "signals" || normalized == "variable") return -1

            val parsed = parseMaxSpeedTag(normalized)
            if (parsed != null) return parsed
        }

        val highway = tags["highway"]
        return when (highway) {
            "living_street" -> 7
            "pedestrian" -> 5
            "track" -> 30
            else -> null
        }
    }

    private fun parseMaxSpeedTag(normalized: String): Int? {
        if (normalized.contains("de:urban")) return 50
        if (normalized.contains("de:rural")) return 100
        if (normalized.contains("de:motorway")) return 0 // In DE oft unbegrenzt

        if (normalized.contains("walk")) return 5

        val firstPart = normalized.split(';', ' ', ':').firstOrNull { part ->
            part.any { it.isDigit() }
        }
        return firstPart?.filter { it.isDigit() }?.toIntOrNull()
    }

    fun parseAdditionalInfo(tags: Map<String, String>?, showSpeedCamerasEnabled: Boolean): List<String> {
        if (tags == null) return emptyList()
        val info = mutableListOf<String>()

        if (tags.containsKey("hazard")) info.add("Gefahr")
        if (showSpeedCamerasEnabled && tags["highway"] == "speed_camera") info.add("Blitzer")
        if (tags["school"] == "yes" || tags["amenity"] == "school") info.add("Schule")

        return info
    }
}

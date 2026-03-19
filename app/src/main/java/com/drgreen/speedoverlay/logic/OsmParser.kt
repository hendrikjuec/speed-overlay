/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

class OsmParser {
    fun parseSpeedLimit(tags: Map<String, String>?): Int? {
        if (tags == null) return null

        val maxSpeed = tags["maxspeed"]
        if (!maxSpeed.isNullOrBlank()) {
            val parsed = parseMaxSpeedTag(maxSpeed)
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

    private fun parseMaxSpeedTag(maxSpeed: String): Int? {
        val normalized = maxSpeed.lowercase().trim()

        if (normalized.contains("de:urban")) return 50
        if (normalized.contains("de:rural")) return 100
        if (normalized.contains("de:motorway")) return null // Advisory 130 km/h is not a hard limit

        if (normalized == "none" || normalized == "unlimited") return null
        if (normalized.contains("walk")) return 5

        val firstPart = normalized.split(';', ' ', ':').firstOrNull { part ->
            part.any { it.isDigit() }
        }
        return firstPart?.filter { it.isDigit() }?.toIntOrNull()
    }
}

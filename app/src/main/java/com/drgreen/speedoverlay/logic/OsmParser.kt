/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.logic

/**
 * Analysiert OpenStreetMap-Daten zur Bestimmung von Tempolimits.
 * Diese Klasse implementiert die Standard-Verkehrsregeln für alle Mitgliedstaaten der Europäischen Union.
 *
 * Priorität der Datenquellen:
 * 1. Explizites 'maxspeed' Tag (Verkehrsschilder) -> Hohe Sicherheit
 * 2. 'zone:maxspeed' oder 'source:maxspeed' (Zonenbeschilderung) -> Hohe Sicherheit
 * 3. Länderspezifische implizite Limits basierend auf dem Straßentyp (Highway-Tag) -> Niedrige Sicherheit
 */
class OsmParser {

    data class SpeedResult(
        val limit: Int?,
        val isConfidenceHigh: Boolean
    )

    private data class CountryDefaults(
        val urban: Int = 50,
        val rural: Int = 90,
        val trunk: Int = 100,
        val motorway: Int = 130,
        val livingStreet: Int = 20
    )

    private val countryMap = mapOf(
        "AT" to CountryDefaults(rural = 100, trunk = 100, motorway = 130, livingStreet = 5),  // Österreich
        "BE" to CountryDefaults(rural = 70, trunk = 120, motorway = 120, livingStreet = 20),   // Belgien
        "BG" to CountryDefaults(rural = 90, trunk = 120, motorway = 140, livingStreet = 20),   // Bulgarien
        "CY" to CountryDefaults(rural = 80, trunk = 80, motorway = 100, livingStreet = 20),    // Zypern
        "CZ" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Tschechien
        "DE" to CountryDefaults(rural = 100, trunk = 100, motorway = 0, livingStreet = 7),     // Deutschland
        "DK" to CountryDefaults(rural = 80, trunk = 80, motorway = 130, livingStreet = 15),    // Dänemark
        "EE" to CountryDefaults(rural = 90, trunk = 90, motorway = 110, livingStreet = 20),    // Estland
        "ES" to CountryDefaults(rural = 90, trunk = 100, motorway = 120, livingStreet = 20),   // Spanien
        "FI" to CountryDefaults(rural = 80, trunk = 100, motorway = 120, livingStreet = 20),   // Finnland
        "FR" to CountryDefaults(rural = 80, trunk = 110, motorway = 130, livingStreet = 20),   // Frankreich
        "GR" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Griechenland
        "EL" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Griechenland (ISO Alternative)
        "HR" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Kroatien
        "HU" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Ungarn
        "IE" to CountryDefaults(rural = 80, trunk = 100, motorway = 120, livingStreet = 30),   // Irland
        "IT" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 30),   // Italien
        "LT" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Litauen
        "LU" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Luxemburg
        "LV" to CountryDefaults(rural = 90, trunk = 90, motorway = 110, livingStreet = 20),    // Lettland
        "MT" to CountryDefaults(urban = 40, rural = 60, trunk = 60, motorway = 80, livingStreet = 20), // Malta
        "NL" to CountryDefaults(rural = 80, trunk = 100, motorway = 100, livingStreet = 15),   // Niederlande
        "PL" to CountryDefaults(rural = 90, trunk = 120, motorway = 140, livingStreet = 20),   // Polen
        "PT" to CountryDefaults(rural = 90, trunk = 100, motorway = 120, livingStreet = 20),   // Portugal
        "RO" to CountryDefaults(rural = 90, trunk = 100, motorway = 130, livingStreet = 20),   // Rumänien
        "SE" to CountryDefaults(rural = 70, trunk = 90, motorway = 110, livingStreet = 20),    // Schweden
        "SI" to CountryDefaults(rural = 90, trunk = 110, motorway = 130, livingStreet = 20),   // Slowenien
        "SK" to CountryDefaults(rural = 90, trunk = 100, motorway = 130, livingStreet = 20),   // Slowakei
        "CH" to CountryDefaults(rural = 80, trunk = 100, motorway = 120, livingStreet = 20),   // Schweiz
        "NO" to CountryDefaults(rural = 80, trunk = 80, motorway = 110, livingStreet = 20)     // Norwegen
    )

    fun parseSpeedLimit(tags: Map<String, String>?): SpeedResult {
        if (tags == null) return SpeedResult(null, false)

        // 1. ABSOLUTE PRIORITÄT: Explizite Schilder (maxspeed Tag) -> Hohe Sicherheit
        val maxspeed = tags["maxspeed"]
        if (!maxspeed.isNullOrBlank()) {
            val normalized = maxspeed.lowercase().trim()
            if (normalized == "none" || normalized == "unlimited") return SpeedResult(0, true)
            if (normalized == "signals" || normalized == "variable") return SpeedResult(-1, true)

            val parsed = parseMaxSpeedTag(normalized)
            if (parsed != null) return SpeedResult(parsed, true)
        }

        // 2. ZWEITE PRIORITÄT: Zonenbeschilderung -> Hohe Sicherheit
        val zoneTag = tags["zone:maxspeed"] ?: tags["source:maxspeed"]
        if (!zoneTag.isNullOrBlank()) {
            val zone = zoneTag.uppercase()
            val numericInZone = zone.split(':', ' ', ';').firstOrNull { it.any { c -> c.isDigit() } }
                ?.filter { it.isDigit() }?.toIntOrNull()

            if (numericInZone != null) return SpeedResult(numericInZone, true)

            // Handle named zones like "DE:motorway"
            val implicitInZone = getImplicitSpeedLimit(tags, zone)
            if (implicitInZone != null) return SpeedResult(implicitInZone, true)
        }

        // 3. LETZTE PRIORITÄT: Implizite Regeln (Fallback) -> Niedrige Sicherheit
        return SpeedResult(getImplicitSpeedLimit(tags, ""), false)
    }

    private fun getImplicitSpeedLimit(tags: Map<String, String>, zone: String): Int? {
        val highway = tags["highway"] ?: return null

        val countryCode = countryMap.keys.find { zone.startsWith(it) } ?: "DE"
        val defaults = countryMap[countryCode] ?: CountryDefaults()

        val isUrban = tags["lit"] == "yes" ||
                     tags["sidewalk"] != null ||
                     tags["is_in:city"] != null ||
                     zone.contains("URBAN") ||
                     highway == "residential" ||
                     highway == "living_street" ||
                     highway == "pedestrian"

        return when (highway) {
            "motorway" -> defaults.motorway
            "motorway_link" -> 80
            "trunk" -> if (isUrban) defaults.urban else defaults.trunk
            "primary", "secondary", "tertiary" -> if (isUrban) defaults.urban else defaults.rural
            "residential", "unclassified" -> if (isUrban) defaults.urban else defaults.rural
            "living_street" -> defaults.livingStreet
            "pedestrian" -> 5
            "track" -> 30
            "service" -> if (isUrban) 30 else defaults.rural
            else -> null
        }
    }

    private fun parseMaxSpeedTag(normalized: String): Int? {
        if (normalized.contains(":urban")) return 50
        if (normalized.contains(":rural")) return 100
        if (normalized.contains(":motorway")) return 0
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

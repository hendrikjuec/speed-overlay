/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.location.Location
import com.drgreen.speedoverlay.logic.OsmParser
import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository für Geschwindigkeitsbegrenzungen.
 * Ruft Daten von der Overpass API ab und verwaltet einen Offline-Cache für Ausfallsicherheit.
 */
@Singleton
class SpeedRepository @Inject constructor(
    private val overpassApi: OverpassApi,
    private val osmParser: OsmParser
) {
    // Shared Element Cache für Offline-Fallback
    private val elementCache = ConcurrentHashMap<Long, Pair<Element, Long>>()

    private var cachedLimit: Int? = null
    private var cachedInfo: List<String> = emptyList()
    private var cachedConfidence: Boolean = false
    private var cachedLocation: Location? = null
    private var cachedTimestamp: Long = 0L

    /**
     * Ruft das Tempolimit für die aktuelle Position ab.
     * Nutzt einen Kurzzeit-Cache für Performance und einen Langzeit-Element-Cache für Offline-Betrieb.
     *
     * @param lat Breitengrad
     * @param lon Längengrad
     * @param heading Aktuelle Fahrtrichtung in Grad
     * @param speedKmh Aktuelle Geschwindigkeit in km/h (beeinflusst Suchradius)
     * @param showCameras Ob Blitzer-Infos mit einbezogen werden sollen
     */
    suspend fun fetchSpeedLimit(
        lat: Double,
        lon: Double,
        heading: Float? = null,
        speedKmh: Float = 0f,
        showCameras: Boolean = false
    ): SpeedResult<Int?> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val currentLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        // --- Kurzzeit-Cache Validierung ---
        if (cachedLimit != null &&
            currentTime - cachedTimestamp < Config.CACHE_EXPIRATION_MS &&
            (cachedLocation?.distanceTo(currentLocation) ?: Float.MAX_VALUE) < Config.CACHE_DISTANCE_THRESHOLD_METERS) {
            return@withContext SpeedResult.Success(cachedLimit, cachedInfo, cachedConfidence)
        }

        return@withContext try {
            val speedMs = speedKmh / 3.6f
            val dynamicRadius = (Config.BASE_SEARCH_RADIUS_METERS + (speedMs * Config.LOOKAHEAD_TIME_SECONDS))
                .coerceAtMost(Config.MAX_SEARCH_RADIUS_METERS.toFloat())

            val query = "[out:json];way(around:$dynamicRadius, $lat, $lon)[highway];out tags geom;"
            val response = overpassApi.getSpeedLimit(query)

            // Offline-Cache mit allen abgerufenen Elementen aktualisieren
            response.elements.forEach { element ->
                elementCache[element.id] = element to (currentTime + Config.OFFLINE_CACHE_EXPIRATION_MS)
            }
            cleanupCache()

            processBestMatch(response.elements, heading, speedKmh, showCameras, currentLocation)
        } catch (e: Exception) {
            // --- 🛡 Offline Resilienz-Strategie: Fallback auf Offline-Cache ---
            val offlineElements = elementCache.values
                .filter { it.second > currentTime }
                .map { it.first }

            if (offlineElements.isNotEmpty()) {
                processBestMatch(offlineElements, heading, speedKmh, showCameras, currentLocation)
            } else {
                SpeedResult.Error(e.message ?: "Netzwerkfehler und kein Offline-Cache verfügbar")
            }
        }
    }

    private fun processBestMatch(
        elements: List<Element>,
        heading: Float?,
        speedKmh: Float,
        showCameras: Boolean,
        currentLocation: Location
    ): SpeedResult<Int?> {
        if (elements.isEmpty()) return SpeedResult.Success(null, emptyList(), false)

        // --- 🧠 Smart Road Filtering & Scoring ---
        // Wählt die wahrscheinlichste Straße basierend auf Richtung und Distanz aus.
        val bestElement = elements.maxByOrNull { element ->
            calculateScore(element, heading, speedKmh, currentLocation)
        } ?: elements.first()

        val parseResult = osmParser.parseSpeedLimit(bestElement.tags)
        val additionalInfo = osmParser.parseAdditionalInfo(bestElement.tags, showCameras)

        val result = SpeedResult.Success(
            data = parseResult.limit,
            additionalInfo = additionalInfo,
            isConfidenceHigh = parseResult.isConfidenceHigh
        )

        cachedLimit = result.data
        cachedInfo = result.additionalInfo
        cachedConfidence = result.isConfidenceHigh
        cachedLocation = currentLocation
        cachedTimestamp = System.currentTimeMillis()

        return result
    }

    private fun calculateScore(element: Element, carHeading: Float?, speedKmh: Float, carLoc: Location): Double {
        var score = 0.0

        // 1. Distanz-Score (0.0 bis 1.0)
        val minDistance = element.geometry?.minOfOrNull { GeoUtils.distanceToPoint(carLoc.latitude, carLoc.longitude, it) } ?: 100f
        val distanceRatio = (minDistance / Config.MAX_SEARCH_RADIUS_METERS.toFloat()).toDouble().coerceIn(0.0, 1.0)
        score += (1.0 - distanceRatio) * 2.0

        // 2. Richtungs-Abgleich (Heading Match)
        if (carHeading != null && element.geometry != null && element.geometry.size >= 2) {
            val wayHeading = GeoUtils.calculateBearing(element.geometry[0], element.geometry.last())
            val diff = GeoUtils.getHeadingDifference(carHeading, wayHeading).toDouble()

            val tolerance = if (speedKmh < Config.JUNCTION_SPEED_THRESHOLD_KMH)
                Config.JUNCTION_HEADING_TOLERANCE_DEG else Config.HEADING_TOLERANCE_DEG

            if (diff <= tolerance) {
                score += 3.0 // Starkes Signal, wenn die Richtung übereinstimmt
            }
        }

        // 3. Straßentyp-Gewichtung basierend auf Geschwindigkeit
        val type = element.tags?.get("highway") ?: ""
        score += calculateTypeScore(type, speedKmh)

        return score
    }

    private fun calculateTypeScore(type: String, speedKmh: Float): Double {
        return when {
            speedKmh > Config.HIGH_SPEED_THRESHOLD_KMH -> {
                when (type) {
                    "motorway", "motorway_link" -> 2.0
                    "trunk", "trunk_link" -> 1.5
                    "primary" -> 1.0
                    else -> 0.1
                }
            }
            speedKmh < Config.LOW_SPEED_THRESHOLD_KMH -> {
                when (type) {
                    "living_street", "residential" -> 1.5
                    "service" -> 1.0
                    else -> 0.5
                }
            }
            else -> 0.5
        }
    }

    private fun cleanupCache() {
        val currentTime = System.currentTimeMillis()
        val iterator = elementCache.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.second < currentTime) {
                iterator.remove()
            }
        }
    }
}

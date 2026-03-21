/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.location.Location
import com.drgreen.speedoverlay.logic.OsmParser
import com.drgreen.speedoverlay.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

class SpeedRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl(Config.OVERPASS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val overpassApi = retrofit.create(OverpassApi::class.java)
    private val osmParser = OsmParser()

    // --- Offline Cache Storage ---
    // Key: Element ID, Value: Pair(Element, ExpiryTimestamp)
    private val elementCache = ConcurrentHashMap<Long, Pair<Element, Long>>()

    private var cachedLimit: Int? = null
    private var cachedInfo: List<String> = emptyList()
    private var cachedLocation: Location? = null
    private var cachedTimestamp: Long = 0L

    suspend fun fetchSpeedLimit(lat: Double, lon: Double, heading: Float? = null, speedKmh: Float = 0f, showCameras: Boolean = false): SpeedResult<Int?> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val currentLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        // --- Standard Cache Validation (Short term) ---
        if (cachedLimit != null &&
            currentTime - cachedTimestamp < Config.CACHE_EXPIRATION_MS &&
            cachedLocation?.distanceTo(currentLocation) ?: Float.MAX_VALUE < Config.CACHE_DISTANCE_THRESHOLD_METERS) {
            return@withContext SpeedResult.Success(cachedLimit, cachedInfo)
        }

        return@withContext try {
            val speedMs = speedKmh / 3.6f
            val dynamicRadius = (Config.BASE_SEARCH_RADIUS_METERS + (speedMs * Config.LOOKAHEAD_TIME_SECONDS))
                .coerceAtMost(Config.MAX_SEARCH_RADIUS_METERS.toDouble())

            val query = "[out:json];way(around:$dynamicRadius, $lat, $lon)[highway];out tags geom;"
            val response = overpassApi.getSpeedLimit(query)

            // Update Offline Cache with all fetched elements
            response.elements.forEach { element ->
                elementCache[element.id] = element to (currentTime + Config.OFFLINE_CACHE_EXPIRATION_MS)
            }
            cleanupCache()

            processBestMatch(response.elements, heading, speedKmh, showCameras, currentLocation)
        } catch (e: Exception) {
            // --- 🛡 Offline Resilience Strategy: Fallback to Offline Cache ---
            val offlineElements = elementCache.values
                .filter { it.second > currentTime }
                .map { it.first }

            if (offlineElements.isNotEmpty()) {
                processBestMatch(offlineElements, heading, speedKmh, showCameras, currentLocation)
            } else if (cachedLimit != null) {
                 SpeedResult.Success(cachedLimit, cachedInfo)
            } else {
                 SpeedResult.Error("API offline and no cache available", e)
            }
        }
    }

    private fun processBestMatch(elements: List<Element>, heading: Float?, speedKmh: Float, showCameras: Boolean, currentLocation: Location): SpeedResult.Success<Int?> {
        val isJunctionMode = speedKmh < Config.JUNCTION_SPEED_THRESHOLD_KMH

        val bestElement = if (heading != null) {
            findBestElementSmartly(elements, heading, speedKmh, isJunctionMode, currentLocation)
        } else {
            elements.firstOrNull()
        }

        val limit = osmParser.parseSpeedLimit(bestElement?.tags)
        val info = osmParser.parseAdditionalInfo(bestElement?.tags, showCameras)

        cachedLimit = limit
        cachedInfo = info
        cachedLocation = currentLocation
        cachedTimestamp = System.currentTimeMillis()

        return SpeedResult.Success(limit, info)
    }

    private fun findBestElementSmartly(elements: List<Element>, userHeading: Float, speedKmh: Float, isJunctionMode: Boolean, currentLocation: Location): Element? {
        val tolerance = if (isJunctionMode) Config.JUNCTION_HEADING_TOLERANCE_DEG else Config.HEADING_TOLERANCE_DEG

        val candidates = elements.mapNotNull { element ->
            val wayHeading = calculateWayHeading(element.geometry) ?: return@mapNotNull null
            val diff = abs(userHeading - wayHeading)
            val normalizedDiff = if (diff > 180) 360 - diff else diff
            val isOneway = element.tags?.get("oneway") == "yes"

            // 1. Heading check (with tolerance)
            val angleMatch = normalizedDiff <= tolerance || (!isOneway && normalizedDiff >= (180 - tolerance))
            if (!angleMatch) return@mapNotNull null

            // 2. Smart Road Weighting
            // Motorways/Trunks are preferred at high speed, Residential at low speed.
            val highwayType = element.tags?.get("highway") ?: "unclassified"
            val typeScore = calculateTypeScore(highwayType, speedKmh)

            // 3. Distance Factor (Penalize far away roads)
            val minDistance = calculateMinDistance(element.geometry, currentLocation)
            val distanceScore = (1.0 / (minDistance + 10.0)) * 100 // Normalized distance impact

            // Combined Match Score (Lower is better, like a "Penalty" score)
            // Penalty = AngleDiff + TypePenalty + DistancePenalty
            val anglePenalty = if (normalizedDiff > 180) abs(180 - normalizedDiff) else normalizedDiff
            val typePenalty = (1.0 - typeScore) * 100
            val distancePenalty = minDistance * 0.5

            val totalPenalty = anglePenalty + typePenalty + distancePenalty
            element to totalPenalty
        }

        return candidates.minByOrNull { it.second }?.first
    }

    private fun calculateTypeScore(type: String, speedKmh: Float): Double {
        return when {
            speedKmh > Config.HIGH_SPEED_THRESHOLD_KMH -> {
                when (type) {
                    "motorway", "motorway_link" -> 1.0
                    "trunk", "trunk_link" -> 0.9
                    "primary" -> 0.7
                    "secondary" -> 0.3
                    else -> 0.1 // Penalize residential/service at 130 km/h
                }
            }
            speedKmh < Config.LOW_SPEED_THRESHOLD_KMH -> {
                when (type) {
                    "residential", "living_street" -> 1.0
                    "service", "unclassified" -> 0.8
                    "tertiary", "secondary" -> 0.6
                    else -> 0.4
                }
            }
            else -> 0.7 // Default weighting in mid-range speed
        }
    }

    private fun calculateMinDistance(geometry: List<GeometryPoint>?, currentLocation: Location): Double {
        if (geometry == null) return Double.MAX_VALUE
        return geometry.minOf { point ->
            val nodeLoc = Location("").apply { latitude = point.lat; longitude = point.lon }
            currentLocation.distanceTo(nodeLoc).toDouble()
        }
    }

    private fun calculateWayHeading(geometry: List<GeometryPoint>?): Double? {
        if (geometry == null || geometry.size < 2) return null
        val p1 = geometry[0]; val p2 = geometry[1]
        val lat1 = Math.toRadians(p1.lat); val lon1 = Math.toRadians(p1.lon)
        val lat2 = Math.toRadians(p2.lat); val lon2 = Math.toRadians(p2.lon)
        val y = sin(lon2 - lon1) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lon2 - lon1)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    private fun cleanupCache() {
        if (elementCache.size > Config.MAX_OFFLINE_CACHE_SIZE) {
            val oldestKeys = elementCache.entries
                .sortedBy { it.value.second }
                .take(elementCache.size - Config.MAX_OFFLINE_CACHE_SIZE)
                .map { it.key }
            oldestKeys.forEach { elementCache.remove(it) }
        }
    }
}

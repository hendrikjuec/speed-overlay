/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.location.Location
import com.drgreen.speedoverlay.logic.OsmParser
import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.GeoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-performance repository for fetching and caching speed limits from OSM.
 * Uses area-based caching (BBox) and directional prefetching (inspired by Blitzer.de/OsmAnd).
 * Optimized for < 1s latency with precise line-segment matching and sticky limits.
 */
@Singleton
class SpeedRepository @Inject constructor(
    private val overpassApi: OverpassApi,
    private val osmParser: OsmParser
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory Box Cache
    private var cachedElements: List<Element> = emptyList()
    private var currentBox: GeoUtils.BBox? = null
    private var safeZone: GeoUtils.BBox? = null
    private var lastFetchTime = 0L

    // Sticky limit logic to prevent flickering
    private var lastValidLimit: Int? = null
    private var lastValidConfidence: Boolean = false
    private var lastValidTime = 0L
    private val STICKY_DURATION_MS = 2500L // Keep limit for 2.5s if lost

    private val fetchMutex = Mutex()

    /**
     * Fetches the speed limit for a given location.
     * Uses local matching if the location is within the cached box.
     */
    suspend fun fetchSpeedLimit(
        lat: Double,
        lon: Double,
        heading: Float? = null,
        speedKmh: Float = 0f,
        @Suppress("UNUSED_PARAMETER") showCameras: Boolean = false
    ): SpeedResult<Int?> = withContext(Dispatchers.IO) {

        val currentLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        // 1. Area management (Fetch box if needed)
        val needsFetch = currentBox == null || !currentBox!!.contains(lat, lon) ||
                         System.currentTimeMillis() - lastFetchTime > Config.CACHE_EXPIRATION_MS

        val isNearEdge = safeZone != null && !safeZone!!.contains(lat, lon)

        if (needsFetch) {
            fetchArea(lat, lon, heading, speedKmh)
        } else if (isNearEdge && !fetchMutex.isLocked) {
            serviceScopeLaunch { fetchArea(lat, lon, heading, speedKmh) }
        }

        // 2. Precise matching with segment distance
        val result = processLocalMatch(cachedElements, heading, speedKmh, currentLocation)

        // 3. Apply Sticky Limit logic
        if (result is SpeedResult.Success) {
            if (result.data != null) {
                lastValidLimit = result.data
                lastValidConfidence = result.isConfidenceHigh
                lastValidTime = System.currentTimeMillis()
                return@withContext result
            } else if (System.currentTimeMillis() - lastValidTime < STICKY_DURATION_MS) {
                // Return sticky limit to prevent flickering
                return@withContext SpeedResult.Success(lastValidLimit, emptyList(), lastValidConfidence)
            }
        }

        result
    }

    private suspend fun fetchArea(lat: Double, lon: Double, heading: Float?, speedKmh: Float) = fetchMutex.withLock {
        if (currentBox?.contains(lat, lon) == true &&
            System.currentTimeMillis() - lastFetchTime < 60000L) return@withLock

        try {
            val box = GeoUtils.getDirectionalBoundingBox(lat, lon, heading, speedKmh, Config.CACHE_BOX_SIZE_KM)
            val zone = GeoUtils.getDirectionalBoundingBox(lat, lon, heading, speedKmh, Config.CACHE_SAFE_ZONE_KM)

            val query = "[out:json];way($box)[maxspeed];out tags geom;"
            val response = overpassApi.getSpeedLimit(query)

            cachedElements = response.elements
            currentBox = box
            safeZone = zone
            lastFetchTime = System.currentTimeMillis()
        } catch (e: Exception) {
            // Keep existing cache on error
        }
    }

    private fun processLocalMatch(
        elements: List<Element>,
        carHeading: Float?,
        speedKmh: Float,
        currentLocation: Location
    ): SpeedResult<Int?> {
        if (elements.isEmpty()) return SpeedResult.Success(null, emptyList(), false)

        // FILTER: Find closest road using precise line-segment distance
        val matchingElements = elements.map { element ->
            val minDistance = calculateMinDistance(currentLocation.latitude, currentLocation.longitude, element)
            element to minDistance
        }.filter { it.second < 45f } // Search radius of 45m from center of road

        if (matchingElements.isEmpty()) return SpeedResult.Success(null, emptyList(), false)

        // SCORE: Weight by distance and heading
        val bestMatch = matchingElements.maxByOrNull { (element, minDistance) ->
            calculateScore(element, minDistance, carHeading, speedKmh, currentLocation)
        } ?: return SpeedResult.Success(null, emptyList(), false)

        val parseResult = osmParser.parseSpeedLimit(bestMatch.first.tags)

        return SpeedResult.Success(
            data = parseResult.limit,
            additionalInfo = emptyList(),
            isConfidenceHigh = parseResult.isConfidenceHigh
        )
    }

    private fun calculateMinDistance(lat: Double, lon: Double, element: Element): Float {
        val geom = element.geometry ?: return 1000f
        if (geom.size < 2) return GeoUtils.distanceToPoint(lat, lon, geom[0])

        var minDistance = Float.MAX_VALUE
        for (i in 0 until geom.size - 1) {
            val dist = GeoUtils.distanceToSegment(lat, lon, geom[i], geom[i + 1])
            if (dist < minDistance) minDistance = dist
        }
        return minDistance
    }

    internal fun calculateScore(
        element: Element,
        minDistance: Float,
        carHeading: Float?,
        speedKmh: Float,
        carLoc: Location
    ): Double {
        var score = 0.0
        val geom = element.geometry ?: return -10.0

        // 1. Distance (Precise segment distance)
        val distanceRatio = (minDistance / 45.0).coerceIn(0.0, 1.0)
        score += (1.0 - distanceRatio) * 7.0

        // 2. Heading (Crucial for bridges/parallel roads)
        if (carHeading != null && geom.size >= 2) {
            val wayHeading = GeoUtils.calculateBearing(geom[0], geom.last())
            val diff = GeoUtils.getHeadingDifference(carHeading, wayHeading)
            val tolerance = if (speedKmh < Config.JUNCTION_SPEED_THRESHOLD_KMH) 75.0 else 35.0

            if (diff <= tolerance) {
                score += (1.0 - (diff / tolerance)) * 4.0
            } else {
                score -= 8.0 // High penalty for wrong direction
            }
        }
        return score
    }

    private fun serviceScopeLaunch(block: suspend () -> Unit) {
        repositoryScope.launch { block() }
    }
}

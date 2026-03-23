/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.location.Location
import com.drgreen.speedoverlay.logic.OsmParser
import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.GeoUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

/**
 * Repository for fetching speed limits from Overpass API (OSM) and managing local cache.
 */
@Singleton
class SpeedRepository @Inject constructor(
    private val overpassApi: OverpassApi,
    private val osmParser: OsmParser,
    private val speedDatabase: SpeedDatabase
) {
    private val gson = Gson()
    private var cachedLimit: Int? = null
    private var cachedInfo: List<String> = emptyList()
    private var cachedConfidence: Boolean = false
    private var cachedLocation: Location? = null
    private var cachedTimestamp: Long = 0L

    companion object {
        private const val MS_TO_KMH = 3.6f
        private const val TILE_PRECISION = 100.0
    }

    /**
     * Fetches the speed limit for a given location, heading, and speed.
     * Uses a two-level caching strategy (RAM and Room database).
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

        // --- 1. Short-term RAM Cache (for performance) ---
        if (cachedLimit != null &&
            currentTime - cachedTimestamp < Config.CACHE_EXPIRATION_MS &&
            (cachedLocation?.distanceTo(currentLocation) ?: Float.MAX_VALUE) < Config.CACHE_DISTANCE_THRESHOLD_METERS) {
            return@withContext SpeedResult.Success(cachedLimit, cachedInfo, cachedConfidence)
        }

        // --- 2. Calculate Tile ID (approx. 1km x 1km) ---
        val tileId = "${floor(lat * TILE_PRECISION).toInt()}_${floor(lon * TILE_PRECISION).toInt()}"

        // --- 3. Attempt to fetch data (Online -> Update Cache) ---
        try {
            val speedMs = speedKmh / MS_TO_KMH
            val dynamicRadius = (Config.BASE_SEARCH_RADIUS_METERS + (speedMs * Config.LOOKAHEAD_TIME_SECONDS))
                .coerceAtMost(Config.MAX_SEARCH_RADIUS_METERS.toFloat())

            // Overpass Query
            val query = "[out:json];way(around:$dynamicRadius, $lat, $lon)[highway];out tags geom;"
            val response = overpassApi.getSpeedLimit(query)

            // Update permanent cache (Room)
            val tileJson = gson.toJson(response)
            speedDatabase.tileDao().insertTile(OsmTile(tileId, tileJson, currentTime))

            processBestMatch(response.elements, heading, speedKmh, showCameras, currentLocation)
        } catch (e: Exception) {
            // --- Offline Fallback (Permanent Cache) ---
            val cachedTile = speedDatabase.tileDao().getTile(tileId)
            if (cachedTile != null) {
                val response = gson.fromJson(cachedTile.data, OverpassResponse::class.java)
                processBestMatch(response.elements, heading, speedKmh, showCameras, currentLocation)
            } else {
                SpeedResult.Error("Offline and no cache available for this area.")
            }
        }
    }

    /**
     * Processes multiple road elements to find the most likely match for the current position and heading.
     */
    private fun processBestMatch(
        elements: List<Element>,
        heading: Float?,
        speedKmh: Float,
        showCameras: Boolean,
        currentLocation: Location
    ): SpeedResult<Int?> {
        if (elements.isEmpty()) return SpeedResult.Success(null, emptyList(), false)

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

        // Update RAM cache
        updateRamCache(result, currentLocation)

        return result
    }

    /**
     * Updates the internal RAM cache with the latest result.
     */
    private fun updateRamCache(result: SpeedResult.Success<Int?>, currentLocation: Location) {
        cachedLimit = result.data
        cachedInfo = result.additionalInfo
        cachedConfidence = result.isConfidenceHigh
        cachedLocation = currentLocation
        cachedTimestamp = System.currentTimeMillis()
    }

    /**
     * Calculates a matching score for a road element based on distance, heading, and road type.
     */
    private fun calculateScore(element: Element, carHeading: Float?, speedKmh: Float, carLoc: Location): Double {
        var score = 0.0
        val geom = element.geometry

        // Distance score
        val minDistance = geom?.minOfOrNull { GeoUtils.distanceToPoint(carLoc.latitude, carLoc.longitude, it) } ?: 1000f
        val distanceRatio = (minDistance / Config.MAX_SEARCH_RADIUS_METERS.toFloat()).toDouble().coerceIn(0.0, 1.0)
        score += (1.0 - distanceRatio) * 2.0

        // Heading score
        if (carHeading != null && geom != null && geom.size >= 2) {
            val wayHeading = GeoUtils.calculateBearing(geom[0], geom.last())
            val diff = GeoUtils.getHeadingDifference(carHeading, wayHeading).toDouble()
            val tolerance = if (speedKmh < Config.JUNCTION_SPEED_THRESHOLD_KMH)
                Config.JUNCTION_HEADING_TOLERANCE_DEG else Config.HEADING_TOLERANCE_DEG
            if (diff <= tolerance) score += 3.0
        }

        // Road type score
        val type = element.tags?.get("highway") ?: ""
        score += when {
            speedKmh > Config.HIGH_SPEED_THRESHOLD_KMH -> if (type.contains("motorway") || type.contains("trunk")) 2.0 else 0.1
            speedKmh < Config.LOW_SPEED_THRESHOLD_KMH -> if (type == "residential" || type == "living_street") 1.5 else 0.5
            else -> 0.5
        }
        return score
    }
}

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
import kotlin.math.*

class SpeedRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl(Config.OVERPASS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val overpassApi = retrofit.create(OverpassApi::class.java)
    private val osmParser = OsmParser()

    private var cachedLimit: Int? = null
    private var cachedInfo: List<String> = emptyList()
    private var cachedLocation: Location? = null
    private var cachedTimestamp: Long = 0L

    suspend fun fetchSpeedLimit(lat: Double, lon: Double, heading: Float? = null, showCameras: Boolean = false): SpeedResult<Int?> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val currentLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        if (cachedLimit != null &&
            currentTime - cachedTimestamp < Config.CACHE_EXPIRATION_MS &&
            cachedLocation?.distanceTo(currentLocation) ?: Float.MAX_VALUE < Config.CACHE_DISTANCE_THRESHOLD_METERS) {
            return@withContext SpeedResult.Success(cachedLimit, cachedInfo)
        }

        return@withContext try {
            val query = "[out:json];way(around:${Config.SEARCH_RADIUS_METERS}, $lat, $lon)[highway];out tags geom;"
            val response = overpassApi.getSpeedLimit(query)

            val bestElement = if (heading != null) {
                findBestElementWithHeading(response.elements, heading)
            } else {
                response.elements.firstOrNull()
            }

            val limit = osmParser.parseSpeedLimit(bestElement?.tags)
            val info = osmParser.parseAdditionalInfo(bestElement?.tags, showCameras)

            cachedLimit = limit
            cachedInfo = info
            cachedLocation = currentLocation
            cachedTimestamp = currentTime

            SpeedResult.Success(limit, info)
        } catch (e: Exception) {
            SpeedResult.Error("Failed to fetch speed limit", e)
        }
    }

    private fun findBestElementWithHeading(elements: List<Element>, userHeading: Float): Element? {
        val candidates = elements.mapNotNull { element ->
            val wayHeading = calculateWayHeading(element.geometry) ?: return@mapNotNull null
            val diff = abs(userHeading - wayHeading)
            val normalizedDiff = if (diff > 180) 360 - diff else diff

            val isOneway = element.tags?.get("oneway") == "yes"

            if (normalizedDiff <= Config.HEADING_TOLERANCE_DEG) {
                element to normalizedDiff
            } else if (!isOneway && normalizedDiff >= (180 - Config.HEADING_TOLERANCE_DEG)) {
                element to (180 - normalizedDiff)
            } else {
                null
            }
        }
        return candidates.minByOrNull { it.second }?.first
    }

    private fun calculateWayHeading(geometry: List<GeometryPoint>?): Double? {
        if (geometry == null || geometry.size < 2) return null
        val p1 = geometry.first()
        val p2 = geometry.last()
        val lat1 = Math.toRadians(p1.lat)
        val lon1 = Math.toRadians(p1.lon)
        val lat2 = Math.toRadians(p2.lat)
        val lon2 = Math.toRadians(p2.lon)
        val y = sin(lon2 - lon1) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lon2 - lon1)
        val angle = Math.toDegrees(atan2(y, x))
        return (angle + 360) % 360
    }
}

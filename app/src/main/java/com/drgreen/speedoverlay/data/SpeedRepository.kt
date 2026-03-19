/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.location.Location
import com.drgreen.speedoverlay.logic.OsmParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.*

class SpeedRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val overpassApi = retrofit.create(OverpassApi::class.java)
    private val osmParser = OsmParser()

    private var cachedLimit: Int? = null
    private var cachedLocation: Location? = null
    private var cachedTimestamp: Long = 0L

    private companion object {
        const val CACHE_EXPIRATION_MS = 5 * 60 * 1000
        const val CACHE_DISTANCE_THRESHOLD = 50f
        const val HEADING_TOLERANCE_DEG = 45.0
    }

    suspend fun fetchSpeedLimit(lat: Double, lon: Double, heading: Float? = null): SpeedResult<Int?> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val currentLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }

        if (cachedLimit != null &&
            currentTime - cachedTimestamp < CACHE_EXPIRATION_MS &&
            cachedLocation?.distanceTo(currentLocation) ?: Float.MAX_VALUE < CACHE_DISTANCE_THRESHOLD) {
            return@withContext SpeedResult.Success(cachedLimit)
        }

        return@withContext try {
            // Geometrie anfordern für Richtungsabgleich
            val query = "[out:json];way(around:50, $lat, $lon)[highway];out tags geom;"
            val response = overpassApi.getSpeedLimit(query)

            val limit = if (heading != null) {
                findBestMatchWithHeading(response.elements, heading)
            } else {
                osmParser.parseSpeedLimit(response.elements.firstOrNull()?.tags)
            }

            cachedLimit = limit
            cachedLocation = currentLocation
            cachedTimestamp = currentTime

            SpeedResult.Success(limit)
        } catch (e: Exception) {
            SpeedResult.Error("Failed to fetch speed limit", e)
        }
    }

    private fun findBestMatchWithHeading(elements: List<Element>, userHeading: Float): Int? {
        val candidates = elements.mapNotNull { element ->
            val limit = osmParser.parseSpeedLimit(element.tags) ?: return@mapNotNull null
            val wayHeading = calculateWayHeading(element.geometry) ?: return@mapNotNull null

            val diff = abs(userHeading - wayHeading)
            val normalizedDiff = if (diff > 180) 360 - diff else diff

            // Check direction (including oneway)
            val isOneway = element.tags?.get("oneway") == "yes"

            if (normalizedDiff <= HEADING_TOLERANCE_DEG) {
                limit to normalizedDiff
            } else if (!isOneway && normalizedDiff >= (180 - HEADING_TOLERANCE_DEG)) {
                // For non-oneway roads, the opposite direction is also valid
                limit to (180 - normalizedDiff)
            } else {
                null
            }
        }

        return candidates.minByOrNull { it.second }?.first
    }

    private fun calculateWayHeading(geometry: List<GeometryPoint>?): Double? {
        if (geometry == null || geometry.size < 2) return null

        // Einfache Annäherung: Winkel zwischen dem ersten und letzten Punkt der Geometrie im Suchradius
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

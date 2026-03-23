/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import android.location.Location
import com.drgreen.speedoverlay.data.GeometryPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geographic utility functions for distance and heading calculations.
 */
object GeoUtils {
    /**
     * Calculates the bearing (0-360 degrees) between two points.
     */
    fun calculateBearing(p1: GeometryPoint, p2: GeometryPoint): Float {
        val lat1 = Math.toRadians(p1.lat)
        val lon1 = Math.toRadians(p1.lon)
        val lat2 = Math.toRadians(p2.lat)
        val lon2 = Math.toRadians(p2.lon)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    /**
     * Calculates the minimal angular difference between two headings.
     */
    fun getHeadingDifference(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2) % 360
        return if (diff > 180) 360 - diff else diff
    }

    /**
     * Calculates the distance between a coordinate and a geometry point.
     */
    fun distanceToPoint(lat: Double, lon: Double, p: GeometryPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat, lon, p.lat, p.lon, results)
        return results[0]
    }
}

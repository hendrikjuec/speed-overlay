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

    /**
     * Calculates the minimum distance from a point to a line segment (defined by two points).
     * Returns distance in meters.
     */
    fun distanceToSegment(lat: Double, lon: Double, p1: GeometryPoint, p2: GeometryPoint): Float {
        val r = distance(p1.lat, p1.lon, p2.lat, p2.lon)
        if (r < 0.001) return distance(lat, lon, p1.lat, p1.lon)

        // Orthogonal projection to find the closest point on the segment
        val t = (((lat - p1.lat) * (p2.lat - p1.lat) + (lon - p1.lon) * (p2.lon - p1.lon)) / (r * r))
            .coerceIn(0.0, 1.0)

        val closestLat = p1.lat + t * (p2.lat - p1.lat)
        val closestLon = p1.lon + t * (p2.lon - p1.lon)

        return distance(lat, lon, closestLat, closestLon)
    }

    /**
     * Calculates the distance between two latitude/longitude points in meters.
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Creates a bounding box around a center point with a given size in kilometers.
     */
    fun getBoundingBox(lat: Double, lon: Double, sizeKm: Double): BBox {
        val halfSize = sizeKm / 2.0
        val latDeg = halfSize / 111.0 // 1 degree lat is approx 111km
        val lonDeg = halfSize / (111.0 * cos(Math.toRadians(lat)))

        return BBox(
            south = lat - latDeg,
            west = lon - lonDeg,
            north = lat + latDeg,
            east = lon + lonDeg
        )
    }

    /**
     * Creates a bounding box that is shifted in the direction of travel if the speed is significant.
     */
    fun getDirectionalBoundingBox(lat: Double, lon: Double, heading: Float?, speedKmh: Float, sizeKm: Double): BBox {
        if (heading == null || speedKmh < 20f) {
            return getBoundingBox(lat, lon, sizeKm)
        }

        // Shift the center of the box forward by 30% of its size (predictive look-ahead)
        val shiftDistanceKm = sizeKm * 0.3
        val latRad = Math.toRadians(lat)
        val headingRad = Math.toRadians(heading.toDouble())

        val offsetLat = lat + (shiftDistanceKm / 111.0) * cos(headingRad)
        val offsetLon = lon + (shiftDistanceKm / (111.0 * cos(latRad))) * sin(headingRad)

        return getBoundingBox(offsetLat, offsetLon, sizeKm)
    }

    data class BBox(val south: Double, val west: Double, val north: Double, val east: Double) {
        fun contains(lat: Double, lon: Double): Boolean {
            return lat in south..north && lon in west..east
        }

        override fun toString(): String = "$south,$west,$north,$east"
    }
}

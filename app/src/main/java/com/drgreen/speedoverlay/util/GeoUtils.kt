/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import android.location.Location
import com.drgreen.speedoverlay.data.GeometryPoint
import kotlin.math.*

object GeoUtils {
    /**
     * Berechnet den Bearing (0-360 Grad) zwischen zwei Punkten.
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
     * Berechnet die minimale Winkeldifferenz zwischen zwei Headings.
     */
    fun getHeadingDifference(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2) % 360
        return if (diff > 180) 360 - diff else diff
    }

    /**
     * Berechnet die Distanz zwischen einem Punkt und einem Liniensegment (vereinfacht).
     */
    fun distanceToPoint(lat: Double, lon: Double, p: GeometryPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat, lon, p.lat, p.lon, results)
        return results[0]
    }
}

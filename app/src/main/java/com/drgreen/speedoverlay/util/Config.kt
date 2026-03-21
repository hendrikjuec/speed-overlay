/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

object Config {
    // Speed Processing
    const val SMOOTHING_WINDOW = 2
    const val JITTER_THRESHOLD_MS = 1.2f
    const val MIN_DISPLAY_SPEED = 0.5f
    const val MAX_PLAUSIBLE_ACCEL_MS2 = 15f

    // Sensor Fusion
    const val ACCELERATION_THRESHOLD = 0.5f // m/s^2 über Erdbeschleunigung
    const val GYRO_THRESHOLD = 0.1f // rad/s Rotation
    const val SENSOR_STILLSTAND_DELAY_MS = 2000L // Wie lange Sensoren "still" sein müssen

    // API & Repository
    const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"
    const val SEARCH_RADIUS_METERS = 150
    const val CACHE_EXPIRATION_MS = 2 * 60 * 1000L
    const val CACHE_DISTANCE_THRESHOLD_METERS = 20f
    const val HEADING_TOLERANCE_DEG = 35.0

    // Service & UI
    const val LOCATION_UPDATE_INTERVAL_MS = 400L
    const val LOCATION_MIN_UPDATE_INTERVAL_MS = 200L
    const val API_CALL_MIN_DISTANCE_METERS = 10f
}

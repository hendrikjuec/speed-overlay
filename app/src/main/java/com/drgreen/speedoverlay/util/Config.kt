/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

object Config {
    // Speed Processing
    const val SMOOTHING_WINDOW = 5
    const val JITTER_THRESHOLD_MS = 0.5f
    const val MIN_DISPLAY_SPEED = 0.1f
    const val MAX_PLAUSIBLE_ACCEL_MS2 = 10f // ~1g

    // API & Repository
    const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"
    const val SEARCH_RADIUS_METERS = 50
    const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
    const val CACHE_DISTANCE_THRESHOLD_METERS = 50f
    const val HEADING_TOLERANCE_DEG = 45.0

    // Service & UI
    const val LOCATION_UPDATE_INTERVAL_MS = 1000L
    const val LOCATION_MIN_UPDATE_INTERVAL_MS = 500L
    const val API_CALL_MIN_DISTANCE_METERS = 20f
}

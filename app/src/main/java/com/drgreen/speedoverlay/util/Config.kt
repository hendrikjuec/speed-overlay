/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

object Config {
    // Speed Processing
    const val SMOOTHING_WINDOW = 2 // Reduced from 3 to 2 for lower latency (~800ms)
    const val JITTER_THRESHOLD_MS = 0.5f // More sensitive
    const val MIN_DISPLAY_SPEED = 0.5f
    const val MAX_PLAUSIBLE_ACCEL_MS2 = 15f

    // GPS Filter
    const val MIN_LOCATION_ACCURACY_METERS = 30.0f
    const val ACCURACY_SPEED_THRESHOLD_MULT = 1.5f

    // Kalman Filter
    const val KALMAN_PROCESS_NOISE = 0.15f // Faster reaction
    const val KALMAN_MEASUREMENT_NOISE = 0.5f // Less weight on measurements, more on model for speed

    // Sensor Fusion
    const val ACCELERATION_THRESHOLD = 0.5f
    const val GYRO_THRESHOLD = 0.1f
    const val SENSOR_STILLSTAND_DELAY_MS = 1500L

    // API & Repository (Area Caching)
    const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"
    const val CACHE_BOX_SIZE_KM = 5.0      // 5x5 km area
    const val CACHE_SAFE_ZONE_KM = 3.5     // Re-fetch if user leaves the inner 3.5x3.5 km area
    const val CACHE_EXPIRATION_MS = 15 * 60 * 1000L // 15 minutes for box cache
    const val LOOKAHEAD_TIME_SECONDS = 25
    const val HEADING_TOLERANCE_DEG = 35.0
    const val JUNCTION_HEADING_TOLERANCE_DEG = 65.0

    // Offline Resilience & Smart Filtering
    const val OFFLINE_CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L
    const val HIGH_SPEED_THRESHOLD_KMH = 80.0f
    const val LOW_SPEED_THRESHOLD_KMH = 30.0f

    // Dynamic Update Triggers
    const val JUNCTION_SPEED_THRESHOLD_KMH = 25.0f

    // Service & UI
    const val LOCATION_UPDATE_INTERVAL_MS = 300L // 3.3 Hz for smoother UI
    const val LOCATION_MIN_UPDATE_INTERVAL_MS = 150L
    const val MAX_SEARCH_RADIUS_METERS = 1500
}

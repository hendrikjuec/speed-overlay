/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

object Config {
    // Speed Processing
    const val SMOOTHING_WINDOW = 3
    const val JITTER_THRESHOLD_MS = 0.8f
    const val MIN_DISPLAY_SPEED = 0.5f
    const val MAX_PLAUSIBLE_ACCEL_MS2 = 15f

    // GPS Filter
    const val MIN_LOCATION_ACCURACY_METERS = 25.0f
    const val ACCURACY_SPEED_THRESHOLD_MULT = 1.5f

    // Kalman Filter
    const val KALMAN_PROCESS_NOISE = 0.12f
    const val KALMAN_MEASUREMENT_NOISE = 0.8f

    // Sensor Fusion
    const val ACCELERATION_THRESHOLD = 0.5f
    const val GYRO_THRESHOLD = 0.1f
    const val SENSOR_STILLSTAND_DELAY_MS = 1800L

    // API & Repository (Predictive Pre-fetching)
    const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"
    const val BASE_SEARCH_RADIUS_METERS = 150 // Erhöht für Head Units
    const val MAX_SEARCH_RADIUS_METERS = 1500 // Massiv erhöht für Funkloch-Resilienz
    const val LOOKAHEAD_TIME_SECONDS = 20    // 20s Puffer bei aktueller Geschwindigkeit
    const val CACHE_EXPIRATION_MS = 60 * 1000L
    const val CACHE_DISTANCE_THRESHOLD_METERS = 50f
    const val HEADING_TOLERANCE_DEG = 35.0
    const val JUNCTION_HEADING_TOLERANCE_DEG = 65.0

    // Offline Resilience & Smart Filtering
    const val OFFLINE_CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 Stunden Cache
    const val HIGH_SPEED_THRESHOLD_KMH = 80.0f
    const val LOW_SPEED_THRESHOLD_KMH = 30.0f

    // Dynamic Update Triggers
    const val SPEED_CHANGE_TRIGGER_KMH = 15.0f
    const val JUNCTION_SPEED_THRESHOLD_KMH = 25.0f

    // Service & UI
    const val LOCATION_UPDATE_INTERVAL_MS = 400L
    const val LOCATION_MIN_UPDATE_INTERVAL_MS = 200L
    const val API_CALL_MIN_DISTANCE_METERS = 20f
}

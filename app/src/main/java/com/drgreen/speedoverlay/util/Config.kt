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

    // API & Repository (Predictive Pre-fetching)
    const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"
    const val BASE_SEARCH_RADIUS_METERS = 100
    const val MAX_SEARCH_RADIUS_METERS = 600
    const val LOOKAHEAD_TIME_SECONDS = 12
    const val CACHE_EXPIRATION_MS = 45 * 1000L
    const val CACHE_DISTANCE_THRESHOLD_METERS = 30f
    const val HEADING_TOLERANCE_DEG = 35.0
    const val JUNCTION_HEADING_TOLERANCE_DEG = 65.0

    // Offline Resilience & Smart Filtering
    const val MAX_OFFLINE_CACHE_SIZE = 100
    const val OFFLINE_CACHE_EXPIRATION_MS = 10 * 60 * 1000L
    const val HIGH_SPEED_THRESHOLD_KMH = 80.0f
    const val LOW_SPEED_THRESHOLD_KMH = 30.0f

    // Battery Optimization
    const val BATTERY_LOW_THRESHOLD = 20
    const val BATTERY_SAVER_GPS_INTERVAL_MS = 1000L
    const val BATTERY_SAVER_API_DIST_MULT = 2.0f

    // Dynamic Update Triggers
    const val SPEED_CHANGE_TRIGGER_KMH = 15.0f
    const val JUNCTION_SPEED_THRESHOLD_KMH = 25.0f

    // --- 📓 Logbook Configuration ---
    const val LOG_SPEED_THRESHOLD_KMH = 15.0f // Abweichung ab der geloggt wird
    const val LOG_COOLDOWN_SECONDS = 5 // Zeit in Sekunden, die das Speeding unterbrochen sein muss, um die Etappe zu beenden
    const val MAX_LOG_ENTRIES = 50 // Maximale Anzahl an gespeicherten Etappen

    // Service & UI
    const val LOCATION_UPDATE_INTERVAL_MS = 400L
    const val LOCATION_MIN_UPDATE_INTERVAL_MS = 200L
    const val API_CALL_MIN_DISTANCE_METERS = 15f
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

/**
 * Repräsentiert eine Etappe mit starker Geschwindigkeitsabweichung.
 */
data class LogEntry(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val speedLimit: Int,
    val maxSpeed: Int,
    val avgSpeed: Int,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double,
    val unit: String
)

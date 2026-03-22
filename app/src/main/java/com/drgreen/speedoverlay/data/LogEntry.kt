/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Repräsentiert eine Etappe mit starker Geschwindigkeitsabweichung.
 */
@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey val id: String,
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

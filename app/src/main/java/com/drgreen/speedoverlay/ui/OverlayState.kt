/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

/**
 * Immutable state representing the visual content of the overlay.
 */
data class OverlayState(
    val currentSpeed: Int,
    val speedLimit: Int?, // >0: km/h, 0: unlimited, -1: variable, null: unknown
    val unit: String,
    val isSpeeding: Boolean,
    val isConfidenceHigh: Boolean = false,
    val showHazard: Boolean = false,
    val showCamera: Boolean = false,
    val isAudioMuted: Boolean = false
)

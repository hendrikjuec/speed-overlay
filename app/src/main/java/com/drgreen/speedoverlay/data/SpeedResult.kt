/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

sealed class SpeedResult<out T> {
    data class Success<T>(val data: T, val additionalInfo: List<String> = emptyList()) : SpeedResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : SpeedResult<Nothing>()
    object Loading : SpeedResult<Nothing>()
}

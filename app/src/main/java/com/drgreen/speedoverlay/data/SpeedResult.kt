/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

sealed class SpeedResult<out T> {
    data class Success<out T>(val data: T) : SpeedResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : SpeedResult<Nothing>()
    object Loading : SpeedResult<Nothing>()
}

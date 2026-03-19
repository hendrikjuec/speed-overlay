/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import retrofit2.http.GET
import retrofit2.http.Query

data class OverpassResponse(
    val elements: List<Element>
)

data class Element(
    val type: String,
    val id: Long,
    val tags: Map<String, String>?,
    val geometry: List<GeometryPoint>?
)

data class GeometryPoint(
    val lat: Double,
    val lon: Double
)

interface OverpassApi {
    @GET("interpreter")
    suspend fun getSpeedLimit(
        @Query("data") query: String
    ): OverpassResponse
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.location.Location
import com.drgreen.speedoverlay.logic.OsmParser
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SpeedRepositoryTest {

    private lateinit var speedRepository: SpeedRepository
    private val mockApi = mockk<OverpassApi>()
    private val mockParser = mockk<OsmParser>()

    @Before
    fun setup() {
        speedRepository = SpeedRepository(mockApi, mockParser)
    }

    @Test
    fun `calculateScore should prioritize road with matching heading over parallel road with opposite heading`() {
        // --- 1. SETUP CAR ---
        // Car moving NORTH (heading 0)
        val carLat = 52.5200 // Berlin
        val carLon = 13.4050
        val carHeading = 0f
        val carLocation = Location("").apply {
            latitude = carLat
            longitude = carLon
        }

        // --- 2. SETUP ROADS ---
        // ROAD A: Correct heading (NORTH: 0), 5m to the right
        val roadA = Element(
            type = "way",
            id = 1,
            tags = mapOf("maxspeed" to "50", "highway" to "residential"),
            geometry = listOf(
                GeometryPoint(carLat - 0.0001, carLon + 0.00007), // Start South
                GeometryPoint(carLat + 0.0001, carLon + 0.00007)  // End North
            )
        )

        // ROAD B: OPPOSITE heading (SOUTH: 180), same distance
        val roadB = Element(
            type = "way",
            id = 2,
            tags = mapOf("maxspeed" to "100", "highway" to "trunk"), // Higher speed but wrong direction
            geometry = listOf(
                GeometryPoint(carLat + 0.0001, carLon - 0.00007), // Start North
                GeometryPoint(carLat - 0.0001, carLon - 0.00007)  // End South
            )
        )

        // --- 3. EXECUTE SCORE CALCULATION ---
        // Distance is ~7.7 meters for both (based on setup)
        val distA = 7.7f
        val distB = 7.7f
        val scoreA = speedRepository.calculateScore(roadA, distA, carHeading, 40f, carLocation)
        val scoreB = speedRepository.calculateScore(roadB, distB, carHeading, 40f, carLocation)

        // --- 4. VERIFY ---
        // Score A should be significantly higher because the heading matches.
        // Even if road B is a larger road (trunk), the direction penalty must win.
        println("Score Road A (Correct): $scoreA")
        println("Score Road B (Opposite): $scoreB")

        assertTrue("Road with matching heading must win", scoreA > scoreB)
    }

    @Test
    fun `calculateScore should apply heavy penalty for crossing roads`() {
        // Car moving NORTH (0)
        val carLocation = Location("").apply { latitude = 50.0; longitude = 10.0 }

        // Road crossing EAST-WEST (90 deg)
        val roadCrossing = Element(
            type = "way",
            id = 3,
            tags = mapOf("maxspeed" to "30"),
            geometry = listOf(
                GeometryPoint(50.0, 9.999),
                GeometryPoint(50.0, 10.001)
            )
        )

        val dist = 5.0f // 5m away
        val score = speedRepository.calculateScore(roadCrossing, dist, 0f, 30f, carLocation)

        // Score should be very low or negative because of heading mismatch (90 vs 0)
        println("Score Crossing Road: $score")
        assertTrue("Crossing roads must have low scores", score < 2.0)
    }
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SpeedWidgetProviderTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testUpdateWidgetBroadcast() {
        val provider = SpeedWidgetProvider()
        val intent = Intent(context, SpeedWidgetProvider::class.java).apply {
            action = SpeedWidgetProvider.ACTION_UPDATE_WIDGET
            putExtra(SpeedWidgetProvider.EXTRA_SPEED, 80)
            putExtra(SpeedWidgetProvider.EXTRA_LIMIT, 70)
            putExtra(SpeedWidgetProvider.EXTRA_UNIT, "km/h")
            putExtra(SpeedWidgetProvider.EXTRA_IS_SPEEDING, true)
            putExtra(SpeedWidgetProvider.EXTRA_CONFIDENCE, true)
        }

        // Simulate receiving the broadcast
        provider.onReceive(context, intent)

        // Verify it runs without crash.
        // Real verification of RemoteViews would require more complex ShadowAppWidgetManager usage.
    }

    @Test
    fun testUpdateWidgetStaticMethod() {
        // Test with confidence high
        SpeedWidgetProvider.updateWidget(
            context = context,
            speed = 100,
            limit = 120,
            unit = "km/h",
            isSpeeding = false,
            isConfidenceHigh = true
        )

        var shadowApp = shadowOf(context as Application)
        var broadcastIntents = shadowApp.broadcastIntents
        var lastIntent = broadcastIntents.last()

        assertEquals(SpeedWidgetProvider.ACTION_UPDATE_WIDGET, lastIntent.action)
        assertEquals(100, lastIntent.getIntExtra(SpeedWidgetProvider.EXTRA_SPEED, 0))
        assertEquals(120, lastIntent.getIntExtra(SpeedWidgetProvider.EXTRA_LIMIT, -1))
        assertEquals("km/h", lastIntent.getStringExtra(SpeedWidgetProvider.EXTRA_UNIT))
        assertEquals(false, lastIntent.getBooleanExtra(SpeedWidgetProvider.EXTRA_IS_SPEEDING, true))
        assertEquals(true, lastIntent.getBooleanExtra(SpeedWidgetProvider.EXTRA_CONFIDENCE, false))

        // Test with confidence low
        SpeedWidgetProvider.updateWidget(
            context = context,
            speed = 50,
            limit = 50,
            unit = "km/h",
            isSpeeding = false,
            isConfidenceHigh = false
        )
        shadowApp = shadowOf(context as Application)
        broadcastIntents = shadowApp.broadcastIntents
        lastIntent = broadcastIntents.last()

        assertEquals(false, lastIntent.getBooleanExtra(SpeedWidgetProvider.EXTRA_CONFIDENCE, true))
    }
}

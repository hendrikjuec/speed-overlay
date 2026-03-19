/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsManagerTest {

    private lateinit var settingsManager: SettingsManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
    }

    @Test
    fun testTolerancePersistence() {
        settingsManager.tolerance = 15
        assertEquals(15, settingsManager.tolerance)
    }

    @Test
    fun testAutostartPersistence() {
        settingsManager.isAutostartBtEnabled = true
        assertTrue(settingsManager.isAutostartBtEnabled)
    }

    @Test
    fun testAudioWarningPersistence() {
        settingsManager.isAudioWarningEnabled = false
        assertFalse(settingsManager.isAudioWarningEnabled)
    }

    @Test
    fun testUnitPersistence() {
        settingsManager.useMph = true
        assertTrue(settingsManager.useMph)
    }

    @Test
    fun testOverlaySizePersistence() {
        settingsManager.overlaySize = 1.5f
        assertEquals(1.5f, settingsManager.overlaySize, 0.01f)
    }

    @Test
    fun testOverlayAlphaPersistence() {
        settingsManager.overlayAlpha = 0.5f
        assertEquals(0.5f, settingsManager.overlayAlpha, 0.01f)
    }

    @Test
    fun testOverlayTextColorPersistence() {
        settingsManager.overlayTextColor = Color.RED
        assertEquals(Color.RED, settingsManager.overlayTextColor)
    }

    @Test
    fun testDisclaimerPersistence() {
        settingsManager.isDisclaimerAccepted = true
        assertTrue(settingsManager.isDisclaimerAccepted)
    }

    @Test
    fun testDefaultValues() {
        assertEquals(5, settingsManager.tolerance)
        assertFalse(settingsManager.isAutostartBtEnabled)
        assertTrue(settingsManager.isAudioWarningEnabled)
        assertFalse(settingsManager.useMph)
        assertEquals(1.0f, settingsManager.overlaySize, 0.01f)
        assertEquals(1.0f, settingsManager.overlayAlpha, 0.01f)
        assertEquals(Color.WHITE, settingsManager.overlayTextColor)
        assertFalse(settingsManager.isDisclaimerAccepted)
    }
}

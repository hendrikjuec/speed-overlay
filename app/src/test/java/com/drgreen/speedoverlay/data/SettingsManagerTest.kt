/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
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
    fun testTolerancePersistence() = runBlocking {
        settingsManager.tolerance = 15
        Thread.sleep(200)
        assertEquals(15, settingsManager.tolerance)
    }

    @Test
    fun testLanguagePersistence() = runBlocking {
        settingsManager.language = "de"
        Thread.sleep(300)
        assertEquals("de", settingsManager.language)
    }

    @Test
    fun testDarkModePersistence() = runBlocking {
        settingsManager.darkMode = 2 // ON
        Thread.sleep(300)
        assertEquals(2, settingsManager.darkMode)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun testAutostartPersistence() = runBlocking {
        settingsManager.isAutostartBtEnabled = true
        Thread.sleep(200)
        assertTrue(settingsManager.isAutostartBtEnabled)
    }

    @Test
    fun testAudioWarningPersistence() = runBlocking {
        settingsManager.isAudioWarningEnabled = false
        Thread.sleep(200)
        assertFalse(settingsManager.isAudioWarningEnabled)
    }

    @Test
    fun testUnitPersistence() = runBlocking {
        settingsManager.useMph = true
        Thread.sleep(200)
        assertTrue(settingsManager.useMph)
    }

    @Test
    fun testOverlaySizePersistence() = runBlocking {
        settingsManager.overlaySize = 1.5f
        Thread.sleep(200)
        assertEquals(1.5f, settingsManager.overlaySize, 0.01f)
    }

    @Test
    fun testOverlayAlphaPersistence() = runBlocking {
        settingsManager.overlayAlpha = 0.5f
        Thread.sleep(200)
        assertEquals(0.5f, settingsManager.overlayAlpha, 0.01f)
    }

    @Test
    fun testOverlayTextColorPersistence() = runBlocking {
        settingsManager.overlayTextColor = Color.RED
        Thread.sleep(200)
        assertEquals(Color.RED, settingsManager.overlayTextColor)
    }

    @Test
    fun testDisclaimerPersistence() = runBlocking {
        settingsManager.isDisclaimerAccepted = true
        Thread.sleep(200)
        assertTrue(settingsManager.isDisclaimerAccepted)
    }

    @Test
    fun testDefaultValues() {
        assertEquals(5, settingsManager.tolerance)
        assertFalse(settingsManager.isAutostartBtEnabled)
        assertTrue(settingsManager.isAudioWarningEnabled)
        assertFalse(settingsManager.useMph)
        // Adjusting default values to match current implementation
        assertEquals(1.0f, settingsManager.overlaySize, 0.01f)
        assertEquals(1.0f, settingsManager.overlayAlpha, 0.01f)
        assertEquals(Color.WHITE, settingsManager.overlayTextColor)
        assertFalse(settingsManager.isDisclaimerAccepted)
    }
}

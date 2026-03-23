/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        // Ensure a clean state for every test
        context.dataStore.edit { it.clear() }
        settingsManager = SettingsManager(context)
    }

    @Test
    fun testTolerancePersistence() = runTest {
        settingsManager.tolerance = 15
        val value = settingsManager.toleranceFlow.first { it == 15 }
        assertEquals(15, value)
    }

    @Test
    fun testLanguagePersistence() = runTest {
        settingsManager.language = "de"
        val value = settingsManager.languageFlow.first { it == "de" }
        assertEquals("de", value)
    }

    @Test
    fun testDarkModePersistence() = runTest {
        settingsManager.darkMode = 2 // ON
        val value = settingsManager.darkModeFlow.first { it == 2 }
        assertEquals(2, value)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun testAutostartPersistence() = runTest {
        settingsManager.isAutostartBootEnabled = true
        val value = settingsManager.autostartBootFlow.first { it }
        assertTrue(value)
    }

    @Test
    fun testAudioWarningPersistence() = runTest {
        settingsManager.isAudioWarningEnabled = false
        val value = settingsManager.audioWarningFlow.first { !it }
        assertFalse(value)
    }

    @Test
    fun testUnitPersistence() = runTest {
        settingsManager.useMph = true
        val value = settingsManager.useMphFlow.first { it }
        assertTrue(value)
    }

    @Test
    fun testOverlaySizePersistence() = runTest {
        settingsManager.overlaySize = 1.5f
        val value = settingsManager.overlaySizeFlow.first { it == 1.5f }
        assertEquals(1.5f, value, 0.01f)
    }

    @Test
    fun testOverlayAlphaPersistence() = runTest {
        settingsManager.overlayAlpha = 0.5f
        val value = settingsManager.overlayAlphaFlow.first { it == 0.5f }
        assertEquals(0.5f, value, 0.01f)
    }

    @Test
    fun testDefaultValues() {
        assertEquals(SettingsManager.DEFAULT_TOLERANCE, settingsManager.tolerance)
        assertFalse(settingsManager.isAutostartBootEnabled)
        assertTrue(settingsManager.isAudioWarningEnabled)
        assertFalse(settingsManager.useMph)
        assertEquals(1.0f, settingsManager.overlaySize, 0.01f)
        assertEquals(1.0f, settingsManager.overlayAlpha, 0.01f)
        assertEquals(Color.WHITE, settingsManager.overlayTextColor)
        assertFalse(settingsManager.isDisclaimerAccepted)
    }
}

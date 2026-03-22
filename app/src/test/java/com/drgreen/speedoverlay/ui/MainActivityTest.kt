/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drgreen.speedoverlay.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.S]) // S is needed for some modern Compose/AppCompat features in Robolectric
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testLanguageSelectionChangesLocale() {
        // Wir gehen davon aus, dass der Disclaimer bereits akzeptiert wurde oder wir uns im MainScreen befinden
        // In einem echten Test würde man hier Hilt nutzen, um den SettingsManager zu mocken.

        // Suche nach dem Sprach-Button (Standard "English" oder "EN")
        composeTestRule.onNodeWithText("English", ignoreCase = true).performClick()

        // Wähle "Deutsch" aus dem Dropdown
        composeTestRule.onNodeWithText("Deutsch", ignoreCase = true).performClick()

        // Überprüfe, ob der AppCompatDelegate die Sprache geändert hat
        composeTestRule.waitForIdle()
        assertEquals("de", AppCompatDelegate.getApplicationLocales().get(0)?.language)
    }

    @Test
    fun testDarkModeToggle() {
        // Suche nach dem Dark Mode Button
        composeTestRule.onNodeWithText("System Default", ignoreCase = true).performClick()

        // Wähle "On"
        composeTestRule.onNodeWithText("On", ignoreCase = true).performClick()

        // Überprüfe den Delegate Status
        composeTestRule.waitForIdle()
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }
}

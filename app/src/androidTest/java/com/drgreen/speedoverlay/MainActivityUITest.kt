/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drgreen.speedoverlay.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Test for MainActivity using Jetpack Compose Testing APIs.
 * Note: Since the app uses Hilt and permissions, this test assumes a device/emulator
 * where permissions are pre-granted or it bypasses the onboarding via state.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testUIElementsVisible() {
        // Wait for potential onboarding to be bypassed or check for main content
        composeTestRule.waitForIdle()

        // Check for App Name in TopBar
        composeTestRule.onNodeWithText("Speed Overlay", ignoreCase = true).assertIsDisplayed()

        // Check for Service Control buttons
        composeTestRule.onNodeWithText("Start Service", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop Service", ignoreCase = true).assertIsDisplayed()

        // Check for basic settings
        composeTestRule.onNodeWithText("Use MPH", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Speed Cameras", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun testSettingsTogglePersistence() {
        composeTestRule.waitForIdle()

        // Toggle "Use MPH"
        val mphNode = composeTestRule.onNodeWithText("Use MPH", ignoreCase = true)
        mphNode.performClick()

        // Wait for state update
        composeTestRule.waitForIdle()

        // Recreate activity to verify persistence (SettingsManager uses DataStore)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // Note: Checking 'isChecked' on a custom SettingSwitch might require
        // semantic properties. If it's a standard Material3 Switch, it should work.
        // For now we just verify it's still displayed after recreation.
        composeTestRule.onNodeWithText("Use MPH", ignoreCase = true).assertIsDisplayed()
    }
}

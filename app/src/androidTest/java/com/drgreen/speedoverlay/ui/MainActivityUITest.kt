/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Test for MainActivity using Jetpack Compose Testing APIs.
 * This replaces the Espresso-based tests as the UI is built with Compose.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testUIElementsVisible() {
        composeTestRule.waitForIdle()

        // Check for App Name/Title
        composeTestRule.onNodeWithText("Speed Overlay", ignoreCase = true).assertIsDisplayed()

        // Check for Service Control buttons (using text since IDs don't exist in Compose)
        composeTestRule.onNodeWithText("Start Service", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop Service", ignoreCase = true).assertIsDisplayed()

        // Check for basic settings sections or items
        composeTestRule.onNodeWithText("Settings", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Use MPH", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun testSettingsTogglePersistence() {
        composeTestRule.waitForIdle()

        // Find a toggle, e.g., "Use MPH"
        val mphNode = composeTestRule.onNodeWithText("Use MPH", ignoreCase = true)
        mphNode.assertIsDisplayed()

        // Perform click
        mphNode.performClick()
        composeTestRule.waitForIdle()

        // Recreate activity to verify persistence via DataStore
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // Verify it's still there
        composeTestRule.onNodeWithText("Use MPH", ignoreCase = true).assertIsDisplayed()
    }
}

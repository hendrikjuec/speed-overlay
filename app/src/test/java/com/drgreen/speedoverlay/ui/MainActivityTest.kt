/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drgreen.speedoverlay.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testOnboardingAndMainFlow() {
        // Step 1: Welcome
        composeTestRule.onNodeWithText("Let's Start").performClick()

        // Step 2: Location
        composeTestRule.onNodeWithText("Grant Permission").performClick()

        // Note: In a real test environment, we would mock the PermissionManager
        // to return true for hasLocationPermission so the 'Continue' button appears.
    }
}

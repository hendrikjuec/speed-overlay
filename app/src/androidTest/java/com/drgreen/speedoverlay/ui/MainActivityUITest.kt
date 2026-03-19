/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drgreen.speedoverlay.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testUIElementsVisible() {
        // App name from strings.xml is "Speed Overlay"
        onView(withText("Speed Overlay")).check(matches(isDisplayed()))
        onView(withId(R.id.btn_start)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_stop)).check(matches(isDisplayed()))
        onView(withId(R.id.slider_tolerance)).check(matches(isDisplayed()))
        onView(withId(R.id.switch_autostart)).check(matches(isDisplayed()))
    }

    @Test
    fun testBluetoothTogglePersistence() {
        // Toggle the switch
        onView(withId(R.id.switch_autostart)).perform(click())

        // Re-launch activity to check persistence
        activityRule.scenario.recreate()

        // The switch should maintain its state (this assumes it was initially unchecked)
        onView(withId(R.id.switch_autostart)).check(matches(isChecked()))
    }
}

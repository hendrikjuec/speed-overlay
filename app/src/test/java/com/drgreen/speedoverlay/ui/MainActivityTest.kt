/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.os.Build
import android.widget.AutoCompleteTextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class MainActivityTest {

    @Test
    fun testLanguageSelectionSync() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dropdownLanguage = activity.findViewById<AutoCompleteTextView>(R.id.dropdown_language)
                val settingsManager = SettingsManager(activity)

                // Force English
                settingsManager.language = "en"

                // Trigger selection of German (index 1) via the listener
                // In Robolectric, we can pass null for the AdapterView
                dropdownLanguage.onItemClickListener?.onItemClick(
                    null, null, 1, 1L
                )

                assertEquals("de", settingsManager.language)
            }
        }
    }

    @Test
    fun testDarkModeSelectionSync() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dropdownDarkMode = activity.findViewById<AutoCompleteTextView>(R.id.dropdown_dark_mode)
                val settingsManager = SettingsManager(activity)

                // Set to Auto (0)
                settingsManager.darkMode = 0

                // Select "Off" (index 1)
                dropdownDarkMode.onItemClickListener?.onItemClick(
                    null, null, 1, 1L
                )

                assertEquals(1, settingsManager.darkMode)
            }
        }
    }
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import java.util.Locale

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("speed_overlay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOLERANCE = "tolerance"
        private const val DEFAULT_TOLERANCE = 5
        private const val KEY_AUTOSTART_BT = "autostart_bt"
        private const val KEY_AUTOSTART_BT_DEVICE = "autostart_bt_device"
        private const val KEY_AUDIO_WARNING = "audio_warning"
        private const val KEY_UNIT_MPH = "unit_mph"
        private const val KEY_OVERLAY_SIZE = "overlay_size"
        private const val KEY_OVERLAY_ALPHA = "overlay_alpha"
        private const val KEY_OVERLAY_TEXT_COLOR = "overlay_text_color"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"

        private const val KEY_DARK_MODE = "dark_mode" // 0: Auto, 1: Off, 2: On
        private const val KEY_LANGUAGE = "language" // "en", "de", "es", "fr", "it"
    }

    var tolerance: Int
        get() = prefs.getInt(KEY_TOLERANCE, DEFAULT_TOLERANCE)
        set(value) = prefs.edit { putInt(KEY_TOLERANCE, value) }

    var isAutostartBtEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOSTART_BT, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTOSTART_BT, value) }

    var autostartBtDeviceAddress: String?
        get() = prefs.getString(KEY_AUTOSTART_BT_DEVICE, null)
        set(value) = prefs.edit { putString(KEY_AUTOSTART_BT_DEVICE, value) }

    var isAudioWarningEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_WARNING, true)
        set(value) = prefs.edit { putBoolean(KEY_AUDIO_WARNING, value) }

    var useMph: Boolean
        get() = prefs.getBoolean(KEY_UNIT_MPH, false)
        set(value) = prefs.edit { putBoolean(KEY_UNIT_MPH, value) }

    var overlaySize: Float
        get() = prefs.getFloat(KEY_OVERLAY_SIZE, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_OVERLAY_SIZE, value) }

    var overlayAlpha: Float
        get() = prefs.getFloat(KEY_OVERLAY_ALPHA, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_OVERLAY_ALPHA, value) }

    var overlayTextColor: Int
        get() = prefs.getInt(KEY_OVERLAY_TEXT_COLOR, Color.WHITE)
        set(value) = prefs.edit { putInt(KEY_OVERLAY_TEXT_COLOR, value) }

    var isDisclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_DISCLAIMER_ACCEPTED, value) }

    var darkMode: Int
        get() = prefs.getInt(KEY_DARK_MODE, 0)
        set(value) {
            if (darkMode != value) {
                prefs.edit { putInt(KEY_DARK_MODE, value) }
                applyDarkMode(value)
            }
        }

    var language: String
        get() {
            val systemLanguage = Locale.getDefault().language
            val defaultLang = when (systemLanguage) {
                "de" -> "de"
                "es" -> "es"
                "fr" -> "fr"
                "it" -> "it"
                else -> "en"
            }
            return prefs.getString(KEY_LANGUAGE, defaultLang) ?: defaultLang
        }
        set(value) {
            if (prefs.getString(KEY_LANGUAGE, null) != value) {
                prefs.edit { putString(KEY_LANGUAGE, value) }
                applyLanguage(value)
            }
        }

    fun applySettings() {
        applyDarkMode(darkMode)
        applyLanguage(language)
    }

    private fun applyDarkMode(mode: Int) {
        val appMode = when (mode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != appMode) {
            AppCompatDelegate.setDefaultNightMode(appMode)
        }
    }

    private fun applyLanguage(lang: String) {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val targetLocale = LocaleListCompat.forLanguageTags(lang)

        // Only apply if the primary locale tag differs
        if (currentLocales.isEmpty || currentLocales.get(0)?.language != lang) {
            AppCompatDelegate.setApplicationLocales(targetLocale)
        }
    }
}

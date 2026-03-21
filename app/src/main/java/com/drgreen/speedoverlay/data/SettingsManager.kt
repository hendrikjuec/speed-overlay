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

    interface OnSettingsChangedListener {
        fun onSettingChanged(key: String)
    }

    private val listeners = mutableListOf<OnSettingsChangedListener>()

    fun addListener(listener: OnSettingsChangedListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: OnSettingsChangedListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(key: String) {
        // Create a copy to avoid ConcurrentModificationException if a listener removes itself
        val currentListeners = synchronized(listeners) { listeners.toList() }
        currentListeners.forEach { it.onSettingChanged(key) }
    }

    companion object {
        const val KEY_TOLERANCE = "tolerance"
        const val DEFAULT_TOLERANCE = 5
        const val KEY_AUTOSTART_BT = "autostart_bt"
        const val KEY_AUTOSTART_BT_DEVICE = "autostart_bt_device"
        const val KEY_AUTOSTART_POWER = "autostart_power"
        const val KEY_AUDIO_WARNING = "audio_warning"
        const val KEY_AUDIO_MUTED_TEMPORARY = "audio_muted_temporary"
        const val KEY_UNIT_MPH = "unit_mph"
        const val KEY_OVERLAY_SIZE = "overlay_size"
        const val KEY_OVERLAY_ALPHA = "overlay_alpha"
        const val KEY_OVERLAY_TEXT_COLOR = "overlay_text_color"
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        const val KEY_SHOW_SPEED_CAMERAS = "show_speed_cameras"

        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LANGUAGE = "language"
    }

    var tolerance: Int
        get() = prefs.getInt(KEY_TOLERANCE, DEFAULT_TOLERANCE)
        set(value) { prefs.edit { putInt(KEY_TOLERANCE, value) }; notifyListeners(KEY_TOLERANCE) }

    var isAutostartBtEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOSTART_BT, false)
        set(value) { prefs.edit { putBoolean(KEY_AUTOSTART_BT, value) }; notifyListeners(KEY_AUTOSTART_BT) }

    var isAutostartPowerEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOSTART_POWER, false)
        set(value) { prefs.edit { putBoolean(KEY_AUTOSTART_POWER, value) }; notifyListeners(KEY_AUTOSTART_POWER) }

    var autostartBtDeviceAddress: String?
        get() = prefs.getString(KEY_AUTOSTART_BT_DEVICE, null)
        set(value) { prefs.edit { putString(KEY_AUTOSTART_BT_DEVICE, value) }; notifyListeners(KEY_AUTOSTART_BT_DEVICE) }

    var isAudioWarningEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_WARNING, true)
        set(value) { prefs.edit { putBoolean(KEY_AUDIO_WARNING, value) }; notifyListeners(KEY_AUDIO_WARNING) }

    var isAudioMutedTemporary: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_MUTED_TEMPORARY, false)
        set(value) {
            prefs.edit { putBoolean(KEY_AUDIO_MUTED_TEMPORARY, value) }
            notifyListeners(KEY_AUDIO_MUTED_TEMPORARY)
        }

    var useMph: Boolean
        get() = prefs.getBoolean(KEY_UNIT_MPH, false)
        set(value) { prefs.edit { putBoolean(KEY_UNIT_MPH, value) }; notifyListeners(KEY_UNIT_MPH) }

    var overlaySize: Float
        get() = prefs.getFloat(KEY_OVERLAY_SIZE, 1.0f)
        set(value) { prefs.edit { putFloat(KEY_OVERLAY_SIZE, value) }; notifyListeners(KEY_OVERLAY_SIZE) }

    var overlayAlpha: Float
        get() = prefs.getFloat(KEY_OVERLAY_ALPHA, 1.0f)
        set(value) { prefs.edit { putFloat(KEY_OVERLAY_ALPHA, value) }; notifyListeners(KEY_OVERLAY_ALPHA) }

    var overlayTextColor: Int
        get() = prefs.getInt(KEY_OVERLAY_TEXT_COLOR, Color.WHITE)
        set(value) { prefs.edit { putInt(KEY_OVERLAY_TEXT_COLOR, value) }; notifyListeners(KEY_OVERLAY_TEXT_COLOR) }

    var isDisclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_DISCLAIMER_ACCEPTED, value) }

    var showSpeedCameras: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SPEED_CAMERAS, false)
        set(value) { prefs.edit { putBoolean(KEY_SHOW_SPEED_CAMERAS, value) }; notifyListeners(KEY_SHOW_SPEED_CAMERAS) }

    var darkMode: Int
        get() = prefs.getInt(KEY_DARK_MODE, 0)
        set(value) {
            if (darkMode != value) {
                prefs.edit { putInt(KEY_DARK_MODE, value) }
                applyDarkMode(value)
                notifyListeners(KEY_DARK_MODE)
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
                notifyListeners(KEY_LANGUAGE)
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
        if (currentLocales.isEmpty || currentLocales.get(0)?.language != lang) {
            AppCompatDelegate.setApplicationLocales(targetLocale)
        }
    }
}

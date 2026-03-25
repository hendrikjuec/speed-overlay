/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private val dataStore = context.dataStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val prefsState = dataStore.data.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = runBlocking { dataStore.data.firstOrNull() ?: emptyPreferences() }
    )

    companion object {
        val TOLERANCE = intPreferencesKey("tolerance")
        val AUTOSTART_BOOT = booleanPreferencesKey("autostart_boot")
        val AUDIO_WARNING = booleanPreferencesKey("audio_warning")
        val AUDIO_MUTED_TEMPORARY = booleanPreferencesKey("audio_muted_temporary")
        val UNIT_MPH = booleanPreferencesKey("unit_mph")
        val OVERLAY_SIZE = floatPreferencesKey("overlay_size")
        val OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
        val OVERLAY_TEXT_COLOR = intPreferencesKey("overlay_text_color")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val SHOW_SPEED_CAMERAS = booleanPreferencesKey("show_speed_cameras")
        val BATTERY_OPTIMIZATION = booleanPreferencesKey("battery_optimization")
        val OVERLAY_X = intPreferencesKey("overlay_x")
        val OVERLAY_Y = intPreferencesKey("overlay_y")
        val DARK_MODE = intPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")

        const val DEFAULT_TOLERANCE = 5
    }

    // --- Flows ---
    val autostartBootFlow = prefsState.map { it[AUTOSTART_BOOT] ?: false }.distinctUntilChanged()
    val debugModeFlow = prefsState.map { it[DEBUG_MODE] ?: false }.distinctUntilChanged()
    val toleranceFlow = prefsState.map { it[TOLERANCE] ?: DEFAULT_TOLERANCE }.distinctUntilChanged()
    val useMphFlow = prefsState.map { it[UNIT_MPH] ?: false }.distinctUntilChanged()
    val audioWarningFlow = prefsState.map { it[AUDIO_WARNING] ?: true }.distinctUntilChanged()
    val showSpeedCamerasFlow = prefsState.map { it[SHOW_SPEED_CAMERAS] ?: false }.distinctUntilChanged()
    val overlaySizeFlow = prefsState.map { it[OVERLAY_SIZE] ?: 1.0f }.distinctUntilChanged()
    val overlayAlphaFlow = prefsState.map { it[OVERLAY_ALPHA] ?: 1.0f }.distinctUntilChanged()
    val overlayTextColorFlow = prefsState.map { it[OVERLAY_TEXT_COLOR] ?: Color.WHITE }.distinctUntilChanged()
    val darkModeFlow = prefsState.map { it[DARK_MODE] ?: 0 }.distinctUntilChanged()
    val languageFlow = prefsState.map { it[LANGUAGE] ?: getDefaultLanguage() }.distinctUntilChanged()

    // --- Properties ---
    var isAutostartBootEnabled: Boolean
        get() = prefsState.value[AUTOSTART_BOOT] ?: false
        set(value) = update(AUTOSTART_BOOT, value)

    var tolerance: Int
        get() = prefsState.value[TOLERANCE] ?: DEFAULT_TOLERANCE
        set(value) = update(TOLERANCE, value)

    var isAudioWarningEnabled: Boolean
        get() = prefsState.value[AUDIO_WARNING] ?: true
        set(value) = update(AUDIO_WARNING, value)

    var isAudioMutedTemporary: Boolean
        get() = prefsState.value[AUDIO_MUTED_TEMPORARY] ?: false
        set(value) = update(AUDIO_MUTED_TEMPORARY, value)

    var useMph: Boolean
        get() = prefsState.value[UNIT_MPH] ?: false
        set(value) = update(UNIT_MPH, value)

    var overlaySize: Float
        get() = prefsState.value[OVERLAY_SIZE] ?: 1.0f
        set(value) = update(OVERLAY_SIZE, value)

    var overlayAlpha: Float
        get() = prefsState.value[OVERLAY_ALPHA] ?: 1.0f
        set(value) = update(OVERLAY_ALPHA, value)

    var overlayTextColor: Int
        get() = prefsState.value[OVERLAY_TEXT_COLOR] ?: Color.WHITE
        set(value) = update(OVERLAY_TEXT_COLOR, value)

    var isDisclaimerAccepted: Boolean
        get() = prefsState.value[DISCLAIMER_ACCEPTED] ?: false
        set(value) = update(DISCLAIMER_ACCEPTED, value)

    var showSpeedCameras: Boolean
        get() = prefsState.value[SHOW_SPEED_CAMERAS] ?: false
        set(value) = update(SHOW_SPEED_CAMERAS, value)

    var isBatteryOptimizationEnabled: Boolean
        get() = prefsState.value[BATTERY_OPTIMIZATION] ?: true
        set(value) = update(BATTERY_OPTIMIZATION, value)

    var overlayX: Int
        get() = prefsState.value[OVERLAY_X] ?: 100
        set(value) = update(OVERLAY_X, value)

    var overlayY: Int
        get() = prefsState.value[OVERLAY_Y] ?: 200
        set(value) = update(OVERLAY_Y, value)

    var darkMode: Int
        get() = prefsState.value[DARK_MODE] ?: 0
        set(value) {
            update(DARK_MODE, value)
            applyDarkMode(value)
        }

    var language: String
        get() = prefsState.value[LANGUAGE] ?: getDefaultLanguage()
        set(value) {
            update(LANGUAGE, value)
            applyLanguage(value)
        }

    var isDebugModeEnabled: Boolean
        get() = prefsState.value[DEBUG_MODE] ?: false
        set(value) = update(DEBUG_MODE, value)

    private fun <T> update(key: Preferences.Key<T>, value: T) {
        scope.launch { dataStore.edit { it[key] = value } }
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
        AppCompatDelegate.setDefaultNightMode(appMode)
    }

    private fun applyLanguage(lang: String) {
        val targetLocale = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(targetLocale)
    }

    private fun getDefaultLanguage(): String = Locale.getDefault().language
}

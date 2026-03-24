/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.service.SpeedService
import com.drgreen.speedoverlay.ui.components.OnboardingScreen
import com.drgreen.speedoverlay.ui.components.SettingDarkMode
import com.drgreen.speedoverlay.ui.components.SettingLanguage
import com.drgreen.speedoverlay.ui.components.SettingSlider
import com.drgreen.speedoverlay.ui.components.SettingSwitch
import com.drgreen.speedoverlay.ui.components.SettingsCard
import com.drgreen.speedoverlay.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity of the application.
 * Handles the entry point, theme selection, and navigation between Onboarding and Main Settings.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsManager.applySettings()

        setContent {
            val darkModeState by settingsManager.darkModeFlow.collectAsStateWithLifecycle(initialValue = 0)

            var hasLocation by remember { mutableStateOf(permissionManager.hasLocationPermission()) }
            var hasOverlay by remember { mutableStateOf(permissionManager.hasOverlayPermission()) }
            var hasNotification by remember { mutableStateOf(permissionManager.hasNotificationPermission()) }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                hasLocation = permissionManager.hasLocationPermission()
                hasOverlay = permissionManager.hasOverlayPermission()
                hasNotification = permissionManager.hasNotificationPermission()
            }

            val isDarkTheme = when (darkModeState) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!hasLocation || !hasOverlay || !hasNotification) {
                        OnboardingScreen(
                            onFinished = { /* State driven */ },
                            onGrantLocation = { permissionManager.requestLocationPermission(this) },
                            onGrantOverlay = { permissionManager.requestOverlayPermission(this) },
                            onGrantNotification = { permissionManager.requestNotificationPermission(this) },
                            hasLocation = hasLocation,
                            hasOverlay = hasOverlay,
                            hasNotification = hasNotification
                        )
                    } else {
                        MainScreen(
                            settings = settingsManager,
                            onStart = { startSpeedService() },
                            onStop = { stopSpeedService() },
                            currentDarkMode = darkModeState
                        )
                    }
                }
            }
        }
    }

    private fun startSpeedService() {
        val intent = Intent(this, SpeedService::class.java)
        startForegroundService(intent)
    }

    private fun stopSpeedService() {
        stopService(Intent(this, SpeedService::class.java))
    }
}

/**
 * Main Settings Screen where the user can control the service and adjust settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settings: SettingsManager,
    onStart: () -> Unit,
    onStop: () -> Unit,
    currentDarkMode: Int
) {
    val tolerance by settings.toleranceFlow.collectAsStateWithLifecycle(initialValue = 5)
    val overlaySize by settings.overlaySizeFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val overlayAlpha by settings.overlayAlphaFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val useMph by settings.useMphFlow.collectAsStateWithLifecycle(initialValue = false)
    val audioWarning by settings.audioWarningFlow.collectAsStateWithLifecycle(initialValue = true)
    val showCameras by settings.showSpeedCamerasFlow.collectAsStateWithLifecycle(initialValue = false)
    val autostartBoot by settings.autostartBootFlow.collectAsStateWithLifecycle(initialValue = false)
    val language by settings.languageFlow.collectAsStateWithLifecycle(initialValue = "en")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Service Control
            SettingsCard(title = stringResource(R.string.service_control_group)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.desc_play_icon)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.start_service), fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.desc_stop_icon)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.stop_service), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // General Settings
            SettingsCard(title = stringResource(R.string.settings_title)) {
                SettingSwitch(stringResource(R.string.use_mph), useMph) { settings.useMph = it }
                SettingSwitch(stringResource(R.string.audio_warning), audioWarning) { settings.isAudioWarningEnabled = it }
                SettingSwitch(stringResource(R.string.show_speed_cameras), showCameras) { settings.showSpeedCameras = it }
                SettingLanguage(currentLanguage = language) { settings.language = it }
                SettingDarkMode(currentMode = currentDarkMode) { settings.darkMode = it }
            }

            // Automation
            SettingsCard(title = stringResource(R.string.automation_group)) {
                SettingSwitch(stringResource(R.string.autostart_boot), autostartBoot) {
                    settings.isAutostartBootEnabled = it
                }
            }

            // Appearance
            SettingsCard(title = stringResource(R.string.appearance_group)) {
                val unitLabel = if (useMph) " mph" else " km/h"
                SettingSlider(
                    label = stringResource(R.string.tolerance_title),
                    value = tolerance.toFloat(),
                    range = 0f..30f,
                    format = "%.0f$unitLabel"
                ) {
                    settings.tolerance = it.toInt()
                }
                SettingSlider(
                    label = stringResource(R.string.overlay_size),
                    value = overlaySize,
                    range = 0.5f..2.5f,
                    format = "%.1fx"
                ) {
                    settings.overlaySize = it
                }
                SettingSlider(
                    label = stringResource(R.string.overlay_alpha),
                    value = (1f - overlayAlpha).coerceIn(0f, 1f),
                    range = 0f..1f,
                    format = "%d%%",
                    factor = 100f
                ) {
                    settings.overlayAlpha = (1f - it).coerceIn(0f, 1f)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

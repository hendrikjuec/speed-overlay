/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.LogManager
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.service.SpeedService
import com.drgreen.speedoverlay.ui.components.*
import com.drgreen.speedoverlay.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Der Haupteinstiegspunkt der App. Verwaltet Einstellungen, Onboarding und den Service-Start.
 * Nutzt AppCompatActivity zur besseren Unterstützung von AppCompatDelegate (z.B. Sprachumschaltung).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var logManager: LogManager
    @Inject lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialisiert die gespeicherten Einstellungen (Sprache, Dark Mode)
        settingsManager.applySettings()

        setContent {
            val darkModeState by settingsManager.darkModeFlow.collectAsStateWithLifecycle(initialValue = 0)
            val isDisclaimerAccepted = remember { mutableStateOf(settingsManager.isDisclaimerAccepted) }

            // Permission states that refresh on ON_RESUME
            var hasLocation by remember { mutableStateOf(permissionManager.hasLocationPermission()) }
            var hasOverlay by remember { mutableStateOf(permissionManager.hasOverlayPermission()) }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                hasLocation = permissionManager.hasLocationPermission()
                hasOverlay = permissionManager.hasOverlayPermission()
            }

            val isDarkTheme = when (darkModeState) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isDisclaimerAccepted.value) {
                        OnboardingScreen(
                            onFinished = {
                                settingsManager.isDisclaimerAccepted = true
                                isDisclaimerAccepted.value = true
                            },
                            onGrantLocation = { permissionManager.requestLocationPermission(this) },
                            onGrantOverlay = { permissionManager.requestOverlayPermission(this) },
                            hasLocation = hasLocation,
                            hasOverlay = hasOverlay
                        )
                    } else {
                        MainScreen(
                            settings = settingsManager,
                            logManager = logManager,
                            permissionManager = permissionManager,
                            onStart = { startSpeedService() },
                            onStop = { stopSpeedService() },
                            currentDarkMode = darkModeState,
                            activity = this
                        )
                    }
                }
            }
        }
    }

    private fun startSpeedService() {
        if (!permissionManager.hasLocationPermission()) {
            permissionManager.requestLocationPermission(this)
            return
        }

        if (!permissionManager.hasOverlayPermission()) {
            permissionManager.requestOverlayPermission(this)
            return
        }

        val intent = Intent(this, SpeedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopSpeedService() {
        val intent = Intent(this, SpeedService::class.java)
        stopService(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settings: SettingsManager,
    logManager: LogManager,
    permissionManager: PermissionManager,
    onStart: () -> Unit,
    onStop: () -> Unit,
    currentDarkMode: Int,
    activity: MainActivity
) {
    val tolerance by settings.toleranceFlow.collectAsStateWithLifecycle(initialValue = 5)
    val overlaySize by settings.overlaySizeFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val overlayAlpha by settings.overlayAlphaFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val useMph by settings.useMphFlow.collectAsStateWithLifecycle(initialValue = false)
    val audioWarning by settings.audioWarningFlow.collectAsStateWithLifecycle(initialValue = true)
    val autostartBt by settings.autostartBtFlow.collectAsStateWithLifecycle(initialValue = false)
    val autostartPower by settings.autostartPowerFlow.collectAsStateWithLifecycle(initialValue = false)
    val autostartBtDevice by settings.autostartBtDeviceFlow.collectAsStateWithLifecycle(initialValue = null)
    val language by settings.languageFlow.collectAsStateWithLifecycle(initialValue = "en")

    val transparency = (1f - overlayAlpha).coerceIn(0f, 1f)
    var showLogbook by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { showLogbook = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Logbuch")
                    }
                },
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

            SettingsCard(title = "Service Control") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.start_service), fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.stop_service), fontWeight = FontWeight.Bold)
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.settings_title)) {
                SettingSwitch(stringResource(R.string.use_mph), useMph) {
                    settings.useMph = it
                }
                SettingSwitch(stringResource(R.string.audio_warning), audioWarning) {
                    settings.isAudioWarningEnabled = it
                    if (it) settings.isAudioMutedTemporary = false
                }
                SettingLanguage(currentLanguage = language) { newLang ->
                    settings.language = newLang
                }
                SettingDarkMode(currentMode = currentDarkMode) { newMode ->
                    settings.darkMode = newMode
                }
            }

            SettingsCard(title = stringResource(R.string.automation_group)) {
                SettingSwitch(stringResource(R.string.autostart_bt), autostartBt) {
                    settings.isAutostartBtEnabled = it
                    if (it && !permissionManager.hasBluetoothPermission()) {
                        permissionManager.requestBluetoothPermission(activity)
                    }
                }
                if (autostartBt) {
                    SettingBluetoothDevice(currentAddress = autostartBtDevice) {
                        settings.autostartBtDeviceAddress = it
                    }
                }
                SettingSwitch(stringResource(R.string.autostart_power), autostartPower) {
                    settings.isAutostartPowerEnabled = it
                }
            }

            SettingsCard(title = stringResource(R.string.appearance_group)) {
                SettingSlider(stringResource(R.string.tolerance_title), tolerance.toFloat(), 0f..30f, "%.0f km/h") {
                    settings.tolerance = it.toInt()
                }
                SettingSlider(stringResource(R.string.overlay_size), overlaySize, 0.5f..2.5f, "%.1fx") {
                    settings.overlaySize = it
                }
                SettingSlider(stringResource(R.string.overlay_alpha), transparency, 0f..1f, "%d%%", 100f) {
                    settings.overlayAlpha = (1f - it).coerceIn(0f, 1f)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogbook) {
        LogbookDialog(logManager = logManager, onDismiss = { showLogbook = false })
    }
}

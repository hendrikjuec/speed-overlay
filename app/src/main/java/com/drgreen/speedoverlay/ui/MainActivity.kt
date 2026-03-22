/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.LogManager
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.service.SpeedService
import com.drgreen.speedoverlay.ui.components.LogbookDialog
import com.drgreen.speedoverlay.ui.components.SettingLanguage
import com.drgreen.speedoverlay.ui.components.SettingSlider
import com.drgreen.speedoverlay.ui.components.SettingSwitch
import com.drgreen.speedoverlay.ui.components.SettingsCard
import com.drgreen.speedoverlay.util.PermissionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Der Haupteinstiegspunkt der App. Verwaltet Einstellungen, Onboarding und den Service-Start.
 *
 * Die UI ist mit Jetpack Compose modular aufgebaut. Komponenten befinden sich in [com.drgreen.speedoverlay.ui.components].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settings: SettingsManager
    @Inject lateinit var logManager: LogManager
    private val permissionManager by lazy { PermissionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialisiert die gespeicherten Einstellungen (Sprache, Dark Mode)
        settings.applySettings()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showOnboarding by remember { mutableStateOf(!settings.isDisclaimerAccepted) }

                    var hasLocation by remember { mutableStateOf(permissionManager.hasLocationPermission()) }
                    var hasOverlay by remember { mutableStateOf(permissionManager.hasOverlayPermission()) }

                    if (showOnboarding) {
                        OnboardingScreen(
                            onFinished = {
                                settings.isDisclaimerAccepted = true
                                showOnboarding = false
                                checkBatteryOptimization()
                            },
                            onGrantLocation = {
                                permissionManager.requestLocationPermission()
                            },
                            onGrantOverlay = {
                                permissionManager.requestOverlayPermission()
                            },
                            hasLocation = hasLocation,
                            hasOverlay = hasOverlay
                        )
                    } else {
                        MainScreen(settings, logManager, permissionManager, ::handleStartService, ::stopSpeedService)
                    }

                    // Periodische Prüfung der Berechtigungen für das Onboarding
                    LaunchedEffect(Unit) {
                        while(true) {
                            hasLocation = permissionManager.hasLocationPermission()
                            hasOverlay = permissionManager.hasOverlayPermission()
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                }
            }
        }
    }

    private fun handleStartService() {
        when {
            !permissionManager.hasLocationPermission() -> permissionManager.requestLocationPermission()
            !permissionManager.hasOverlayPermission() -> permissionManager.requestOverlayPermission()
            !permissionManager.hasNotificationPermission() -> permissionManager.requestNotificationPermission()
            else -> startSpeedService()
        }
    }

    private fun startSpeedService() {
        val intent = Intent(this, SpeedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopSpeedService() = stopService(Intent(this, SpeedService::class.java))

    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.battery_opt_title)
                .setMessage(R.string.battery_opt_message)
                .setPositiveButton(R.string.battery_opt_ok) { _, _ ->
                    val intent = try {
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
                    } catch (e: Exception) {
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    }
                    startActivity(intent)
                }
                .setNegativeButton(R.string.battery_opt_cancel, null)
                .show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settings: SettingsManager,
    logManager: LogManager,
    permissionManager: PermissionManager,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    var tolerance by remember { mutableFloatStateOf(settings.tolerance.toFloat()) }
    var overlaySize by remember { mutableFloatStateOf(settings.overlaySize) }
    var overlayAlpha by remember { mutableFloatStateOf(settings.overlayAlpha) }
    var autostartBt by remember { mutableStateOf(settings.isAutostartBtEnabled) }
    var autostartPower by remember { mutableStateOf(settings.isAutostartPowerEnabled) }
    var useMph by remember { mutableStateOf(settings.useMph) }
    var audioWarning by remember { mutableStateOf(settings.isAudioWarningEnabled) }
    var language by remember { mutableStateOf(settings.language) }

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

            // Dashboard Controls
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

            // General Settings
            SettingsCard(title = stringResource(R.string.settings_title)) {
                SettingSwitch(stringResource(R.string.use_mph), useMph) {
                    useMph = it
                    settings.useMph = it
                }
                SettingSwitch(stringResource(R.string.audio_warning), audioWarning) {
                    audioWarning = it
                    settings.isAudioWarningEnabled = it
                    if (it) settings.isAudioMutedTemporary = false
                }

                // Spracheinstellung
                SettingLanguage(currentLanguage = language) { newLang ->
                    language = newLang
                    settings.language = newLang
                }
            }

            // Automation
            SettingsCard(title = stringResource(R.string.automation_group)) {
                SettingSwitch(stringResource(R.string.autostart_bt), autostartBt) {
                    autostartBt = it
                    settings.isAutostartBtEnabled = it
                    if (it && !permissionManager.hasBluetoothPermission()) {
                        permissionManager.requestBluetoothPermission()
                    }
                }
                SettingSwitch(stringResource(R.string.autostart_power), autostartPower) {
                    autostartPower = it
                    settings.isAutostartPowerEnabled = it
                }
            }

            // Appearance
            SettingsCard(title = stringResource(R.string.appearance_group)) {
                SettingSlider(stringResource(R.string.tolerance_title), tolerance, 0f..30f, "%.0f km/h") {
                    tolerance = it
                    settings.tolerance = it.toInt()
                }
                SettingSlider(stringResource(R.string.overlay_size), overlaySize, 0.5f..2.5f, "%.1fx") {
                    overlaySize = it
                    settings.overlaySize = it
                }
                SettingSlider(stringResource(R.string.overlay_alpha), overlayAlpha, 0.2f..1f, "%d%%", 100f) {
                    overlayAlpha = it
                    settings.overlayAlpha = it
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogbook) {
        LogbookDialog(logManager = logManager, onDismiss = { showLogbook = false })
    }
}

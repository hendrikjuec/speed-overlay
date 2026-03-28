/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.LogManager
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.data.SpeedRepository
import com.drgreen.speedoverlay.data.SpeedResult
import com.drgreen.speedoverlay.logic.SpeedProcessor
import com.drgreen.speedoverlay.ui.OverlayManager
import com.drgreen.speedoverlay.ui.OverlayState
import com.drgreen.speedoverlay.ui.SpeedWidgetProvider
import com.drgreen.speedoverlay.util.Config
import com.drgreen.speedoverlay.util.HardwareHelper
import com.drgreen.speedoverlay.util.MotionDetector
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that tracks GPS location, processes speed, and manages the overlay and widget.
 * Optimized for < 1s latency using Kotlin Flows and decoupled UI updates.
 */
@AndroidEntryPoint
class SpeedService : Service() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var logManager: LogManager
    @Inject lateinit var hardwareHelper: HardwareHelper
    @Inject lateinit var motionDetector: MotionDetector
    @Inject lateinit var speedRepository: SpeedRepository

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var overlayManager: OverlayManager? = null
    private var toneGenerator: ToneGenerator? = null

    private val speedProcessor = SpeedProcessor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Reactive State for Latency Optimization
    private val currentSpeedFlow = MutableStateFlow(0)
    private val speedLimitFlow = MutableStateFlow<Int?>(null)
    private val confidenceFlow = MutableStateFlow(false)
    private val isMutedFlow = MutableStateFlow(false)

    // API Throttling
    private var lastApiCallTime = 0L
    private var lastApiLocation: Location? = null
    private var isCurrentlySpeeding = false

    companion object {
        private const val CHANNEL_ID = "SpeedServiceChannel"
        const val STOP_ACTION = "STOP_SERVICE"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_ACTION) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        overlayManager = OverlayManager(this, settingsManager, logManager) { toggleAudioMute() }

        // Immediate initial state
        isMutedFlow.value = !settingsManager.isAudioWarningEnabled || settingsManager.isAudioMutedTemporary

        try {
            createNotificationChannel()
            startAsForeground()
            overlayManager?.show()
            motionDetector.start()

            initLocationUpdates()
            observeMotion()
            observeStateForUI()

            // Trigger initial UI update
            refreshUI()

        } catch (e: Exception) {
            Log.e("SpeedService", "Failed to initialize service", e)
            stopSelf()
        }
    }

    private fun refreshUI() {
        serviceScope.launch {
            val state = OverlayState(
                currentSpeed = currentSpeedFlow.value,
                speedLimit = speedLimitFlow.value,
                unit = if (settingsManager.useMph) "mph" else "km/h",
                isSpeeding = speedProcessor.isSpeeding(currentSpeedFlow.value, speedLimitFlow.value, settingsManager.tolerance),
                isConfidenceHigh = confidenceFlow.value,
                isAudioMuted = isMutedFlow.value
            )
            overlayManager?.updateState(state)
            updateWidget(state)
        }
    }

    private fun startAsForeground() {
        val notification = createNotification() ?: throw RuntimeException("Notification failed")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeMotion() {
        serviceScope.launch {
            motionDetector.isMovingFlow.collectLatest { isMoving ->
                if (!isMoving) currentSpeedFlow.value = 0
            }
        }
    }

    /**
     * Latency-optimized UI observation.
     * Combines speed and limit flows to update the UI immediately when ANY value changes.
     */
    private fun observeStateForUI() {
        serviceScope.launch {
            combine(
                currentSpeedFlow,
                speedLimitFlow,
                confidenceFlow,
                isMutedFlow
            ) { speed, limit, confidence, muted ->
                val isSpeeding = speedProcessor.isSpeeding(speed, limit, settingsManager.tolerance)
                checkSpeedingAlert(isSpeeding, limit)

                OverlayState(
                    currentSpeed = speed,
                    speedLimit = limit,
                    unit = if (settingsManager.useMph) "mph" else "km/h",
                    isSpeeding = isSpeeding,
                    isConfidenceHigh = confidence,
                    isAudioMuted = muted
                )
            }.collectLatest { state ->
                overlayManager?.updateState(state)
                updateWidget(state)
            }
        }
    }

    private fun updateWidget(state: OverlayState) {
        SpeedWidgetProvider.updateWidget(
            context = this,
            speed = state.currentSpeed,
            limit = state.speedLimit,
            unit = state.unit,
            isSpeeding = state.isSpeeding,
            isConfidenceHigh = state.isConfidenceHigh
        )
    }

    @SuppressLint("MissingPermission")
    private fun initLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, Config.LOCATION_UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(Config.LOCATION_MIN_UPDATE_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                res.lastLocation?.let { processLocation(it) }
            }
        }
        fusedLocationClient?.requestLocationUpdates(request, locationCallback!!, null)
    }

    private fun processLocation(location: Location) {
        // 1. Immediate Speed Update (Non-blocking)
        val smoothedSpeed = speedProcessor.getSmoothedSpeed(location, settingsManager.useMph, motionDetector.isMoving)
        currentSpeedFlow.value = smoothedSpeed

        // 2. High-frequency Limit Update (Fast local cache lookup)
        fetchSpeedLimit(location)
    }

    private fun checkSpeedingAlert(isSpeeding: Boolean, limit: Int?) {
        if (isSpeeding && (limit ?: 0) > 0) {
            if (!isCurrentlySpeeding && !isMutedFlow.value) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
        }
        isCurrentlySpeeding = isSpeeding
    }

    private fun fetchSpeedLimit(location: Location) {
        serviceScope.launch {
            // repositoryScope handles IO dispatching internally now
            val result = speedRepository.fetchSpeedLimit(
                location.latitude,
                location.longitude,
                location.bearing,
                location.speed * 3.6f,
                false
            )

            if (result is SpeedResult.Success) {
                speedLimitFlow.value = result.data
                confidenceFlow.value = result.isConfidenceHigh
            }
        }
    }

    private fun toggleAudioMute() {
        settingsManager.isAudioMutedTemporary = !settingsManager.isAudioMutedTemporary
        isMutedFlow.value = !settingsManager.isAudioWarningEnabled || settingsManager.isAudioMutedTemporary
        hardwareHelper.vibrate()
        refreshUI() // Immediate update on status change
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification? {
        val stopIntent = Intent(this, SpeedService::class.java).apply { action = STOP_ACTION }
        val pendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_service), pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        toneGenerator?.release()
        overlayManager?.hide()
        motionDetector.stop()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }
}

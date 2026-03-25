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
import com.drgreen.speedoverlay.logic.OsmParser
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that tracks GPS location, processes speed, and manages the overlay and widget.
 * Optimized with robust error handling for Android 9+ Head Units.
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

    // State
    private var currentLimit: Int? = null
    private var currentAdditionalInfo: List<String> = emptyList()
    private var currentConfidenceHigh = false
    private var isCurrentlySpeeding = false

    // API Throttling
    private var lastApiCallTime = 0L
    private var lastApiLocation: Location? = null

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

        if (settingsManager.isDebugModeEnabled) {
            logManager.logDebug("SpeedService: onCreate started")
        }

        try {
            createNotificationChannel()
            startAsForeground()
        } catch (e: Exception) {
            Log.e("SpeedService", "Failed to start foreground service", e)
            logManager.logDebug("SpeedService: Failed to start foreground service", e)
            stopSelf()
            return
        }

        try {
            overlayManager?.show()
            motionDetector.start()

            initLocationUpdates()
            observeMotion()

            if (settingsManager.isDebugModeEnabled) {
                logManager.logDebug("SpeedService: Initialization complete")
            }
        } catch (e: Exception) {
            Log.e("SpeedService", "Failed to initialize service components", e)
            logManager.logDebug("SpeedService: Failed to initialize components", e)
            stopSelf()
        }
    }

    private fun startAsForeground() {
        val notification = createNotification()
        if (notification == null) {
            Log.e("SpeedService", "Failed to create notification")
            logManager.logDebug("SpeedService: Notification creation failed")
            throw RuntimeException("Notification creation failed")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("SpeedService", "Error in startForeground", e)
            logManager.logDebug("SpeedService: Error in startForeground", e)
            throw e
        }
    }

    private fun observeMotion() {
        serviceScope.launch {
            motionDetector.isMovingFlow.collectLatest { isMoving ->
                if (!isMoving) updateUI(speed = 0)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(NotificationManager::class.java)
                if (notificationManager != null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Speed tracking and overlay service"
                    }
                    notificationManager.createNotificationChannel(channel)
                } else {
                    Log.w("SpeedService", "NotificationManager is null")
                    logManager.logDebug("SpeedService: NotificationManager is null")
                }
            } catch (e: Exception) {
                Log.e("SpeedService", "Failed to create notification channel", e)
                logManager.logDebug("SpeedService: Failed to create notification channel", e)
            }
        }
    }

    private fun createNotification(): Notification? {
        return try {
            val stopIntent = Intent(this, SpeedService::class.java).apply { action = STOP_ACTION }

            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getService(this, 1, stopIntent, flag)

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.start_service))
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_service), pendingIntent)
                .setOngoing(true)
                .build()
        } catch (e: Exception) {
            Log.e("SpeedService", "Failed to build notification", e)
            logManager.logDebug("SpeedService: Failed to build notification", e)
            null
        }
    }

    private fun toggleAudioMute() {
        settingsManager.isAudioMutedTemporary = !settingsManager.isAudioMutedTemporary
        overlayManager?.flash()
        hardwareHelper.vibrate()
        updateUI(speedProcessor.lastSpeedKmh.toInt(), isCurrentlySpeeding)
    }

    @SuppressLint("MissingPermission")
    private fun initLocationUpdates() {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, Config.LOCATION_UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(Config.LOCATION_MIN_UPDATE_INTERVAL_MS)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(res: LocationResult) {
                    if (res.locations.isNotEmpty()) {
                        res.lastLocation?.let { processLocation(it) }
                    }
                }
            }
            fusedLocationClient?.requestLocationUpdates(request, locationCallback ?: return, null)
        } catch (e: SecurityException) {
            Log.e("SpeedService", "SecurityException: Location permission missing at runtime", e)
            logManager.logDebug("SpeedService: Location permission missing at runtime", e)
        } catch (e: Exception) {
            Log.e("SpeedService", "Unexpected error in initLocationUpdates", e)
            logManager.logDebug("SpeedService: Unexpected error in initLocationUpdates", e)
        }
    }

    private fun processLocation(location: Location) {
        val speedKmh = location.speed * 3.6f
        val useMph = settingsManager.useMph

        val smoothedSpeed = speedProcessor.getSmoothedSpeed(location, useMph, motionDetector.isMoving)
        val isSpeeding = speedProcessor.isSpeeding(smoothedSpeed, currentLimit, settingsManager.tolerance)

        updateUI(smoothedSpeed, isSpeeding)
        checkSpeedingAlert(isSpeeding)
        checkApiUpdate(location, speedKmh)
    }

    private fun updateUI(speed: Int, isSpeeding: Boolean = false) {
        val unit = if (settingsManager.useMph) "mph" else "km/h"

        overlayManager?.updateState(OverlayState(
            currentSpeed = speed,
            speedLimit = currentLimit,
            unit = unit,
            isSpeeding = isSpeeding,
            isConfidenceHigh = currentConfidenceHigh,
            showHazard = currentAdditionalInfo.any { it == OsmParser.INFO_HAZARD || it == OsmParser.INFO_SCHOOL },
            showCamera = currentAdditionalInfo.contains(OsmParser.INFO_CAMERA),
            isAudioMuted = !settingsManager.isAudioWarningEnabled || settingsManager.isAudioMutedTemporary
        ))

        SpeedWidgetProvider.updateWidget(
            context = this,
            speed = speed,
            limit = currentLimit,
            unit = unit,
            isSpeeding = isSpeeding,
            isConfidenceHigh = currentConfidenceHigh
        )
    }

    private fun checkSpeedingAlert(isSpeeding: Boolean) {
        if (isSpeeding && (currentLimit ?: 0) > 0) {
            if (!isCurrentlySpeeding && settingsManager.isAudioWarningEnabled && !settingsManager.isAudioMutedTemporary) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
        }
        isCurrentlySpeeding = isSpeeding
    }

    private fun checkApiUpdate(location: Location, speedKmh: Float) {
        val time = System.currentTimeMillis()
        val dist = lastApiLocation?.distanceTo(location) ?: Float.MAX_VALUE

        val interval = if (speedKmh > 80) 3000L else 10000L
        val minDist = if (speedKmh > 80) 50f else 15f

        if (time - lastApiCallTime > interval && dist > minDist) {
            lastApiCallTime = time
            lastApiLocation = location
            fetchSpeedLimit(location)
        }
    }

    private fun fetchSpeedLimit(location: Location) {
        serviceScope.launch {
            val speedKmh = location.speed * 3.6f
            val result = speedRepository.fetchSpeedLimit(
                location.latitude,
                location.longitude,
                location.bearing,
                speedKmh,
                settingsManager.showSpeedCameras
            )

            if (result is SpeedResult.Success) {
                currentLimit = result.data
                currentAdditionalInfo = result.additionalInfo
                currentConfidenceHigh = result.isConfidenceHigh
                updateUI(speedProcessor.lastSpeedKmh.toInt(), isCurrentlySpeeding)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        toneGenerator?.release()
        overlayManager?.hide()
        overlayManager?.release()
        motionDetector.stop()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }
}

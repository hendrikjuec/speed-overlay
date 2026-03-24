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
import androidx.core.app.NotificationCompat
import com.drgreen.speedoverlay.R
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
 */
@AndroidEntryPoint
class SpeedService : Service() {

    @Inject lateinit var settingsManager: SettingsManager
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
        overlayManager = OverlayManager(this, settingsManager) { toggleAudioMute() }

        createNotificationChannel()
        startAsForeground()

        overlayManager?.show()
        motionDetector.start()

        initLocationUpdates()
        observeMotion()
    }

    private fun startAsForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Speed tracking and overlay service"
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, SpeedService::class.java).apply { action = STOP_ACTION }

        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getService(this, 1, stopIntent, flag)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.start_service))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_service), pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun toggleAudioMute() {
        settingsManager.isAudioMutedTemporary = !settingsManager.isAudioMutedTemporary
        overlayManager?.flash()
        hardwareHelper.vibrate()
        updateUI(speedProcessor.lastSpeedKmh.toInt(), isCurrentlySpeeding)
    }

    @SuppressLint("MissingPermission")
    private fun initLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, Config.LOCATION_UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(Config.LOCATION_MIN_UPDATE_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                res.locations.lastOrNull()?.let { processLocation(it) }
            }
        }
        fusedLocationClient?.requestLocationUpdates(request, locationCallback!!, null)
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

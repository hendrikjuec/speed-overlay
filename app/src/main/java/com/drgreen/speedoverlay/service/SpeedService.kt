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
 * Foreground service that tracks GPS location, processes speed, and manages the overlay.
 */
@AndroidEntryPoint
class SpeedService : Service() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var hardwareHelper: HardwareHelper
    @Inject lateinit var motionDetector: MotionDetector
    @Inject lateinit var speedRepository: SpeedRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var overlayManager: OverlayManager
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
        const val CHANNEL_ID = "SpeedServiceChannel"
        const val STOP_ACTION = "STOP_SERVICE"
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
        startForeground(1, createNotification())

        overlayManager.show()
        motionDetector.start()

        initLocationUpdates()
        observeMotion()
    }

    /**
     * Observes physical motion to handle standstill detection.
     */
    private fun observeMotion() {
        serviceScope.launch {
            motionDetector.isMovingFlow.collectLatest { isMoving ->
                if (!isMoving) updateOverlay(speed = 0)
            }
        }
    }

    /**
     * Creates the notification channel required for foreground services.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, SpeedService::class.java).apply { action = STOP_ACTION }
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.start_service))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_service), pendingIntent)
            .build()
    }

    /**
     * Toggles audio warning mute state temporarily.
     */
    private fun toggleAudioMute() {
        settingsManager.isAudioMutedTemporary = !settingsManager.isAudioMutedTemporary
        overlayManager.flash()
        hardwareHelper.vibrate()
        // Force an immediate UI update
        updateOverlay(speedProcessor.lastSpeedKmh.toInt(), isCurrentlySpeeding)
    }

    /**
     * Initializes FusedLocationProvider for high accuracy updates.
     */
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
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
    }

    /**
     * Processes new location data to update speed and check for speed limits.
     */
    private fun processLocation(location: Location) {
        val speedKmh = location.speed * 3.6f
        val useMph = settingsManager.useMph

        val smoothedSpeed = speedProcessor.getSmoothedSpeed(location, useMph, motionDetector.isMoving)
        val isSpeeding = speedProcessor.isSpeeding(smoothedSpeed, currentLimit, settingsManager.tolerance)

        updateOverlay(smoothedSpeed, isSpeeding)
        checkSpeedingAlert(isSpeeding)
        checkApiUpdate(location, speedKmh)
    }

    /**
     * Updates the UI state of the overlay.
     */
    private fun updateOverlay(speed: Int, isSpeeding: Boolean = false) {
        overlayManager.updateState(OverlayState(
            currentSpeed = speed,
            speedLimit = currentLimit,
            unit = if (settingsManager.useMph) "mph" else "km/h",
            isSpeeding = isSpeeding,
            isConfidenceHigh = currentConfidenceHigh,
            showHazard = currentAdditionalInfo.any { it == OsmParser.INFO_HAZARD || it == OsmParser.INFO_SCHOOL },
            showCamera = currentAdditionalInfo.contains(OsmParser.INFO_CAMERA),
            isAudioMuted = !settingsManager.isAudioWarningEnabled || settingsManager.isAudioMutedTemporary
        ))
    }

    /**
     * Plays an audio alert if the user is speeding.
     */
    private fun checkSpeedingAlert(isSpeeding: Boolean) {
        if (isSpeeding && currentLimit != null && currentLimit!! > 0) {
            if (!isCurrentlySpeeding && settingsManager.isAudioWarningEnabled && !settingsManager.isAudioMutedTemporary) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
        }
        isCurrentlySpeeding = isSpeeding
    }

    /**
     * Throttles API calls for speed limit updates based on distance and speed.
     */
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

    /**
     * Fetches the current speed limit from the repository.
     */
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
                // Update overlay with new limit info
                updateOverlay(speedProcessor.lastSpeedKmh.toInt(), isCurrentlySpeeding)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        toneGenerator?.release()
        overlayManager.hide()
        overlayManager.release()
        motionDetector.stop()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

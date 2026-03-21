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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.data.SpeedRepository
import com.drgreen.speedoverlay.data.SpeedResult
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpeedService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var settingsManager: SettingsManager
    private lateinit var overlayManager: OverlayManager
    private lateinit var hardwareHelper: HardwareHelper
    private lateinit var motionDetector: MotionDetector

    private var toneGenerator: ToneGenerator? = null

    private var currentLimit: Int? = null
    private var currentAdditionalInfo: List<String> = emptyList()
    private var isCurrentlySpeeding = false

    private val speedProcessor = SpeedProcessor()
    private val speedRepository = SpeedRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var lastApiCallTime = 0L
    private var lastApiLocation: Location? = null

    private companion object {
        const val CHANNEL_ID = "SpeedServiceChannel"
        const val STOP_ACTION = "STOP_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_ACTION) {
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        hardwareHelper = HardwareHelper(this)
        motionDetector = MotionDetector(this)
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

        overlayManager = OverlayManager(this, settingsManager) {
            toggleAudioMute()
        }

        createNotificationChannel()
        startForeground(1, createNotification())

        overlayManager.show()
        motionDetector.start()
        initLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Speed Overlay Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, SpeedService::class.java).apply { action = STOP_ACTION }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun toggleAudioMute() {
        settingsManager.isAudioMutedTemporary = !settingsManager.isAudioMutedTemporary

        val msgRes = if (settingsManager.isAudioMutedTemporary) R.string.audio_muted_on else R.string.audio_muted_off
        Toast.makeText(this, getString(msgRes), Toast.LENGTH_SHORT).show()

        overlayManager.flash()
        hardwareHelper.vibrate()
    }

    @SuppressLint("MissingPermission")
    private fun initLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, Config.LOCATION_UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(Config.LOCATION_MIN_UPDATE_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                res.locations.lastOrNull()?.let { updateSpeed(it) }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun updateSpeed(location: Location) {
        val useMph = settingsManager.useMph
        val speed = speedProcessor.getSmoothedSpeed(location.speed, useMph, motionDetector.isMoving)

        val speeding = speedProcessor.isSpeeding(speed, currentLimit, settingsManager.tolerance)

        overlayManager.updateState(OverlayState(
            currentSpeed = speed,
            speedLimit = currentLimit,
            unit = if (useMph) "mph" else "km/h",
            isSpeeding = speeding,
            showHazard = currentAdditionalInfo.contains("Gefahr") || currentAdditionalInfo.contains("Schule"),
            showCamera = currentAdditionalInfo.contains("Blitzer"),
            isAudioMuted = settingsManager.isAudioMutedTemporary
        ))

        if (speeding && currentLimit != null && currentLimit!! > 0) {
            if (!isCurrentlySpeeding && settingsManager.isAudioWarningEnabled && !settingsManager.isAudioMutedTemporary) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
        }
        isCurrentlySpeeding = speeding

        val speedKmh = if (useMph) speed * 1.609f else speed.toFloat()
        val dynamicInterval = if (speedKmh > 80) 8000L else 4000L
        val dist = lastApiLocation?.distanceTo(location) ?: Float.MAX_VALUE
        val time = System.currentTimeMillis()

        if (time - lastApiCallTime > dynamicInterval && dist > Config.API_CALL_MIN_DISTANCE_METERS) {
            lastApiCallTime = time
            lastApiLocation = location
            fetchSpeedLimit(location.latitude, location.longitude, location.bearing)
        }
    }

    private fun fetchSpeedLimit(lat: Double, lon: Double, heading: Float) {
        serviceScope.launch {
            when (val result = speedRepository.fetchSpeedLimit(lat, lon, heading, settingsManager.showSpeedCameras)) {
                is SpeedResult.Success -> {
                    currentLimit = result.data
                    currentAdditionalInfo = result.additionalInfo
                }
                is SpeedResult.Error -> {
                    currentLimit = null
                    currentAdditionalInfo = emptyList()
                }
                is SpeedResult.Loading -> { }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        toneGenerator?.release()
        overlayManager.hide()
        motionDetector.stop()
        if (::fusedLocationClient.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        speedProcessor.clearHistory()
    }
}

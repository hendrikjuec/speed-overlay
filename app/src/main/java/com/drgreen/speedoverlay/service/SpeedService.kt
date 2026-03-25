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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.LogEntry
import com.drgreen.speedoverlay.data.LogManager
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/**
 * Der Kern-Service, der GPS-Daten verarbeitet und das Overlay steuert.
 * Erkennt Ladestatus und nutzt Sensor Fusion für sofortige Stillstandserkennung.
 */
@AndroidEntryPoint
class SpeedService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var hardwareHelper: HardwareHelper
    @Inject lateinit var motionDetector: MotionDetector
    @Inject lateinit var logManager: LogManager
    @Inject lateinit var speedRepository: SpeedRepository

    private lateinit var overlayManager: OverlayManager
    private var toneGenerator: ToneGenerator? = null

    private var currentLimit: Int? = null
    private var currentAdditionalInfo: List<String> = emptyList()
    private var currentConfidenceHigh = false
    private var isCurrentlySpeeding = false
    private var lastKnownLocation: Location? = null

    private val speedProcessor = SpeedProcessor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var lastApiCallTime = 0L
    private var lastApiLocation: Location? = null
    private var lastApiSpeedKmh = 0f

    private var isLowBatteryMode = false
    private var isCharging = false
    private var lastUsedInterval = -1L

    // --- 📓 Logbook State ---
    private var isLoggingActive = false
    private var logStartTime = 0L
    private var logMaxSpeed = 0
    private var logSpeedSum = 0L
    private var logSampleCount = 0
    private var logStartLocation: Location? = null
    private var lastSpeedingTime = 0L

    private companion object {
        const val CHANNEL_ID = "SpeedServiceChannel"
        const val STOP_ACTION = "STOP_SERVICE"
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            checkBatteryStatus(intent)
        }
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
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

        overlayManager = OverlayManager(this, settingsManager) {
            toggleAudioMute()
        }

        try {
            createNotificationChannel()
            val notification = createNotification()
            if (notification != null) {
                startForeground(1, notification)
            } else {
                Log.e("SpeedService", "Failed to create notification, stopping service")
                stopSelf()
                return
            }
        } catch (e: Exception) {
            Log.e("SpeedService", "Failed to start foreground service", e)
            stopSelf()
            return
        }

        try {
            overlayManager.show()
            motionDetector.start()
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            initLocationUpdates(Config.LOCATION_UPDATE_INTERVAL_MS)
            observeMotion()
        } catch (e: Exception) {
            Log.e("SpeedService", "Failed to initialize service components", e)
            stopSelf()
        }
    }

    private fun observeMotion() {
        serviceScope.launch {
            motionDetector.isMovingFlow.collectLatest { isMoving ->
                // Wenn wir am Strom hängen, ignorieren wir den Stillstand für die GPS-Drosselung,
                // aber für die Geschwindigkeitsanzeige (0 km/h) nutzen wir ihn trotzdem.
                if (!isMoving) {
                    forceZeroSpeed()
                }
            }
        }
    }

    private fun forceZeroSpeed() {
        val useMph = settingsManager.useMph
        overlayManager.updateState(OverlayState(
            currentSpeed = 0,
            speedLimit = currentLimit,
            unit = if (useMph) "mph" else "km/h",
            isSpeeding = false,
            isConfidenceHigh = currentConfidenceHigh,
            showHazard = currentAdditionalInfo.contains("Gefahr") || currentAdditionalInfo.contains("Schule"),
            showCamera = currentAdditionalInfo.contains("Blitzer"),
            isAudioMuted = settingsManager.isAudioMutedTemporary
        ))
        speedProcessor.clearHistory()
    }

    private fun checkBatteryStatus(intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (!settingsManager.isBatteryOptimizationEnabled) {
            if (isLowBatteryMode) disableBatterySaver()
            return
        }

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()

        val shouldBeInLowPower = batteryPct <= Config.BATTERY_LOW_THRESHOLD && !isCharging

        if (shouldBeInLowPower && !isLowBatteryMode) {
            enableBatterySaver()
        } else if (!shouldBeInLowPower && isLowBatteryMode) {
            disableBatterySaver()
        }
    }

    private fun enableBatterySaver() {
        isLowBatteryMode = true
        motionDetector.stop()
        restartLocationUpdates(Config.BATTERY_SAVER_GPS_INTERVAL_MS)
        Toast.makeText(this, "Akkusparmodus aktiv: GPS gedrosselt", Toast.LENGTH_SHORT).show()
    }

    private fun disableBatterySaver() {
        isLowBatteryMode = false
        motionDetector.start()
        restartLocationUpdates(Config.LOCATION_UPDATE_INTERVAL_MS)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(NotificationManager::class.java)
                if (notificationManager != null) {
                    val serviceChannel = NotificationChannel(
                        CHANNEL_ID,
                        "Speed Overlay Service Channel",
                        NotificationManager.IMPORTANCE_LOW
                    )
                    notificationManager.createNotificationChannel(serviceChannel)
                } else {
                    Log.w("SpeedService", "NotificationManager is null")
                }
            } catch (e: Exception) {
                Log.e("SpeedService", "Failed to create notification channel", e)
            }
        }
    }

    private fun createNotification(): Notification? {
        return try {
            val stopIntent = Intent(this, SpeedService::class.java).apply {
                action = STOP_ACTION
            }

            val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                })

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Dienst aktiv")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .build()
        } catch (e: Exception) {
            Log.e("SpeedService", "Failed to build notification", e)
            null
        }
    }

    private fun toggleAudioMute() {
        settingsManager.isAudioMutedTemporary = !settingsManager.isAudioMutedTemporary
        overlayManager.flash()
        hardwareHelper.vibrate()
    }

    @SuppressLint("MissingPermission")
    private fun initLocationUpdates(intervalMs: Long) {
        if (lastUsedInterval == intervalMs) return
        lastUsedInterval = intervalMs

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(res: LocationResult?) {
                    if (res != null && res.locations.isNotEmpty()) {
                        res.lastLocation?.let { updateSpeed(it) }
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            Log.e("SpeedService", "SecurityException: Location permission missing at runtime", e)
        } catch (e: Exception) {
            Log.e("SpeedService", "Unexpected error in initLocationUpdates", e)
        }
    }

    private fun restartLocationUpdates(newInterval: Long) {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            initLocationUpdates(newInterval)
        }
    }

    private fun updateSpeed(location: Location) {
        lastKnownLocation = location
        val useMph = settingsManager.useMph
        val speedKmh = location.speed * 3.6f

        // --- 🔋 Dynamic GPS Interval ---
        val targetInterval = when {
            isCharging -> 300L
            isLowBatteryMode -> Config.BATTERY_SAVER_GPS_INTERVAL_MS
            else -> when {
                speedKmh > 100 -> 300L
                speedKmh > 50 -> 500L
                speedKmh < 5 -> 2000L
                else -> 800L
            }
        }

        if (abs(targetInterval - lastUsedInterval) > 100) {
            restartLocationUpdates(targetInterval)
        }

        // Wir nutzen die Sensoren für die physikalische Bewegungserkennung.
        // Falls Sensoren fehlen, liefert isMoving immer true.
        val speed = speedProcessor.getSmoothedSpeed(location, useMph, motionDetector.isMoving)
        val speeding = speedProcessor.isSpeeding(speed, currentLimit, settingsManager.tolerance)

        overlayManager.updateState(OverlayState(
            currentSpeed = speed,
            speedLimit = currentLimit,
            unit = if (useMph) "mph" else "km/h",
            isSpeeding = speeding,
            isConfidenceHigh = currentConfidenceHigh,
            showHazard = currentAdditionalInfo.contains("Gefahr") || currentAdditionalInfo.contains("Schule"),
            showCamera = currentAdditionalInfo.contains("Blitzer"),
            isAudioMuted = settingsManager.isAudioMutedTemporary
        ))

        handleSpeedingAlerts(speed, speeding)
        handleLogbook(speed, location)

        val time = System.currentTimeMillis()
        val dist = lastApiLocation?.distanceTo(location) ?: Float.MAX_VALUE

        val significantSpeedChange = abs(speedKmh - lastApiSpeedKmh) > Config.SPEED_CHANGE_TRIGGER_KMH
        val isJunctionMode = speedKmh < Config.JUNCTION_SPEED_THRESHOLD_KMH

        var dynamicInterval = when {
            isCharging -> 2000L
            significantSpeedChange -> 1000L
            speedKmh > 100 -> 3500L
            speedKmh > 50 -> 6000L
            isJunctionMode -> 4000L
            else -> 12000L
        }

        var minDistance = if (isCharging) 10f else if (significantSpeedChange || isJunctionMode) 10f else Config.API_CALL_MIN_DISTANCE_METERS

        if (isLowBatteryMode) {
            dynamicInterval *= 2
            minDistance *= Config.BATTERY_SAVER_API_DIST_MULT
        }

        if (time - lastApiCallTime > dynamicInterval && dist > minDistance) {
            lastApiCallTime = time
            lastApiLocation = location
            lastApiSpeedKmh = speedKmh
            fetchSpeedLimit(location.latitude, location.longitude, location.bearing, speedKmh)
        }
    }

    private fun handleSpeedingAlerts(speed: Int, speeding: Boolean) {
        if (speeding && currentLimit != null && currentLimit!! > 0) {
            if (!isCurrentlySpeeding && settingsManager.isAudioWarningEnabled && !settingsManager.isAudioMutedTemporary) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
        }
        isCurrentlySpeeding = speeding
    }

    private fun handleLogbook(speed: Int, location: Location) {
        val time = System.currentTimeMillis()
        val limit = currentLimit ?: 0
        val isDeviation = limit > 0 && speed > (limit + Config.LOG_SPEED_THRESHOLD_KMH)

        if (isDeviation) {
            lastSpeedingTime = time
            if (!isLoggingActive) {
                startLog(speed, location, limit)
            } else {
                updateLog(speed)
            }
        } else if (isLoggingActive) {
            if (time - lastSpeedingTime > (Config.LOG_COOLDOWN_SECONDS * 1000L)) {
                finishLog(location)
            } else {
                updateLog(speed)
            }
        }
    }

    private fun startLog(speed: Int, location: Location, limit: Int) {
        isLoggingActive = true
        logStartTime = System.currentTimeMillis()
        logMaxSpeed = speed
        logSpeedSum = speed.toLong()
        logSampleCount = 1
        logStartLocation = location
    }

    private fun updateLog(speed: Int) {
        if (speed > logMaxSpeed) logMaxSpeed = speed
        logSpeedSum += speed
        logSampleCount++
    }

    private fun finishLog(endLocation: Location) {
        isLoggingActive = false
        val startLoc = logStartLocation ?: return
        val avgSpeed = (logSpeedSum / logSampleCount).toInt()

        val entry = LogEntry(
            id = UUID.randomUUID().toString(),
            startTime = logStartTime,
            endTime = System.currentTimeMillis(),
            speedLimit = currentLimit ?: 0,
            maxSpeed = logMaxSpeed,
            avgSpeed = avgSpeed,
            startLat = startLoc.latitude,
            startLon = startLoc.longitude,
            endLat = endLocation.latitude,
            endLon = endLocation.longitude,
            unit = if (settingsManager.useMph) "mph" else "km/h"
        )

        serviceScope.launch(Dispatchers.IO) {
            logManager.saveLog(entry)
        }
    }

    private fun fetchSpeedLimit(lat: Double, lon: Double, heading: Float, speedKmh: Float) {
        serviceScope.launch {
            when (val result = speedRepository.fetchSpeedLimit(lat, lon, heading, speedKmh, settingsManager.showSpeedCameras)) {
                is SpeedResult.Success -> {
                    currentLimit = result.data
                    currentAdditionalInfo = result.additionalInfo
                    currentConfidenceHigh = result.isConfidenceHigh
                }
                is SpeedResult.Error -> { }
                is SpeedResult.Loading -> { }
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
        unregisterReceiver(batteryReceiver)
        if (::fusedLocationClient.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        speedProcessor.clearHistory()
    }
}

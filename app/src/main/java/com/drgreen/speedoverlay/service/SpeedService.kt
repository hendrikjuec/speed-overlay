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
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.SettingsManager
import com.drgreen.speedoverlay.data.SpeedRepository
import com.drgreen.speedoverlay.data.SpeedResult
import com.drgreen.speedoverlay.logic.SpeedProcessor
import com.drgreen.speedoverlay.ui.OverlayTouchListener
import com.drgreen.speedoverlay.util.Config
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

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var settingsManager: SettingsManager
    private var toneGenerator: ToneGenerator? = null

    private lateinit var tvCurrentSpeed: TextView
    private lateinit var tvSpeedLimit: TextView
    private lateinit var tvUnit: TextView

    private var currentLimit: Int? = null
    private var isCurrentlySpeeding = false

    private val speedProcessor = SpeedProcessor()
    private val speedRepository = SpeedRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var lastApiCallTime = 0L
    private var lastApiLocation: Location? = null

    private companion object {
        const val CHANNEL_ID = "SpeedServiceChannel"
        const val STOP_ACTION = "STOP_SERVICE"
        const val INITIAL_X = 100
        const val INITIAL_Y = 100
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
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        createNotificationChannel()
        startForeground(1, createNotification())
        initOverlay()
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
            .setContentTitle("Speed Overlay Active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)
        tvCurrentSpeed = overlayView.findViewById(R.id.current_speed_text)
        tvSpeedLimit = overlayView.findViewById(R.id.speed_limit_text)
        tvUnit = overlayView.findViewById(R.id.unit_text)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = INITIAL_X
            y = INITIAL_Y
        }

        updateOverlayVisuals(params)

        overlayView.setOnTouchListener(OverlayTouchListener(windowManager, overlayView, params))
        windowManager.addView(overlayView, params)
    }

    private fun updateOverlayVisuals(params: WindowManager.LayoutParams? = null) {
        val scale = settingsManager.overlaySize
        overlayView.scaleX = scale
        overlayView.scaleY = scale

        if (params != null || overlayView.layoutParams is WindowManager.LayoutParams) {
            val p = params ?: overlayView.layoutParams as WindowManager.LayoutParams
            overlayView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            p.width = (overlayView.measuredWidth * scale).toInt()
            p.height = (overlayView.measuredHeight * scale).toInt()
            if (params == null) windowManager.updateViewLayout(overlayView, p)
        }

        overlayView.alpha = settingsManager.overlayAlpha
        tvCurrentSpeed.setTextColor(settingsManager.overlayTextColor)
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
        val speed = speedProcessor.getSmoothedSpeed(location.speed, useMph)

        tvCurrentSpeed.text = speed.toString()
        tvUnit.text = if (useMph) "mph" else "km/h"

        updateOverlayVisuals()

        val speeding = speedProcessor.isSpeeding(speed, currentLimit, settingsManager.tolerance)
        if (speeding) {
            overlayView.setBackgroundResource(R.drawable.overlay_bg_warning)
            if (!isCurrentlySpeeding && settingsManager.isAudioWarningEnabled) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
        } else {
            overlayView.setBackgroundResource(R.drawable.overlay_bg)
        }
        isCurrentlySpeeding = speeding

        // API Call Logic
        val speedKmh = if (useMph) speed * 1.609f else speed.toFloat()
        val dynamicInterval = if (speedKmh > 80) 10000L else 5000L
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
            // Passing heading for direction-aware matching
            when (val result = speedRepository.fetchSpeedLimit(lat, lon, heading)) {
                is SpeedResult.Success -> {
                    currentLimit = result.data
                    tvSpeedLimit.text = result.data?.toString() ?: "--"
                    tvSpeedLimit.alpha = 1.0f
                }
                is SpeedResult.Error -> {
                    tvSpeedLimit.alpha = 0.5f
                }
                is SpeedResult.Loading -> { }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        toneGenerator?.release()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        if (::fusedLocationClient.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        speedProcessor.clearHistory()
    }
}

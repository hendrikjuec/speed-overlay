/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Nutzt Beschleunigungssensor und Gyroskop, um physische Bewegungen des Geräts zu erkennen.
 * Ermöglicht eine sofortige Stillstandserkennung unabhängig vom GPS-Status (z.B. in Tunneln oder Ampeln).
 */
class MotionDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _isMovingFlow = MutableStateFlow(true)
    /** Ein reaktiver Flow, der den aktuellen Bewegungsstatus liefert. */
    val isMovingFlow: StateFlow<Boolean> = _isMovingFlow.asStateFlow()

    private var lastMotionTimestamp = System.currentTimeMillis()

    /**
     * Sofortige Abfrage des Bewegungsstatus.
     * Falls keine Sensoren vorhanden sind, wird standardmäßig 'true' zurückgegeben.
     */
    val isMoving: Boolean
        get() {
            if (accelerometer == null && gyroscope == null) return true
            val now = System.currentTimeMillis()
            val moving = (now - lastMotionTimestamp) < Config.SENSOR_STILLSTAND_DELAY_MS
            if (_isMovingFlow.value != moving) {
                _isMovingFlow.value = moving
            }
            return moving
        }

    /** Startet die Überwachung der Sensoren. */
    fun start() {
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    /** Stoppt die Überwachung der Sensoren. */
    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                // Optimierung: Quadratvergleich statt Wurzel, falls Performance kritisch wird.
                // Hier bleiben wir bei sqrt für die Lesbarkeit, da SENSOR_DELAY_UI moderat ist.
                val acceleration = sqrt(x * x + y * y + z * z)

                if (acceleration > Config.ACCELERATION_THRESHOLD) {
                    updateMotion(now)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val rotation = sqrt(x * x + y * y + z * z)

                if (rotation > Config.GYRO_THRESHOLD) {
                    updateMotion(now)
                }
            }
        }
    }

    private fun updateMotion(timestamp: Long) {
        lastMotionTimestamp = timestamp
        if (!_isMovingFlow.value) {
            _isMovingFlow.value = true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

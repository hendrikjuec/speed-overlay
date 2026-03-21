/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import android.content.Context
import android.hardware.Sensor
import android.content.Context.SENSOR_SERVICE
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Uses Accelerometer and Gyroscope to detect if the device is physically moving.
 * This helps to distinguish between GPS jitter and actual vehicle movement.
 */
class MotionDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var lastMotionTimestamp = 0L

    /**
     * Wenn keine Sensoren vorhanden sind, gehen wir standardmäßig von Bewegung aus,
     * um die GPS-Logik nicht zu blockieren.
     */
    val isMoving: Boolean
        get() {
            if (accelerometer == null && gyroscope == null) return true
            return (System.currentTimeMillis() - lastMotionTimestamp) < Config.SENSOR_STILLSTAND_DELAY_MS
        }

    fun start() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val acceleration = sqrt(x * x + y * y + z * z)
                if (acceleration > Config.ACCELERATION_THRESHOLD) {
                    lastMotionTimestamp = System.currentTimeMillis()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val rotation = sqrt(x * x + y * y + z * z)
                if (rotation > Config.GYRO_THRESHOLD) {
                    lastMotionTimestamp = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

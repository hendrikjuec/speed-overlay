/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Centrally manages permissions for the application, handling both legacy (Android 9)
 * and modern (Android 14+) requirements.
 */
class PermissionManager(private val activity: Activity) {

    companion object {
        const val REQ_LOCATION = 100
        const val REQ_OVERLAY = 101
        const val REQ_BT = 102
        const val REQ_POST_NOTIFICATIONS = 103
    }

    /**
     * Checks for location permission. Android 10+ requires background location
     * for foreground services with type 'location', but since we use a
     * Foreground Service, 'fine_location' is generally sufficient if the
     * service is visible to the user.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQ_LOCATION
        )
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(activity)
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${activity.packageName}".toUri()
        )
        activity.startActivityForResult(intent, REQ_OVERLAY)
    }

    /**
     * Handles Bluetooth permissions which changed significantly in Android 12 (API 31).
     */
    fun hasBluetoothPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> true // Android 9-11 doesn't require runtime BLUETOOTH_CONNECT
        }
    }

    fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQ_BT
            )
        }
    }

    /**
     * Required for Android 13+ (API 33) to show the foreground service notification.
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS
            )
        }
    }

    fun hasAllCriticalPermissions(): Boolean {
        return hasLocationPermission() && hasOverlayPermission() && hasNotificationPermission()
    }
}

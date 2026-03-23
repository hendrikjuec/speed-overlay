/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Zentrales Management für Berechtigungen.
 * Behandelt sowohl Legacy-Anforderungen (Android 9) als auch moderne (Android 16+).
 */
open class PermissionManager(private val context: Context) {

    companion object {
        const val REQ_LOCATION = 100
        const val REQ_OVERLAY = 101
        const val REQ_POST_NOTIFICATIONS = 103
    }

    /** Prüft auf Standortberechtigung (Fine Location). */
    open fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Fordert die Standortberechtigung an. */
    open fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQ_LOCATION
        )
    }

    /** Prüft, ob die App über anderen Apps eingeblendet werden darf. */
    open fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /** Öffnet das System-Menü für die Overlay-Berechtigung. */
    open fun requestOverlayPermission(activity: Activity) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            activity.startActivity(intent)
        }
    }

    /** Prüft auf Benachrichtigungsberechtigung (ab Android 13 erforderlich). */
    open fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Fordert Benachrichtigungsberechtigung an. */
    open fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS
            )
        }
    }

    /** Prüft, ob alle für den Kernbetrieb kritischen Berechtigungen vorhanden sind. */
    open fun hasAllCriticalPermissions(): Boolean {
        return hasLocationPermission() && hasOverlayPermission() && hasNotificationPermission()
    }
}

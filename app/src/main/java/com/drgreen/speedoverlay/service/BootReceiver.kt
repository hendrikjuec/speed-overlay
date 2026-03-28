/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.drgreen.speedoverlay.data.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsManager: SettingsManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // Wir prüfen synchron, ob der Autostart aktiviert ist
            val isEnabled = runBlocking {
                settingsManager.autostartBootFlow.first()
            }

            if (isEnabled) {
                Log.d("BootReceiver", "Autostart is enabled. Starting SpeedService...")
                val serviceIntent = Intent(context, SpeedService::class.java)
                context.startForegroundService(serviceIntent)
            } else {
                Log.d("BootReceiver", "Autostart is disabled. Skipping service start.")
            }
        }
    }
}

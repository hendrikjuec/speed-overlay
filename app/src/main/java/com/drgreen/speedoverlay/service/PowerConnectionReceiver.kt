/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drgreen.speedoverlay.data.SettingsManager

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsManager(context)
        if (!settings.isAutostartPowerEnabled) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val serviceIntent = Intent(context, SpeedService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                context.stopService(Intent(context, SpeedService::class.java))
            }
        }
    }
}

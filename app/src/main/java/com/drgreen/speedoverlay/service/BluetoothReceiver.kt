/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.drgreen.speedoverlay.data.SettingsManager

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settingsManager = SettingsManager(context)
        if (!settingsManager.isAutostartBtEnabled) return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        }

        val targetAddress = settingsManager.autostartBtDeviceAddress

        // If a specific device is selected, only start if it matches.
        if (targetAddress != null && device?.address != targetAddress) return

        // If no specific device is selected, we don't start (safer for battery)
        if (targetAddress == null) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val startIntent = Intent(context, SpeedService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(startIntent)
                } else {
                    context.startService(startIntent)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                context.stopService(Intent(context, SpeedService::class.java))
            }
        }
    }
}

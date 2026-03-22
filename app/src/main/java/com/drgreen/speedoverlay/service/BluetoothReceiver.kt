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

/**
 * Empfängt Bluetooth-Ereignisse (Verbindung/Trennung), um den SpeedService automatisch zu starten oder zu stoppen.
 * Berücksichtigt die Benutzereinstellungen für Autostart und spezifische Bluetooth-Geräte.
 */
class BluetoothReceiver : BroadcastReceiver() {

    /**
     * Verarbeitet eingehende Intents für Bluetooth-Verbindungsstatusänderungen.
     * Startet den [SpeedService], wenn ein konfiguriertes Gerät verbunden wird.
     * Stoppt den [SpeedService], wenn die Verbindung getrennt wird.
     */
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

        // Falls kein spezifisches Gerät ausgewählt ist, starten wir nicht (Sicherheit für Akku)
        if (targetAddress == null) return

        // Falls ein spezifisches Gerät ausgewählt ist, nur verarbeiten wenn es übereinstimmt.
        if (device?.address != targetAddress) return

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

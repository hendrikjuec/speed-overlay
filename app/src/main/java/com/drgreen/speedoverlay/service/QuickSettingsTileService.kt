/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val isServiceRunning = isServiceRunning(SpeedService::class.java)

        if (isServiceRunning) {
            val stopIntent = Intent(this, SpeedService::class.java).apply {
                action = SpeedService.STOP_ACTION
            }
            // Ab Android 8 (O) muss startService für Hintergrund-Aktionen vorsichtig genutzt werden,
            // aber für STOP_ACTION eines laufenden Services ist es okay.
            startService(stopIntent)
        } else {
            val startIntent = Intent(this, SpeedService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
        }

        // Kachel-Status sofort aktualisieren (Zustandswechsel wird visuell sichtbar)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isServiceRunning = isServiceRunning(SpeedService::class.java)
        tile.state = if (isServiceRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    /**
     * Prüft, ob der eigene Service läuft.
     * Hinweis: getRunningServices ist für eigene Services weiterhin funktionsfähig.
     */
    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

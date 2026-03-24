/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.service

import android.app.ActivityManager
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint

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
            startService(stopIntent)
        } else {
            val startIntent = Intent(this, SpeedService::class.java)
            startForegroundService(startIntent)
        }

        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isServiceRunning = isServiceRunning(SpeedService::class.java)
        tile.state = if (isServiceRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val runningServices = manager.getRunningServices(Int.MAX_VALUE) ?: return false
        for (service in runningServices) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

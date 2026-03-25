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
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        try {
            super.onClick()
            val isServiceRunning = isServiceRunning(SpeedService::class.java)
            if (isServiceRunning) {
                val stopIntent = Intent(this, SpeedService::class.java).apply {
                    action = "STOP_SERVICE"
                }
                startService(stopIntent)
            } else {
                val startIntent = Intent(this, SpeedService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(startIntent)
                } else {
                    startService(startIntent)
                }
            }
            updateTile()
        } catch (e: Exception) {
            Log.e("QuickSettingsTileService", "onClick failed", e)
        }
    }

    private fun updateTile() {
        try {
            val tile = qsTile ?: return
            val isServiceRunning = isServiceRunning(SpeedService::class.java)
            tile.state = if (isServiceRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        } catch (e: Exception) {
            Log.w("QuickSettingsTileService", "updateTile failed", e)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        return try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            manager?.getRunningServices(5)?.any {
                serviceClass.name == it.service.className
            } ?: false
        } catch (e: Exception) {
            Log.w("QuickSettingsTileService", "Cannot check service status", e)
            false
        }
    }
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.drgreen.speedoverlay.util.Config
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Verwaltet das Logbuch für Abweichungsetappen und persistiert diese in den SharedPreferences.
 */
class LogManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("speed_overlay_logbook", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_LOGS = "deviation_logs"
    }

    /**
     * Speichert einen neuen LogEntry und entfernt ggf. den ältesten Eintrag, wenn das Limit erreicht ist.
     */
    fun saveLog(entry: LogEntry) {
        val logs = getAllLogs().toMutableList()
        logs.add(0, entry) // Neuester Eintrag zuerst

        if (logs.size > Config.MAX_LOG_ENTRIES) {
            logs.removeAt(logs.size - 1)
        }

        prefs.edit {
            putString(KEY_LOGS, gson.toJson(logs))
        }
    }

    /**
     * Gibt alle gespeicherten LogEntries zurück.
     */
    fun getAllLogs(): List<LogEntry> {
        val json = prefs.getString(KEY_LOGS, null) ?: return emptyList()
        val type = object : TypeToken<List<LogEntry>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Löscht das komplette Logbuch.
     */
    fun clearLogs() {
        prefs.edit { remove(KEY_LOGS) }
    }
}

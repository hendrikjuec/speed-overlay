/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import com.drgreen.speedoverlay.util.Config
import kotlinx.coroutines.flow.Flow

/**
 * Verwaltet das Logbuch für Abweichungsetappen über die Room Database.
 */
class LogManager(context: Context) {
    private val logDao = LogDatabase.getDatabase(context).logDao()

    /**
     * Speichert einen neuen LogEntry und entfernt ggf. den ältesten Eintrag, wenn das Limit erreicht ist.
     */
    suspend fun saveLog(entry: LogEntry) {
        logDao.insertLog(entry)

        val count = logDao.getLogCount()
        if (count > Config.MAX_LOG_ENTRIES) {
            logDao.deleteOldestLogs(count - Config.MAX_LOG_ENTRIES)
        }
    }

    /**
     * Gibt alle gespeicherten LogEntries als reaktiven Flow zurück.
     */
    fun getAllLogs(): Flow<List<LogEntry>> {
        return logDao.getAllLogs()
    }

    /**
     * Löscht das komplette Logbuch.
     */
    suspend fun clearLogs() {
        logDao.clearAllLogs()
    }
}

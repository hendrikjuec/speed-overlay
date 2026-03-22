/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import com.drgreen.speedoverlay.util.Config
import kotlinx.coroutines.flow.Flow

/**
 * Verwaltet das Logbuch der Anwendung.
 * Speichert Fahrten mit Geschwindigkeitsüberschreitungen in einer Room-Datenbank.
 * Stellt sicher, dass das Logbuch die maximale Anzahl an Einträgen (Config.MAX_LOG_ENTRIES) nicht überschreitet.
 */
class LogManager(context: Context) {
    private val logDao = LogDatabase.getDatabase(context).logDao()

    /**
     * Speichert einen neuen Log-Eintrag.
     * Wenn das Limit für Log-Einträge erreicht ist, werden die ältesten Einträge automatisch gelöscht.
     *
     * @param entry Das zu speichernde [LogEntry]-Objekt.
     */
    suspend fun saveLog(entry: LogEntry) {
        logDao.insertLog(entry)

        val count = logDao.getLogCount()
        if (count > Config.MAX_LOG_ENTRIES) {
            logDao.deleteOldestLogs(count - Config.MAX_LOG_ENTRIES)
        }
    }

    /**
     * Gibt einen Flow aller Log-Einträge zurück, sortiert nach Zeit.
     */
    fun getAllLogs(): Flow<List<LogEntry>> {
        return logDao.getAllLogs()
    }

    /**
     * Löscht alle Einträge aus dem Logbuch unwiderruflich.
     */
    suspend fun clearLogs() {
        logDao.clearAllLogs()
    }
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) für die Log-Einträge.
 */
@Dao
interface LogDao {
    @Query("SELECT * FROM log_entries ORDER BY startTime DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: LogEntry)

    @Query("DELETE FROM log_entries WHERE id IN (SELECT id FROM log_entries ORDER BY startTime ASC LIMIT :count)")
    suspend fun deleteOldestLogs(count: Int)

    @Query("DELETE FROM log_entries")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun getLogCount(): Int
}

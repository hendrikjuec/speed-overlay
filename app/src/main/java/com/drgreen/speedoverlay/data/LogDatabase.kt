/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Die Room-Datenbank für das Projekt.
 */
@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: LogDatabase? = null

        fun getDatabase(context: Context): LogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "speed_overlay_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

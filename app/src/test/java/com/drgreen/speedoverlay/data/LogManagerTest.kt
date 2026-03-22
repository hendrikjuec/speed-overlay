/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.drgreen.speedoverlay.util.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class LogManagerTest {

    private lateinit var db: LogDatabase
    private lateinit var logDao: LogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        logDao = db.logDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test log insertion and limit`() = runBlocking {
        val entries = (1..60).map { i ->
            LogEntry(
                id = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis() + i,
                endTime = System.currentTimeMillis() + i + 1000,
                speedLimit = 50,
                maxSpeed = 70,
                avgSpeed = 65,
                startLat = 52.0,
                startLon = 7.0,
                endLat = 52.1,
                endLon = 7.1,
                unit = "km/h"
            )
        }

        // Wir nutzen direkt das DAO für den Test der Logik
        entries.forEach { logDao.insertLog(it) }

        var count = logDao.getLogCount()
        assertEquals(60, count)

        // Simuliere LogManager Limit-Logik
        if (count > Config.MAX_LOG_ENTRIES) {
            logDao.deleteOldestLogs(count - Config.MAX_LOG_ENTRIES)
        }

        count = logDao.getLogCount()
        assertEquals(Config.MAX_LOG_ENTRIES, count)

        val allLogs = logDao.getAllLogs().first()
        assertEquals(Config.MAX_LOG_ENTRIES, allLogs.size)
    }
}

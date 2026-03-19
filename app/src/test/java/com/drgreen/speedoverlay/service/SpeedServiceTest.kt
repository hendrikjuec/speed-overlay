/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpeedServiceTest {

    private lateinit var context: Context
    private lateinit var controller: ServiceController<SpeedService>
    private lateinit var service: SpeedService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun testServiceStartStop() {
        val intent = Intent(context, SpeedService::class.java)
        controller = Robolectric.buildService(SpeedService::class.java, intent)
        service = controller.create().get()

        // Initializing with startCommand to trigger notification channel creation
        service.onStartCommand(intent, 0, 0)

        // Check if notification channel was created
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel("SpeedServiceChannel")
            assertNotNull(channel)
        }

        controller.destroy()
    }

    @Test
    fun testStopAction() {
        val intent = Intent(context, SpeedService::class.java).apply {
            action = "STOP_SERVICE"
        }
        controller = Robolectric.buildService(SpeedService::class.java, intent)
        service = controller.create().get()
        service.onStartCommand(intent, 0, 0)

        assertTrue(shadowOf(service).isStoppedBySelf)
    }
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.content.Context
import android.view.WindowManager
import com.drgreen.speedoverlay.data.LogManager
import com.drgreen.speedoverlay.data.SettingsManager
import io.mockk.*
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OverlayManagerTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager
    private lateinit var logManager: LogManager
    private lateinit var overlayManager: OverlayManager
    private val windowManager = mockk<WindowManager>(relaxed = true)

    @Before
    fun setup() {
        context = spyk(RuntimeEnvironment.getApplication())
        every { context.getSystemService(Context.WINDOW_SERVICE) } returns windowManager

        settingsManager = SettingsManager(context)
        logManager = mockk(relaxed = true)
        overlayManager = OverlayManager(context, settingsManager, logManager) { }
    }

    @Test
    fun `test overlay manager show and hide`() {
        overlayManager.show()
        verify { windowManager.addView(any(), any()) }

        overlayManager.hide()
        verify { windowManager.removeView(any()) }
    }

    @Test
    fun `test update state`() {
        val state = OverlayState(
            currentSpeed = 50,
            speedLimit = 50,
            unit = "km/h",
            isSpeeding = false
        )
        overlayManager.updateState(state)
        // State update happens via Compose state, which is hard to verify in unit test
        // but we can verify it doesn't crash
        assertNotNull(overlayManager)
    }

    @Test
    fun `test release`() {
        overlayManager.release()
        // verify no crash
        assertNotNull(overlayManager)
    }
}

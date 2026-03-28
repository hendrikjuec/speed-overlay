/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.drgreen.speedoverlay.data.LogManager
import com.drgreen.speedoverlay.data.SettingsManager

/**
 * Manages the floating overlay window using Jetpack Compose.
 * Uses reactive flows from SettingsManager for immediate UI updates when settings change.
 * Optimized with robust error handling for Android 9+ Head Units.
 *
 * @property context The application context.
 * @property settings The settings manager for reactive configuration.
 * @property logManager The log manager for debug logging.
 * @property onLongClick Callback triggered when the overlay is long-pressed.
 */
class OverlayManager(
    private val context: Context,
    private val settings: SettingsManager,
    private val logManager: LogManager,
    private val onLongClick: () -> Unit
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams

    private val overlayState = mutableStateOf<OverlayState?>(null)

    // Lifecycle requirements for using ComposeView in a Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /**
     * Creates and displays the overlay window.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (composeView != null) return

        if (settings.isDebugModeEnabled) {
            logManager.logDebug("OverlayManager: Attempting to show overlay")
        }

        try {
            composeView = ComposeView(context).apply {
                // Set required owners for Compose
                setViewTreeLifecycleOwner(this@OverlayManager)
                setViewTreeViewModelStoreOwner(this@OverlayManager)
                setViewTreeSavedStateRegistryOwner(this@OverlayManager)

                setContent {
                    val size by settings.overlaySizeFlow.collectAsState(initial = settings.overlaySize)
                    val alpha by settings.overlayAlphaFlow.collectAsState(initial = settings.overlayAlpha)
                    val colorInt by settings.overlayTextColorFlow.collectAsState(initial = settings.overlayTextColor)

                    overlayState.value?.let { state ->
                        OverlayScreen(
                            state = state,
                            scale = size,
                            alpha = alpha,
                            textColor = Color(colorInt)
                        )
                    }
                }
            }

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = settings.overlayX
                y = settings.overlayY
            }

            composeView?.setOnTouchListener(OverlayTouchListener(windowManager, composeView!!, params, settings, onLongClick))

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            windowManager.addView(composeView, params)

            if (settings.isDebugModeEnabled) {
                logManager.logDebug("OverlayManager: Overlay successfully added to window manager")
            }
        } catch (e: WindowManager.BadTokenException) {
            Log.e("OverlayManager", "BadToken: Window token invalid on Android 9 Head Unit", e)
            logManager.logDebug("OverlayManager: BadTokenException", e)
            composeView = null
        } catch (e: SecurityException) {
            Log.e("OverlayManager", "SecurityException: SYSTEM_ALERT_WINDOW permission issue", e)
            logManager.logDebug("OverlayManager: SecurityException", e)
            composeView = null
        } catch (e: RuntimeException) {
            Log.e("OverlayManager", "RuntimeException: Failed to show overlay", e)
            logManager.logDebug("OverlayManager: RuntimeException", e)
            composeView = null
        } catch (e: Exception) {
            Log.e("OverlayManager", "Unexpected error in show()", e)
            logManager.logDebug("OverlayManager: Unexpected error in show()", e)
            composeView = null
        }
    }

    /**
     * Updates the data to be displayed (speed, limit, etc.).
     *
     * @param state The new state of the overlay.
     */
    fun updateState(state: OverlayState) {
        overlayState.value = state
    }

    /**
     * Triggers a visual flash effect (handled internally by Compose animations).
     */
    fun flash() {
        // Compose handles animations internally in OverlayScreen based on state changes
    }

    /**
     * Removes the overlay window from the screen.
     */
    fun hide() {
        composeView?.let {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            windowManager.removeView(it)
            composeView = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    /**
     * Releases resources.
     */
    fun release() {
        // No-op as flows are managed via the Compose lifecycle
    }
}

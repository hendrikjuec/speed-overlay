/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.drgreen.speedoverlay.data.SettingsManager

/**
 * Verwaltet das schwebende Overlay-Fenster unter Verwendung von Jetpack Compose.
 * Nutzt reaktive Flows von SettingsManager für sofortige UI-Updates bei Einstellungsänderungen.
 */
class OverlayManager(
    private val context: Context,
    private val settings: SettingsManager,
    private val onLongClick: () -> Unit
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams

    private val overlayState = mutableStateOf<OverlayState?>(null)

    // Lifecycle-Anforderungen für die Nutzung von ComposeView in einem Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /** Erstellt und zeigt das Overlay-Fenster an. */
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (composeView != null) return

        composeView = ComposeView(context).apply {
            // Notwendige Owner für Compose setzen
            setViewTreeLifecycleOwner(this@OverlayManager)
            setViewTreeViewModelStoreOwner(this@OverlayManager)
            setViewTreeSavedStateRegistryOwner(this@OverlayManager)

            setContent {
                val size by settings.overlaySizeFlow.collectAsState(initial = settings.overlaySize)
                val alpha by settings.overlayAlphaFlow.collectAsState(initial = settings.overlayAlpha)
                val color by settings.overlayTextColorFlow.collectAsState(initial = settings.overlayTextColor)

                overlayState.value?.let { state ->
                    OverlayScreen(
                        state = state,
                        scale = size,
                        alpha = alpha,
                        textColor = color
                    )
                }
            }
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
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
    }

    /** Aktualisiert die anzuzeigenden Daten (Geschwindigkeit, Limit etc.). */
    fun updateState(state: OverlayState) {
        overlayState.value = state
    }

    fun flash() {
        // Compose handhabt Animationen intern in OverlayScreen basierend auf State-Änderungen
    }

    /** Entfernt das Overlay-Fenster vom Bildschirm. */
    fun hide() {
        composeView?.let {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            windowManager.removeView(it)
            composeView = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    /** Gibt Ressourcen frei. */
    fun release() {
        // No-op, da Flows über den Compose-Lifecycle verwaltet werden.
    }
}

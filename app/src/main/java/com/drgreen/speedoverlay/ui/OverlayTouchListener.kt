/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.drgreen.speedoverlay.data.SettingsManager

/**
 * Behandelt Touch-Ereignisse auf dem Overlay, um Verschiebung und Long-Press zu ermöglichen.
 * Das Overlay bleibt frei positionierbar und snappt nicht an den Rand.
 */
class OverlayTouchListener(
    private val windowManager: WindowManager,
    private val overlayView: View,
    private val params: WindowManager.LayoutParams,
    private val settingsManager: SettingsManager,
    private val onLongClick: () -> Unit = {}
) : View.OnTouchListener {
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val gestureDetector = GestureDetector(overlayView.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            onLongClick()
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val metrics = v.resources.displayMetrics

                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()

                // Begrenze Position auf den Bildschirm, damit das Overlay nicht verloren geht
                params.x = params.x.coerceIn(0, metrics.widthPixels - v.width)
                params.y = params.y.coerceIn(0, metrics.heightPixels - v.height)

                windowManager.updateViewLayout(overlayView, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Speichere die Position permanent ab
                settingsManager.overlayX = params.x
                settingsManager.overlayY = params.y
                v.performClick()
                return true
            }
        }
        return false
    }
}

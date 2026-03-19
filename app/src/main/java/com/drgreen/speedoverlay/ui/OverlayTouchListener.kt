/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class OverlayTouchListener(
    private val windowManager: WindowManager,
    private val overlayView: View,
    private val params: WindowManager.LayoutParams
) : View.OnTouchListener {
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
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
                params.x = (initialX + (event.rawX - initialTouchX).toInt()).coerceIn(0, metrics.widthPixels - v.width)
                params.y = (initialY + (event.rawY - initialTouchY).toInt()).coerceIn(0, metrics.heightPixels - v.height)
                windowManager.updateViewLayout(overlayView, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                v.performClick()
                return true
            }
        }
        return false
    }
}

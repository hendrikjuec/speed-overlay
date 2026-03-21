/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.data.SettingsManager

/**
 * Manages the floating overlay UI, including layout, scaling, and state updates.
 */
class OverlayManager(
    private val context: Context,
    private val settings: SettingsManager,
    private val onLongClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayWrapper: FrameLayout? = null
    private var innerContentView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var tvCurrentSpeed: TextView? = null
    private var tvSpeedLimit: TextView? = null
    private var tvUnit: TextView? = null
    private var ivHazard: ImageView? = null
    private var ivCamera: ImageView? = null
    private var ivMuteStatus: ImageView? = null
    private var infoContainer: View? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (overlayWrapper != null) return

        overlayWrapper = FrameLayout(context)

        innerContentView = LayoutInflater.from(context).inflate(R.layout.overlay_view, overlayWrapper, false)
        innerContentView?.let { view ->
            tvCurrentSpeed = view.findViewById(R.id.current_speed_text)
            tvSpeedLimit = view.findViewById(R.id.speed_limit_text)
            tvUnit = view.findViewById(R.id.unit_text)
            ivHazard = view.findViewById(R.id.iv_hazard)
            ivCamera = view.findViewById(R.id.iv_camera)
            ivMuteStatus = view.findViewById(R.id.iv_mute_status)
            infoContainer = view.findViewById(R.id.info_container)

            overlayWrapper?.addView(view)

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
                x = 100
                y = 100
            }

            // Pivot top-left for scaling
            view.pivotX = 0f
            view.pivotY = 0f

            updateVisuals()

            overlayWrapper?.setOnTouchListener(OverlayTouchListener(windowManager, overlayWrapper!!, params, onLongClick))
            windowManager.addView(overlayWrapper, params)
        }
    }

    fun updateState(state: OverlayState) {
        tvCurrentSpeed?.text = state.currentSpeed.toString()
        tvUnit?.text = state.unit

        updateSpeedLimitUI(state.speedLimit)

        ivHazard?.visibility = if (state.showHazard) View.VISIBLE else View.GONE
        ivCamera?.visibility = if (state.showCamera) View.VISIBLE else View.GONE
        infoContainer?.visibility = if (state.showHazard || state.showCamera) View.VISIBLE else View.GONE

        ivMuteStatus?.setImageResource(
            if (state.isAudioMuted) R.drawable.ic_notifications_off else R.drawable.ic_notifications
        )
        ivMuteStatus?.alpha = if (state.isAudioMuted) 0.5f else 1.0f

        innerContentView?.setBackgroundResource(
            if (state.isSpeeding && state.speedLimit != null && state.speedLimit > 0)
                R.drawable.overlay_bg_warning
            else
                R.drawable.overlay_bg
        )

        updateVisuals()
    }

    private fun updateSpeedLimitUI(limit: Int?) {
        tvSpeedLimit?.let { tv ->
            tv.alpha = 1.0f
            when (limit) {
                0 -> {
                    tv.text = ""
                    tv.setBackgroundResource(R.drawable.ic_unlimited)
                }
                -1 -> {
                    tv.text = ""
                    tv.setBackgroundResource(R.drawable.ic_variable)
                }
                null -> {
                    tv.text = "--"
                    tv.setBackgroundResource(R.drawable.speed_limit_bg_unknown)
                }
                else -> {
                    tv.text = limit.toString()
                    tv.setBackgroundResource(R.drawable.speed_limit_bg)
                }
            }
        }
    }

    fun flash() {
        innerContentView?.let {
            val anim = AlphaAnimation(0.3f, 1.0f)
            anim.duration = 200
            it.startAnimation(anim)
        }
    }

    private fun updateVisuals() {
        val view = innerContentView ?: return
        val wrapper = overlayWrapper ?: return
        val scale = settings.overlaySize

        // 1. Apply visual scale transformation
        view.scaleX = scale
        view.scaleY = scale
        view.alpha = settings.overlayAlpha
        tvCurrentSpeed?.setTextColor(settings.overlayTextColor)

        // 2. Measure natural size at scale 1.0
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val originalWidth = view.measuredWidth
        val originalHeight = view.measuredHeight

        // 3. Calculate scaled dimensions for the Window
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        // 4. Update the WindowManager window size (the wrapper)
        // We do NOT change innerContentView.layoutParams to scaled size,
        // because that would trigger a re-layout with more space than needed.
        params.width = scaledWidth
        params.height = scaledHeight

        try {
            windowManager.updateViewLayout(wrapper, params)
        } catch (e: Exception) {}
    }

    fun hide() {
        overlayWrapper?.let {
            windowManager.removeView(it)
            overlayWrapper = null
            innerContentView = null
        }
    }
}

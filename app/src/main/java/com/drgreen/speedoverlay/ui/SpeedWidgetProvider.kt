/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.logic.OsmParser

/**
 * AppWidgetProvider for displaying speed and speed limits on the home screen.
 */
class SpeedWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.drgreen.speedoverlay.ACTION_UPDATE_WIDGET"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_LIMIT = "extra_limit"
        const val EXTRA_UNIT = "extra_unit"
        const val EXTRA_IS_SPEEDING = "extra_is_speeding"
        const val EXTRA_CONFIDENCE = "extra_confidence"

        fun updateWidget(
            context: Context,
            speed: Int,
            limit: Int?,
            unit: String,
            isSpeeding: Boolean,
            isConfidenceHigh: Boolean = false
        ) {
            val intent = Intent(context, SpeedWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
                putExtra(EXTRA_SPEED, speed)
                putExtra(EXTRA_LIMIT, limit ?: -1)
                putExtra(EXTRA_UNIT, unit)
                putExtra(EXTRA_IS_SPEEDING, isSpeeding)
                putExtra(EXTRA_CONFIDENCE, isConfidenceHigh)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val speed = intent.getIntExtra(EXTRA_SPEED, 0)
            val limit = intent.getIntExtra(EXTRA_LIMIT, -1).let { if (it == -1) null else it }
            val unit = intent.getStringExtra(EXTRA_UNIT) ?: "km/h"
            val isSpeeding = intent.getBooleanExtra(EXTRA_IS_SPEEDING, false)
            val isConfidenceHigh = intent.getBooleanExtra(EXTRA_CONFIDENCE, false)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SpeedWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                val views = createRemoteViews(context, speed, limit, unit, isSpeeding, isConfidenceHigh)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
        }
    }

    private fun createRemoteViews(
        context: Context,
        speed: Int,
        limit: Int?,
        unit: String,
        isSpeeding: Boolean,
        isConfidenceHigh: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.speed_widget)

        views.setTextViewText(R.id.widget_current_speed, speed.toString())
        views.setTextViewText(R.id.widget_unit, unit)

        val speedColor = if (isSpeeding) 0xFFFF1744.toInt() else 0xFFFFFFFF.toInt()
        views.setTextColor(R.id.widget_current_speed, speedColor)

        updateLimitDisplay(views, limit, isConfidenceHigh)

        return views
    }

    private fun updateLimitDisplay(views: RemoteViews, limit: Int?, isConfidenceHigh: Boolean) {
        if (limit == null) {
            views.setViewVisibility(R.id.widget_limit_container, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_limit_container, View.VISIBLE)

        val backgroundRes = when {
            limit <= 0 -> R.drawable.speed_limit_circle_empty
            isConfidenceHigh -> R.drawable.speed_limit_circle_red
            else -> R.drawable.speed_limit_circle_gray
        }
        views.setImageViewResource(R.id.widget_limit_bg, backgroundRes)

        when (limit) {
            0 -> views.showIcon(R.drawable.ic_unlimited)
            -1 -> views.showIcon(R.drawable.ic_variable)
            OsmParser.URBAN_ICON_CODE -> views.showIcon(R.drawable.ic_urban)
            else -> views.showText(limit.toString())
        }
    }

    private fun RemoteViews.showIcon(resId: Int) {
        setViewVisibility(R.id.widget_speed_limit, View.GONE)
        setViewVisibility(R.id.widget_limit_icon, View.VISIBLE)
        setImageViewResource(R.id.widget_limit_icon, resId)
    }

    private fun RemoteViews.showText(text: String) {
        setViewVisibility(R.id.widget_limit_icon, View.GONE)
        setViewVisibility(R.id.widget_speed_limit, View.VISIBLE)
        setTextViewText(R.id.widget_speed_limit, text)
    }
}

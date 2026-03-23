/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drgreen.speedoverlay.R
import com.drgreen.speedoverlay.logic.OsmParser
import java.util.Locale

/**
 * The actual overlay view showing current speed and limit.
 *
 * @param state The current state containing speed, limit, and warnings.
 * @param scale The visual scale of the overlay.
 * @param alpha The transparency of the overlay.
 * @param textColor The color used for text elements.
 */
@Composable
fun OverlayScreen(
    state: OverlayState,
    scale: Float = 1f,
    alpha: Float = 1f,
    textColor: Color = Color.White
) {
    val limit = state.speedLimit
    val isWarning = state.isSpeeding && limit != null && limit > 0

    val infiniteTransition = rememberInfiniteTransition(label = "pulsate")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isWarning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isWarning) Color(0xDDFF1744) else Color(0xAA000000),
        animationSpec = tween(durationMillis = 500),
        label = "bgColor"
    )

    val borderBrush = Brush.linearGradient(
        colors = if (isWarning) {
            listOf(Color.White, Color.Red)
        } else {
            listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))
        }
    )

    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val pulseBuffer = 1.1f
                val scaledWidth = (placeable.width * scale * pulseBuffer).toInt()
                val scaledHeight = (placeable.height * scale * pulseBuffer).toInt()

                layout(scaledWidth, scaledHeight) {
                    placeable.placeRelativeWithLayer(
                        x = (scaledWidth - placeable.width) / 2,
                        y = (scaledHeight - placeable.height) / 2
                    ) {
                        this.scaleX = scale * pulseScale
                        this.scaleY = scale * pulseScale
                        this.alpha = alpha
                    }
                }
            }
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderBrush, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Speed Display ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.currentSpeed.toString(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    lineHeight = 34.sp
                )
                Text(
                    text = state.unit.uppercase(Locale.getDefault()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }

            // --- Sign Display ---
            if (limit != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    // Red border only for real speed limits (> 0)
                    if (limit > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 4.dp,
                                    color = if (state.isConfidenceHigh) Color.Red else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                    }

                    when (limit) {
                        0 -> Icon(
                            painter = painterResource(id = R.drawable.ic_unlimited),
                            contentDescription = stringResource(R.string.overlay_unlimited),
                            tint = Color.Unspecified,
                            modifier = Modifier.fillMaxSize()
                        )
                        -1 -> Icon(
                            painter = painterResource(id = R.drawable.ic_variable),
                            contentDescription = stringResource(R.string.overlay_variable),
                            tint = Color.Unspecified,
                            modifier = Modifier.fillMaxSize()
                        )
                        OsmParser.URBAN_ICON_CODE -> Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = stringResource(R.string.overlay_urban),
                            tint = Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        else -> Text(
                            text = limit.toString(),
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // --- Warning Icons ---
            if (state.showHazard || state.showCamera || state.isAudioMuted) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.showHazard) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = stringResource(R.string.overlay_hazard),
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (state.showCamera) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = stringResource(R.string.overlay_camera),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (state.isAudioMuted) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notifications_off),
                            contentDescription = stringResource(R.string.overlay_muted),
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

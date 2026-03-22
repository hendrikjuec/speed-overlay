/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drgreen.speedoverlay.R

@Composable
fun OverlayScreen(
    state: OverlayState,
    scale: Float = 1f,
    alpha: Float = 1f,
    textColor: Int = 0xFFFFFFFF.toInt()
) {
    val isWarning = state.isSpeeding && state.speedLimit != null && state.speedLimit > 0

    // Pulsating animation when speeding
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

    // Root container that reports its scaled size to the WindowManager
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                // Add a small buffer (1.1x) for the pulse animation to prevent clipping
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
            // Speed Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.currentSpeed.toString(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(textColor),
                    lineHeight = 34.sp
                )
                Text(
                    text = state.unit.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(textColor).copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }

            // Speed Limit Sign
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(if (state.speedLimit == null) Color(0xFFE0E0E0) else Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (state.speedLimit != null && state.speedLimit > 0) {
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

                when (state.speedLimit) {
                    0 -> Icon(
                        painter = painterResource(id = R.drawable.ic_unlimited),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                    -1 -> Icon(
                        painter = painterResource(id = R.drawable.ic_variable),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                    null -> Text(
                        text = "?",
                        color = Color.Gray,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    else -> Text(
                        text = state.speedLimit.toString(),
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Warnings and Mute status
            if (state.showHazard || state.showCamera || state.isAudioMuted) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.showHazard) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = "Hazard",
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (state.showCamera) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (state.isAudioMuted) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notifications_off),
                            contentDescription = "Muted",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun OverlayPreview() {
    OverlayScreen(
        state = OverlayState(
            currentSpeed = 105,
            speedLimit = 100,
            unit = "km/h",
            isSpeeding = true,
            isConfidenceHigh = true,
            showHazard = true,
            showCamera = true
        )
    )
}

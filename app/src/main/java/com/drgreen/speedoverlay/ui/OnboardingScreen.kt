/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onGrantLocation: () -> Unit,
    onGrantOverlay: () -> Unit,
    hasLocation: Boolean,
    hasOverlay: Boolean
) {
    var step by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            },
            label = "stepAnimation"
        ) { currentStep ->
            when (currentStep) {
                1 -> OnboardingStep(
                    title = "Welcome to Speed Overlay",
                    description = "Keep track of speed limits in real-time, right on top of your favorite navigation app.",
                    icon = Icons.Default.LocationOn,
                    buttonText = "Let's Start",
                    onNext = { step = 2 }
                )
                2 -> PermissionStep(
                    title = "Location Access",
                    description = "We need your GPS position to determine the current speed limit from OpenStreetMap.",
                    icon = Icons.Default.LocationOn,
                    isGranted = hasLocation,
                    onGrant = onGrantLocation,
                    onNext = { step = 3 }
                )
                3 -> PermissionStep(
                    title = "Overlay Permission",
                    description = "This allows us to show the speed limit while you are using other apps like Google Maps.",
                    icon = Icons.Default.Warning,
                    isGranted = hasOverlay,
                    onGrant = onGrantOverlay,
                    onNext = { onFinished() }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Step Indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(if (step == i + 1) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (step == i + 1) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
fun OnboardingStep(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(buttonText, fontSize = 18.sp)
        }
    }
}

@Composable
fun PermissionStep(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrant: () -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).align(Alignment.BottomEnd),
                    tint = Color(0xFF4CAF50)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))

        if (!isGranted) {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Grant Permission", fontSize = 18.sp)
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Continue", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

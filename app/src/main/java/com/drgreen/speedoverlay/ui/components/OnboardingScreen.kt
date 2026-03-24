/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drgreen.speedoverlay.R

/**
 * Onboarding screen that handles initial permission requests.
 * Shown when the app lacks location, overlay, or notification permissions.
 *
 * @param onFinished Callback when all permissions are granted and user clicks "Start App".
 * @param onGrantLocation Action to request location permission.
 * @param onGrantOverlay Action to request overlay permission.
 * @param onGrantNotification Action to request notification permission.
 * @param hasLocation Current status of location permission.
 * @param hasOverlay Current status of overlay permission.
 * @param hasNotification Current status of notification permission.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onGrantLocation: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantNotification: () -> Unit,
    hasLocation: Boolean,
    hasOverlay: Boolean,
    hasNotification: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.setup_required),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_desc),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        PermissionItem(
            title = stringResource(R.string.perm_location_title),
            isGranted = hasLocation,
            onClick = onGrantLocation,
            icon = Icons.Default.LocationOn
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = stringResource(R.string.perm_overlay_title),
            isGranted = hasOverlay,
            onClick = onGrantOverlay,
            icon = Icons.Default.Warning
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionItem(
            title = stringResource(R.string.perm_notification_title),
            isGranted = hasNotification,
            onClick = onGrantNotification,
            icon = Icons.Default.Notifications
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (hasLocation && hasOverlay && hasNotification) {
            Button(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.start_app_button))
            }
        }
    }
}

/**
 * Represents a single permission request item.
 * Changes appearance based on whether the permission is granted.
 */
@Composable
fun PermissionItem(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    Button(
        onClick = onClick,
        enabled = !isGranted,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isGranted) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = Color(0xFF4CAF50),
            disabledContentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Icon(Icons.Default.Check, contentDescription = null)
            } else {
                Text(
                    text = stringResource(R.string.perm_grant_button),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

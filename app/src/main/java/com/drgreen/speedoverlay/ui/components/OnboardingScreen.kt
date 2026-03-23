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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drgreen.speedoverlay.R

/**
 * Onboarding screen that handles initial permission requests.
 * Shown when the app lacks location or overlay permissions.
 *
 * @param onFinished Callback when all permissions are granted and user clicks "Start App".
 * @param onGrantLocation Action to request location permission.
 * @param onGrantOverlay Action to request overlay permission.
 * @param hasLocation Current status of location permission.
 * @param hasOverlay Current status of overlay permission.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onGrantLocation: () -> Unit,
    onGrantOverlay: () -> Unit,
    hasLocation: Boolean,
    hasOverlay: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
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

        Spacer(modifier = Modifier.height(48.dp))

        PermissionItem(
            title = stringResource(R.string.perm_location_title),
            isGranted = hasLocation,
            onClick = onGrantLocation,
            icon = Icons.Default.LocationOn
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionItem(
            title = stringResource(R.string.perm_overlay_title),
            isGranted = hasOverlay,
            onClick = onGrantOverlay,
            icon = Icons.Default.Warning
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (hasLocation && hasOverlay) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (isGranted) {
                Icon(Icons.Default.Check, contentDescription = null)
            } else {
                Text(stringResource(R.string.perm_grant_button), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

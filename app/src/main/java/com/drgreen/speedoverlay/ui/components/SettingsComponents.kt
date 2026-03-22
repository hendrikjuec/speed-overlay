/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drgreen.speedoverlay.R
import java.util.Locale

/**
 * Eine Karte zur Gruppierung von Einstellungen.
 */
@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Ein Standard-Switch für boolesche Einstellungen.
 */
@Composable
fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Ein Slider für numerische Einstellungen mit Formatierung.
 */
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    factor: Float = 1f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            val formattedValue = if (format.contains("%d")) {
                String.format(format, (value * factor).toInt())
            } else {
                String.format(Locale.getDefault(), format, value)
            }
            Text(
                text = formattedValue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value, valueRange = range, onValueChange = onValueChange)
    }
}

/**
 * Eine Dropdown-Auswahl für die App-Sprache.
 */
@Composable
fun SettingLanguage(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.language), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(when(currentLanguage) {
                    "de" -> stringResource(R.string.lang_de)
                    "es" -> stringResource(R.string.lang_es)
                    "fr" -> stringResource(R.string.lang_fr)
                    "it" -> stringResource(R.string.lang_it)
                    else -> stringResource(R.string.lang_en)
                })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("en", "de", "es", "fr", "it").forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(when(lang) {
                            "de" -> stringResource(R.string.lang_de)
                            "es" -> stringResource(R.string.lang_es)
                            "fr" -> stringResource(R.string.lang_fr)
                            "it" -> stringResource(R.string.lang_it)
                            else -> stringResource(R.string.lang_en)
                        }) },
                        onClick = {
                            onLanguageChange(lang)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Eine Dropdown-Auswahl für den Dark Mode.
 */
@Composable
fun SettingDarkMode(
    currentMode: Int,
    onModeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.dark_mode), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(when(currentMode) {
                    1 -> stringResource(R.string.dark_mode_off)
                    2 -> stringResource(R.string.dark_mode_on)
                    else -> stringResource(R.string.dark_mode_auto)
                })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(0, 1, 2).forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(when(mode) {
                            1 -> stringResource(R.string.dark_mode_off)
                            2 -> stringResource(R.string.dark_mode_on)
                            else -> stringResource(R.string.dark_mode_auto)
                        }) },
                        onClick = {
                            onModeChange(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Auswahl für das Bluetooth-Gerät für den Autostart.
 */
@SuppressLint("MissingPermission")
@Composable
fun SettingBluetoothDevice(
    currentAddress: String?,
    onAddressChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter

    val pairedDevices = remember {
        try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val currentDeviceName = pairedDevices.find { it.address == currentAddress }?.name
        ?: currentAddress
        ?: stringResource(R.string.no_bt_device_selected)

    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.select_bt_device), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(currentDeviceName)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.no_bt_device_selected)) },
                    onClick = {
                        onAddressChange(null)
                        expanded = false
                    }
                )
                pairedDevices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.name ?: device.address) },
                        onClick = {
                            onAddressChange(device.address)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

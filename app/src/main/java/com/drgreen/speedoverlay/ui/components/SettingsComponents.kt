/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drgreen.speedoverlay.R
import java.util.Locale

/**
 * A card for grouping settings in a consistent look.
 *
 * @param title The title of the settings group (displayed in uppercase).
 * @param content The compose content of the card.
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
                text = title.uppercase(Locale.getDefault()),
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
 * A row with a label and a switch for boolean settings.
 *
 * @param label The text displayed to the left of the switch.
 * @param checked The current state of the switch.
 * @param onCheckedChange Callback when the switch is toggled.
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
 * A component for numerical settings with a slider and value display.
 *
 * @param label The label of the setting.
 * @param value The current value.
 * @param range The allowed value range.
 * @param format The string formatter for the value display (e.g., "%.1f km/h").
 * @param factor An optional factor for display (e.g., 100 for percent).
 * @param onValueChange Callback on value change.
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
                String.format(Locale.getDefault(), format, (value * factor).toInt())
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
 * A generic dropdown selection for settings.
 *
 * @param T The type of selectable items.
 * @param label The label of the setting.
 * @param currentValue The currently selected item.
 * @param items The list of available options.
 * @param itemToText A function that translates an item into a displayable string.
 * @param onValueChange Callback upon selection of a new item.
 */
@Composable
private fun <T> SettingDropdown(
    label: String,
    currentValue: T,
    items: List<T>,
    itemToText: @Composable (T) -> String,
    onValueChange: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(itemToText(currentValue))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemToText(item)) },
                        onClick = {
                            onValueChange(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Specific selection for the application language.
 *
 * @param currentLanguage The current language code (e.g., "de").
 * @param onLanguageChange Callback on language change.
 */
@Composable
fun SettingLanguage(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val languages = listOf("en", "de", "es", "fr", "it")
    SettingDropdown(
        label = stringResource(R.string.language),
        currentValue = currentLanguage,
        items = languages,
        itemToText = { lang ->
            when (lang) {
                "de" -> stringResource(R.string.lang_de)
                "es" -> stringResource(R.string.lang_es)
                "fr" -> stringResource(R.string.lang_fr)
                "it" -> stringResource(R.string.lang_it)
                else -> stringResource(R.string.lang_en)
            }
        },
        onValueChange = onLanguageChange
    )
}

/**
 * Specific selection for the appearance (Light/Dark theme).
 *
 * @param currentMode The current mode (0=System, 1=Light, 2=Dark).
 * @param onModeChange Callback on mode change.
 */
@Composable
fun SettingDarkMode(
    currentMode: Int,
    onModeChange: (Int) -> Unit
) {
    SettingDropdown(
        label = stringResource(R.string.dark_mode),
        currentValue = currentMode,
        items = listOf(0, 1, 2),
        itemToText = { mode ->
            when (mode) {
                1 -> stringResource(R.string.dark_mode_off)
                2 -> stringResource(R.string.dark_mode_on)
                else -> stringResource(R.string.dark_mode_auto)
            }
        },
        onValueChange = onModeChange
    )
}

/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drgreen.speedoverlay.data.LogEntry
import com.drgreen.speedoverlay.data.LogManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ein Dialog zur Anzeige und Verwaltung der aufgezeichneten Geschwindigkeitsüberschreitungen.
 */
@Composable
fun LogbookDialog(logManager: LogManager, onDismiss: () -> Unit) {
    val logs by logManager.getAllLogs().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📓 Automatisches Logbuch") },
        text = {
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Noch keine Einträge vorhanden.", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(logs) { log ->
                        LogEntryItem(log)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
        dismissButton = {
            IconButton(onClick = { scope.launch { logManager.clearLogs() } }) {
                Icon(Icons.Default.Delete, contentDescription = "Alles löschen", tint = Color.Red)
            }
        }
    )
}

@Composable
fun LogEntryItem(log: LogEntry) {
    val df = remember { SimpleDateFormat("dd.MM.HH:mm", Locale.getDefault()) }
    Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(df.format(Date(log.startTime)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${log.maxSpeed} / ${log.speedLimit} ${log.unit}", color = Color.Red, fontWeight = FontWeight.Bold)
        }
        Text(
            "Ø ${log.avgSpeed} ${log.unit} | Dauer: ${(log.endTime - log.startTime) / 1000}s",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

package com.murmli.webpursuer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murmli.webpursuer.data.Monitor
import com.murmli.webpursuer.data.MonitorDao
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ReportSettingsDialog(
        monitorDao: MonitorDao,
        initialSelection: Set<String>,
        onDismiss: () -> Unit,
        onConfirm: (Set<String>) -> Unit
) {
    var monitors by remember { mutableStateOf<List<Monitor>>(emptyList()) }
    // local mutable selection
    val selectedIds = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }

    LaunchedEffect(Unit) { monitorDao.getAll().collectLatest { monitors = it } }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Websites for Report") },
            text = {
                Column(
                        modifier =
                                Modifier.fillMaxSize() // Fill available space in dialog
                                        .padding(8.dp)
                ) {
                    Text(
                            "Include updates from:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Divider()

                    LazyColumn(
                            modifier = Modifier.weight(1f) // Take remaining space
                    ) {
                        items(monitors) { monitor ->
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                        checked = selectedIds.contains(monitor.id.toString()),
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                selectedIds.add(monitor.id.toString())
                                            } else {
                                                selectedIds.remove(monitor.id.toString())
                                            }
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                            text = monitor.name,
                                            style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                            text = monitor.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                    )
                                }
                            }
                            Divider(
                                    color =
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.5f
                                            )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onConfirm(selectedIds.toSet()) }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

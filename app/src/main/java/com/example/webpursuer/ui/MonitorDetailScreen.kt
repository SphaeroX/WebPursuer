package com.example.webpursuer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.webpursuer.data.Monitor
import com.example.webpursuer.data.CheckLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDetailScreen(
    monitor: Monitor,
    viewModel: MonitorViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val logs by viewModel.getLogsForMonitor(monitor.id).collectAsState(initial = emptyList())
    var currentInterval by remember { mutableLongStateOf(monitor.checkIntervalMinutes) }
    
    // Dropdown state
    var expanded by remember { mutableStateOf(false) }
    val intervals = listOf(10L to "10 Minutes", 60L to "1 Hour", 1440L to "24 Hours")

    // Log Detail Dialog
    var selectedLog by remember { mutableStateOf<CheckLog?>(null) }

    if (selectedLog != null) {
        AlertDialog(
            onDismissRequest = { selectedLog = null },
            title = { Text("Check Details") },
            text = {
                Column {
                    Text("Result: ${selectedLog!!.result}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Message: ${selectedLog!!.message}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Content Found:", style = MaterialTheme.typography.labelLarge)
                    val scrollState = rememberScrollState()
                    Text(
                        text = selectedLog!!.content ?: "No content stored",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .heightIn(max = 200.dp) // Limit height
                            .verticalScroll(scrollState)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLog = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(monitor.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.checkNow(monitor) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Check Now")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Info Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("URL: ${monitor.url}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = monitor.selector,
                        onValueChange = { 
                            viewModel.updateMonitor(monitor.copy(selector = it)) 
                        },
                        label = { Text("CSS Selector") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Interval Selection
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Check Interval: ")
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(intervals.find { it.first == currentInterval }?.second ?: "$currentInterval min")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                intervals.forEach { (minutes, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            currentInterval = minutes
                                            expanded = false
                                            viewModel.updateMonitor(monitor.copy(checkIntervalMinutes = minutes))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Notification Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Notifications", modifier = Modifier.weight(1f))
                        Switch(
                            checked = monitor.notificationsEnabled,
                            onCheckedChange = {
                                viewModel.updateMonitor(monitor.copy(notificationsEnabled = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // LLM Configuration
                    Text("Intelligent Monitoring (LLM)", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable LLM Check", modifier = Modifier.weight(1f))
                        Switch(
                            checked = monitor.llmEnabled,
                            onCheckedChange = { 
                                viewModel.updateMonitor(monitor.copy(llmEnabled = it)) 
                            }
                        )
                    }
                    
                    if (monitor.llmEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = monitor.llmPrompt ?: "",
                            onValueChange = { 
                                viewModel.updateMonitor(monitor.copy(llmPrompt = it)) 
                            },
                            label = { Text("Condition Prompt") },
                            placeholder = { Text("e.g., Notify if price is below $50") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Check History", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Logs List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogItem(log, onClick = { selectedLog = log })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogItem(log: CheckLog, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall)
                Text(log.message, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = log.result,
                color = if (log.result == "CHANGED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

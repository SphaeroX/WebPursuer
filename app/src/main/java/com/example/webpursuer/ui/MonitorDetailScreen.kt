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
                    
                    // Schedule Settings
                    Text("Schedule Type", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = monitor.scheduleType == "INTERVAL",
                            onClick = { viewModel.updateMonitor(monitor.copy(scheduleType = "INTERVAL")) }
                        )
                        Text("Interval")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = monitor.scheduleType == "DAILY",
                            onClick = { viewModel.updateMonitor(monitor.copy(scheduleType = "DAILY")) }
                        )
                        Text("Daily")
                    }

                    if (monitor.scheduleType == "INTERVAL") {
                        var intervalText by remember(monitor.checkIntervalMinutes) { 
                            if (monitor.checkIntervalMinutes == 0L) mutableStateOf("")
                            else if (monitor.checkIntervalMinutes % 60 == 0L) mutableStateOf("${monitor.checkIntervalMinutes / 60}h")
                            else mutableStateOf("${monitor.checkIntervalMinutes}m")
                        }
                        
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { input ->
                                intervalText = input
                                // Parse input like "1h", "15m", "1h 30m"
                                var totalMinutes = 0L
                                val hourMatch = Regex("(\\d+)\\s*h").find(input)
                                val minMatch = Regex("(\\d+)\\s*m").find(input)
                                
                                if (hourMatch != null) {
                                    totalMinutes += (hourMatch.groupValues[1].toLongOrNull() ?: 0) * 60
                                }
                                if (minMatch != null) {
                                    totalMinutes += (minMatch.groupValues[1].toLongOrNull() ?: 0)
                                }
                                
                                // Fallback for just numbers
                                if (totalMinutes == 0L && input.all { it.isDigit() }) {
                                    totalMinutes = input.toLongOrNull() ?: 0L
                                }

                                if (totalMinutes > 0) {
                                     viewModel.updateMonitor(monitor.copy(checkIntervalMinutes = totalMinutes))
                                }
                            },
                            label = { Text("Interval (e.g. 15m, 1h)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("Android minimum is 15 minutes") }
                        )
                    } else {
                        // Daily Time Selection
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val calendar = Calendar.getInstance()
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = calendar.get(Calendar.MINUTE)

                        var timeText by remember(monitor.checkTime) { mutableStateOf(monitor.checkTime ?: String.format("%02d:%02d", currentHour, currentMinute)) }

                        val timePickerDialog = android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                                timeText = selectedTime
                                viewModel.updateMonitor(monitor.copy(checkTime = selectedTime))
                            },
                            currentHour,
                            currentMinute,
                            true // 24 hour format
                        )

                        OutlinedTextField(
                            value = timeText,
                            onValueChange = {}, // Read only, click sets it
                            label = { Text("Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                .also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect {
                                            if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                                timePickerDialog.show()
                                            }
                                        }
                                    }
                                }
                        )
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

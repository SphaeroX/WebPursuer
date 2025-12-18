package com.example.webpursuer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.webpursuer.data.CheckLog
import com.example.webpursuer.data.Monitor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDetailScreen(
        monitor: Monitor,
        viewModel: MonitorViewModel = viewModel(),
        onBackClick: () -> Unit,
        onLogClick: (CheckLog) -> Unit
) {
    val logs by viewModel.getLogsForMonitor(monitor.id).collectAsState(initial = emptyList())
    // Local state for immediate input feedback to avoid cursor jumping
    // We use remember(monitor.id) so it resets if we switch monitors somehow,
    // but not strictly necessary if screen is recreated.
    var selectorInput by remember(monitor.id) { mutableStateOf(monitor.selector) }
    var aiInstructionInput by
            remember(monitor.id) { mutableStateOf(monitor.aiInterpreterInstruction) }
    var llmPromptInput by remember(monitor.id) { mutableStateOf(monitor.llmPrompt ?: "") }

    // Dropdown state
    var expanded by remember { mutableStateOf(false) }
    val intervals = listOf(10L to "10 Minutes", 60L to "1 Hour", 1440L to "24 Hours")

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
        // Use LazyColumn for the WHOLE screen content to ensure scrolling works
        // even with keyboard open and lists at the bottom.
        LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ITEM 1: General Info Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("URL: ${monitor.url}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Selector Input with local state
                        OutlinedTextField(
                                value = selectorInput,
                                onValueChange = {
                                    selectorInput = it
                                    viewModel.updateMonitor(monitor.copy(selector = it))
                                },
                                label = { Text("CSS Selector") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    IconButton(
                                            onClick = {
                                                val intent =
                                                        android.content.Intent(
                                                                        context,
                                                                        BrowserActivity::class.java
                                                                )
                                                                .apply {
                                                                    putExtra(
                                                                            "monitorId",
                                                                            monitor.id
                                                                    )
                                                                    putExtra("url", monitor.url)
                                                                }
                                                context.startActivity(intent)
                                            }
                                    ) {
                                        Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit in Browser"
                                        )
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Schedule Settings
                        Text("Schedule Type", style = MaterialTheme.typography.titleSmall)
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                    selected = monitor.scheduleType == "INTERVAL",
                                    onClick = {
                                        viewModel.updateMonitor(
                                                monitor.copy(scheduleType = "INTERVAL")
                                        )
                                    }
                            )
                            Text("Interval")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                    selected = monitor.scheduleType == "DAILY",
                                    onClick = {
                                        val newCheckTime =
                                                monitor.checkTime
                                                        ?: run {
                                                            val cal = Calendar.getInstance()
                                                            String.format(
                                                                    Locale.getDefault(),
                                                                    "%02d:%02d",
                                                                    cal.get(Calendar.HOUR_OF_DAY),
                                                                    cal.get(Calendar.MINUTE)
                                                            )
                                                        }
                                        viewModel.updateMonitor(
                                                monitor.copy(
                                                        scheduleType = "DAILY",
                                                        checkTime = newCheckTime
                                                )
                                        )
                                    }
                            )
                            Text("Daily")
                        }

                        if (monitor.scheduleType == "INTERVAL") {
                            // Local state for interval text to allow typing like "30" without
                            // jumping
                            var intervalText by
                                    remember(monitor.checkIntervalMinutes) {
                                        if (monitor.checkIntervalMinutes == 0L) mutableStateOf("")
                                        else if (monitor.checkIntervalMinutes % 60 == 0L)
                                                mutableStateOf(
                                                        "${monitor.checkIntervalMinutes / 60}h"
                                                )
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
                                            totalMinutes +=
                                                    (hourMatch.groupValues[1].toLongOrNull()
                                                            ?: 0) * 60
                                        }
                                        if (minMatch != null) {
                                            totalMinutes +=
                                                    (minMatch.groupValues[1].toLongOrNull() ?: 0)
                                        }

                                        // Fallback for just numbers
                                        if (totalMinutes == 0L && input.all { it.isDigit() }) {
                                            totalMinutes = input.toLongOrNull() ?: 0L
                                        }

                                        if (totalMinutes > 0) {
                                            viewModel.updateMonitor(
                                                    monitor.copy(
                                                            checkIntervalMinutes = totalMinutes
                                                    )
                                            )
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

                            var timeText by
                                    remember(monitor.checkTime) {
                                        mutableStateOf(
                                                monitor.checkTime
                                                        ?: String.format(
                                                                "%02d:%02d",
                                                                currentHour,
                                                                currentMinute
                                                        )
                                        )
                                    }

                            val timePickerDialog =
                                    android.app.TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val selectedTime =
                                                        String.format(
                                                                "%02d:%02d",
                                                                hourOfDay,
                                                                minute
                                                        )
                                                timeText = selectedTime
                                                viewModel.updateMonitor(
                                                        monitor.copy(checkTime = selectedTime)
                                                )
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
                                    interactionSource =
                                            remember {
                                                androidx.compose.foundation.interaction
                                                        .MutableInteractionSource()
                                            }
                                                    .also { interactionSource ->
                                                        LaunchedEffect(interactionSource) {
                                                            interactionSource.interactions.collect {
                                                                if (it is
                                                                                androidx.compose.foundation.interaction.PressInteraction.Release
                                                                ) {
                                                                    timePickerDialog.show()
                                                                }
                                                            }
                                                        }
                                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
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
                                        viewModel.updateMonitor(
                                                monitor.copy(notificationsEnabled = it)
                                        )
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // AI Data Interpretation
                        Text("AI Data Interpretation", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Interpret Data with AI", modifier = Modifier.weight(1f))
                            Switch(
                                    checked = monitor.useAiInterpreter,
                                    onCheckedChange = {
                                        viewModel.updateMonitor(monitor.copy(useAiInterpreter = it))
                                    }
                            )
                        }

                        if (monitor.useAiInterpreter) {
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                    value = aiInstructionInput,
                                    onValueChange = {
                                        aiInstructionInput = it
                                        viewModel.updateMonitor(
                                                monitor.copy(aiInterpreterInstruction = it)
                                        )
                                    },
                                    label = { Text("Interpretation Instruction") },
                                    placeholder = {
                                        Text("e.g. Extract the price, Summarize the table")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // LLM Configuration
                        Text(
                                "Intelligent Monitoring (LLM Condition)",
                                style = MaterialTheme.typography.titleSmall
                        )
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
                                    value = llmPromptInput,
                                    onValueChange = {
                                        llmPromptInput = it
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
            }

            // ITEM 2: Section Header
            item { Text("Check History", style = MaterialTheme.typography.titleMedium) }

            // ITEM 3: Logs List (directly as items in parent LazyColumn)
            items(logs) { log -> LogItem(log, onClick = { onLogClick(log) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogItem(log: CheckLog, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        dateFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall
                )
                Text(log.message, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                    text = log.result,
                    color =
                            if (log.result == "CHANGED") MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

package com.example.webpursuer.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.webpursuer.data.Report
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportEditScreen(
        report: Report? = null,
        reportViewModel: ReportViewModel,
        monitorViewModel: MonitorViewModel,
        onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf(report?.name ?: "") }
    var userPrompt by remember { mutableStateOf(report?.customPrompt ?: "") }

    // Scheduling State
    var scheduleType by remember { mutableStateOf(report?.scheduleType ?: "SPECIFIC_TIME") }
    var selectedHour by remember {
        mutableIntStateOf(report?.scheduleHour ?: 8)
    } // Start time or Daily time
    var selectedMinute by remember { mutableIntStateOf(report?.scheduleMinute ?: 0) }
    var scheduleDays by remember {
        mutableIntStateOf(report?.scheduleDays ?: 127)
    } // Default all days
    var intervalHours by remember { mutableStateOf((report?.intervalHours ?: 24).toString()) }

    // Monitors selection
    val allMonitors by monitorViewModel.monitors.collectAsState()
    val initialSelection =
            report?.monitorIds?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                    ?: emptySet()

    var selectedMonitorIds by remember { mutableStateOf(initialSelection) }

    val context = LocalContext.current
    val timePickerDialog =
            TimePickerDialog(
                    context,
                    { _, hour: Int, minute: Int ->
                        selectedHour = hour
                        selectedMinute = minute
                    },
                    selectedHour,
                    selectedMinute,
                    true
            )

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(if (report == null) "New Report" else "Edit Report") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { innerPadding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Report Name") },
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    label = { Text("LLM Instructions (Prompt)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("e.g., Summarize the changes as a bulleted list.") }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Schedule Type", style = MaterialTheme.typography.titleMedium)

            // Schedule Type Dropdown
            val options =
                    listOf(
                            "Specific Time (Weekly/Daily)" to "SPECIFIC_TIME",
                            "Interval" to "INTERVAL"
                    )
            val selectedOptionText =
                    options.find { it.second == scheduleType }?.first ?: options[0].first
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedOptionText,
                        onValueChange = {},
                        label = { Text("Schedule Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                ) {
                    options.forEach { selectionOption ->
                        DropdownMenuItem(
                                text = { Text(selectionOption.first) },
                                onClick = {
                                    scheduleType = selectionOption.second
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (scheduleType == "SPECIFIC_TIME") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text =
                                    "Time: ${String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)}",
                            style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { timePickerDialog.show() }) { Text("Set Time") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Repeat on:", style = MaterialTheme.typography.bodyMedium)
                // Day Selection
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                // Bitmask: 0=Mon, ... 6=Sun
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { index, dayName ->
                        val mask = 1 shl index
                        val isSelected = (scheduleDays and mask) != 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        scheduleDays =
                                                if (checked) {
                                                    scheduleDays or mask
                                                } else {
                                                    scheduleDays and mask.inv()
                                                }
                                    }
                            )
                            Text(dayName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                // Interval
                OutlinedTextField(
                        value = intervalHours,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) intervalHours = it
                        },
                        label = { Text("Interval (Hours)") },
                        keyboardOptions =
                                androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                ),
                        modifier = Modifier.fillMaxWidth()
                )
                Text(
                        "Start first run at:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text =
                                    "${String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)}",
                            style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { timePickerDialog.show() }) { Text("Set Start Time") }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Web Search Toggle
            var useWebSearch by remember { mutableStateOf(report?.useWebSearch ?: false) }
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { useWebSearch = !useWebSearch }
                                    .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Switch(
                        checked = useWebSearch,
                        onCheckedChange = { useWebSearch = it }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Enable Web Search", style = MaterialTheme.typography.bodyLarge)
                    Text(
                            "Allow AI to search the web during report generation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Select Websites to Monitor:", style = MaterialTheme.typography.titleMedium)

            Column {
                allMonitors.forEach { monitor ->
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickable {
                                                val current = selectedMonitorIds.toMutableSet()
                                                if (current.contains(monitor.id)) {
                                                    current.remove(monitor.id)
                                                } else {
                                                    current.add(monitor.id)
                                                }
                                                selectedMonitorIds = current
                                            }
                                            .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                                checked = selectedMonitorIds.contains(monitor.id),
                                onCheckedChange = null // Handled by Row click
                        )
                        Text(text = monitor.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                    onClick = {
                        val intHours = intervalHours.toIntOrNull() ?: 24

                        if (report == null) {
                            reportViewModel.addReport(
                                    name = name,
                                    customPrompt = userPrompt,
                                    scheduleType = scheduleType,
                                    scheduleHour = selectedHour,
                                    scheduleMinute = selectedMinute,
                                    scheduleDays = scheduleDays,
                                    intervalHours = intHours,
                                    monitorIds = selectedMonitorIds,
                                    useWebSearch = useWebSearch
                            )
                        } else {
                            val updatedReport =
                                    report.copy(
                                            name = name,
                                            customPrompt = userPrompt,
                                            scheduleType = scheduleType,
                                            scheduleHour = selectedHour,
                                            scheduleMinute = selectedMinute,
                                            scheduleDays = scheduleDays,
                                            intervalHours = intHours,
                                            monitorIds = selectedMonitorIds.joinToString(","),
                                            useWebSearch = useWebSearch
                                    )
                            reportViewModel.updateReportFull(updatedReport)
                        }
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && (scheduleDays != 0 || scheduleType == "INTERVAL")
            ) { Text("Save Report") }
        }
    }
}

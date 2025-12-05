package com.example.webpursuer.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
    var selectedHour by remember { mutableIntStateOf(report?.scheduleHour ?: 8) }
    var selectedMinute by remember { mutableIntStateOf(report?.scheduleMinute ?: 0) }

    // Monitors selection
    val allMonitors by monitorViewModel.monitors.collectAsState()
    val initialSelection = report?.monitorIds?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
    
    var selectedMonitorIds by remember { mutableStateOf(initialSelection) }

    val context = LocalContext.current
    val timePickerDialog = TimePickerDialog(
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Message Time: ${String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { timePickerDialog.show() }) {
                    Text("Set Time")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Select Websites to Monitor:", style = MaterialTheme.typography.titleMedium)
            
            Column {
                allMonitors.forEach { monitor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    if (report == null) {
                        reportViewModel.addReport(name, userPrompt, selectedHour, selectedMinute, selectedMonitorIds)
                    } else {
                        // We need to pass the updated object but we only have updateReport(report, ids) helper in VM?
                        // Actually the VM helper updateReport takes report and ids.
                        // But wait, the report object passed to VM update needs to be the updated one for name/prompt/time.
                        val updatedReport = report.copy(
                            name = name,
                            customPrompt = userPrompt,
                            scheduleHour = selectedHour,
                            scheduleMinute = selectedMinute
                        )
                        reportViewModel.updateReport(updatedReport, selectedMonitorIds)
                    }
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Save Report")
            }
        }
    }
}

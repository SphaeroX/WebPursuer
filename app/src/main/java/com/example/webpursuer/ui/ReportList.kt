package com.example.webpursuer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.webpursuer.data.Report
import java.util.Locale

@Composable
fun ReportList(
    viewModel: ReportViewModel,
    onEditClick: (Report) -> Unit,
    innerPadding: PaddingValues
) {
    val reports by viewModel.reports.collectAsState()

    if (reports.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No reports yet. Add one!")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { report ->
                ReportItem(
                    report = report,
                    onEditClick = { onEditClick(report) },
                    onDeleteClick = { viewModel.deleteReport(report) },
                    onToggle = { enabled -> viewModel.toggleReport(report, enabled) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportItem(
    report: Report,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = report.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Scheduled: ${String.format(Locale.getDefault(), "%02d:%02d", report.scheduleHour, report.scheduleMinute)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (report.customPrompt.isNotBlank()) {
                     Text(
                        text = "Prompt: ${report.customPrompt.take(30)}...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = report.enabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

package com.murmli.webpursuer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmli.webpursuer.data.Monitor
import com.murmli.webpursuer.data.Report
import com.murmli.webpursuer.data.Search
import com.murmli.webpursuer.data.SettingsRepository
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerOverviewScreen(
    onBackClick: () -> Unit,
    monitors: List<Monitor>,
    reports: List<Report>,
    searches: List<Search>,
    settingsRepository: SettingsRepository,
    onEditMonitor: (Int) -> Unit,
    onEditReport: (Report) -> Unit,
    onEditSearch: (Search) -> Unit
) {
    val workerQuietEnabled by settingsRepository.workerQuietEnabled.collectAsState(initial = false)
    val workerQuietStart by settingsRepository.workerQuietStartHour.collectAsState(initial = 22)
    val workerQuietEnd by settingsRepository.workerQuietEndHour.collectAsState(initial = 6)

    val notificationQuietEnabled by settingsRepository.notificationQuietEnabled.collectAsState(initial = false)
    val notificationQuietStart by settingsRepository.notificationQuietStartHour.collectAsState(initial = 22)
    val notificationQuietEnd by settingsRepository.notificationQuietEndHour.collectAsState(initial = 6)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker & Quiet Times") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Quiet Times", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // Worker Quiet Time
            item {
                QuietTimeCard(
                    title = "Pause all Workers",
                    enabled = workerQuietEnabled,
                    startHour = workerQuietStart,
                    endHour = workerQuietEnd,
                    onEnabledChange = {
                        kotlinx.coroutines.launch { settingsRepository.saveWorkerQuietEnabled(it) }
                    },
                    onStartTimeClick = {
                        android.app.TimePickerDialog(context, { _, h, _ ->
                            kotlinx.coroutines.launch { settingsRepository.saveWorkerQuietStartHour(h) }
                        }, workerQuietStart, 0, true).show()
                    },
                    onEndTimeClick = {
                        android.app.TimePickerDialog(context, { _, h, _ ->
                            kotlinx.coroutines.launch { settingsRepository.saveWorkerQuietEndHour(h) }
                        }, workerQuietEnd, 0, true).show()
                    }
                )
            }

            // Notification Quiet Time
            item {
                QuietTimeCard(
                    title = "Mute Notifications",
                    enabled = notificationQuietEnabled,
                    startHour = notificationQuietStart,
                    endHour = notificationQuietEnd,
                    onEnabledChange = {
                        kotlinx.coroutines.launch { settingsRepository.saveNotificationQuietEnabled(it) }
                    },
                    onStartTimeClick = {
                        android.app.TimePickerDialog(context, { _, h, _ ->
                            kotlinx.coroutines.launch { settingsRepository.saveNotificationQuietStartHour(h) }
                        }, notificationQuietStart, 0, true).show()
                    },
                    onEndTimeClick = {
                        android.app.TimePickerDialog(context, { _, h, _ ->
                            kotlinx.coroutines.launch { settingsRepository.saveNotificationQuietEndHour(h) }
                        }, notificationQuietEnd, 0, true).show()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Scheduled Jobs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // Job List
            val allJobs = mutableListOf<JobItemData>()
            allJobs.addAll(monitors.map { JobItemData.MonitorJob(it) })
            allJobs.addAll(reports.map { JobItemData.ReportJob(it) })
            allJobs.addAll(searches.map { JobItemData.SearchJob(it) })
            
            // Sort by name/title
            allJobs.sortBy { it.getTitle().lowercase() }

            if (allJobs.isEmpty()) {
                item {
                    Text("No scheduled jobs found.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                items(allJobs) { job ->
                    JobListItem(
                        job = job,
                        onClick = {
                            when (job) {
                                is JobItemData.MonitorJob -> onEditMonitor(job.monitor.id)
                                is JobItemData.ReportJob -> onEditReport(job.report)
                                is JobItemData.SearchJob -> onEditSearch(job.search)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuietTimeCard(
    title: String,
    enabled: Boolean,
    startHour: Int,
    endHour: Int,
    onEnabledChange: (Boolean) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("From:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${String.format("%02d:00", startHour)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable { onStartTimeClick() }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("To:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${String.format("%02d:00", endHour)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable { onEndTimeClick() }
                        )
                    }
                }
            }
        }
    }
}

sealed class JobItemData {
    abstract fun getTitle(): String
    abstract fun getSubtitle(): String
    abstract fun getIcon(): androidx.compose.ui.graphics.vector.ImageVector
    abstract fun isEnabled(): Boolean

    data class MonitorJob(val monitor: Monitor) : JobItemData() {
        override fun getTitle() = monitor.name
        override fun getSubtitle() = "Monitor • ${monitor.scheduleType} • ${monitor.url}"
        override fun getIcon() = Icons.Default.Language
        override fun isEnabled() = monitor.enabled
    }

    data class ReportJob(val report: Report) : JobItemData() {
        override fun getTitle() = report.title
        override fun getSubtitle() = "Report • Daily at ${String.format("%02d:00", report.targetHour)}"
        override fun getIcon() = Icons.Default.Description
        override fun isEnabled() = report.enabled
    }

    data class SearchJob(val search: Search) : JobItemData() {
        override fun getTitle() = if (search.title.isBlank()) search.prompt else search.title
        override fun getSubtitle() = "Search • Interval ${search.checkIntervalMinutes}m"
        override fun getIcon() = Icons.Default.Search
        override fun isEnabled() = search.enabled
    }
}

@Composable
fun JobListItem(job: JobItemData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (job.isEnabled()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                job.getIcon(),
                contentDescription = null,
                tint = if (job.isEnabled()) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.getTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (job.isEnabled()) Color.Unspecified else Color.Gray
                )
                Text(
                    text = job.getSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            if (!job.isEnabled()) {
                Text("OFF", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

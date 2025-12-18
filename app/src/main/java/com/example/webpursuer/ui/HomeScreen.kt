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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.webpursuer.data.Monitor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        monitorViewModel: MonitorViewModel = viewModel(),
        reportViewModel: ReportViewModel = viewModel(),
        generatedReportRepository: com.example.webpursuer.data.GeneratedReportRepository,
        initialDiffLogId: Int? = null,
        initialMonitorId: Int? = null,
        initialGeneratedReportId: Int? = null,
        onAddMonitorClick: () -> Unit
) {
    val monitors by monitorViewModel.monitors.collectAsState()
    var selectedMonitorId by remember { mutableStateOf<Int?>(null) }

    val selectedMonitor = monitors.find { it.id == selectedMonitorId }

    // Navigation States
    var showSettings by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) } // New state
    var showReportEdit by remember { mutableStateOf(false) }
    var selectedReportForEdit by remember {
        mutableStateOf<com.example.webpursuer.data.Report?>(null)
    }

    var showReportHistory by remember { mutableStateOf<Int?>(null) } // Report ID
    var showReportContent by remember {
        mutableStateOf(initialGeneratedReportId)
    } // GeneratedReport ID

    // Diff Screen State
    var diffLogId by remember { mutableStateOf(initialDiffLogId) }
    var diffMonitorId by remember { mutableStateOf(initialMonitorId) }

    // Tab State
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Monitors, 1 = Reports

    // FAB Dialog State
    var showAddDialog by remember { mutableStateOf(false) }

    if (diffLogId != null && diffMonitorId != null) {
        DiffScreen(
                checkLogId = diffLogId!!,
                monitorId = diffMonitorId!!,
                viewModel = monitorViewModel,
                onBackClick = {
                    diffLogId = null
                    diffMonitorId = null
                }
        )
    } else if (showLogs) {
        LogScreen(onBackClick = { showLogs = false })
    } else if (showSettings) {
        SettingsScreen(
                onBackClick = { showSettings = false },
                onLogsClick = {
                    showSettings = false
                    showLogs = true
                }
        )
    } else if (showReportContent != null) {
        ReportContentScreen(
                generatedReportId = showReportContent!!,
                repository = generatedReportRepository,
                onNavigateBack = { showReportContent = null }
        )
    } else if (showReportHistory != null) {
        ReportHistoryScreen(
                reportId = showReportHistory!!,
                repository = generatedReportRepository,
                onNavigateBack = { showReportHistory = null },
                onViewReport = { generatedId -> showReportContent = generatedId }
        )
    } else if (showReportEdit) {
        ReportEditScreen(
                report = selectedReportForEdit,
                reportViewModel = reportViewModel,
                monitorViewModel = monitorViewModel,
                onBackClick = {
                    showReportEdit = false
                    selectedReportForEdit = null
                }
        )
    } else if (selectedMonitor != null) {
        MonitorDetailScreen(
                monitor = selectedMonitor!!,
                viewModel = monitorViewModel,
                onBackClick = { selectedMonitorId = null },
                onLogClick = { log ->
                    diffLogId = log.id
                    diffMonitorId = log.monitorId
                }
        )
    } else {
        Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                                title = { Text("WebPursuer") },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                titleContentColor =
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                actions = {
                                    IconButton(onClick = { showSettings = true }) {
                                        Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Settings"
                                        )
                                    }
                                }
                        )
                        androidx.compose.material3.TabRow(selectedTabIndex = selectedTab) {
                            androidx.compose.material3.Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Monitors") }
                            )
                            androidx.compose.material3.Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("Reports") }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
        ) { innerPadding ->
            if (selectedTab == 0) {
                // Monitor List
                if (monitors.isEmpty()) {
                    Column(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) { Text("No monitors yet. Add one!") }
                } else {
                    LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monitors) { monitor ->
                            MonitorItem(
                                    monitor = monitor,
                                    onClick = { selectedMonitorId = monitor.id },
                                    onDeleteClick = { monitorViewModel.deleteMonitor(monitor) }
                            )
                        }
                    }
                }
            } else {
                // Report List
                ReportList(
                        viewModel = reportViewModel,
                        onEditClick = { report ->
                            selectedReportForEdit = report
                            showReportEdit = true
                        },
                        onHistoryClick = { report -> showReportHistory = report.id },
                        innerPadding = innerPadding
                )
            }

            if (showAddDialog) {
                androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showAddDialog = false },
                        title = { Text("Create New") },
                        text = {
                            Column {
                                androidx.compose.material3.TextButton(
                                        onClick = {
                                            showAddDialog = false
                                            onAddMonitorClick()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                ) { Text("New Website Monitor") }
                                androidx.compose.material3.TextButton(
                                        onClick = {
                                            showAddDialog = false
                                            selectedReportForEdit = null
                                            showReportEdit = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                ) { Text("New Report") }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                    onClick = { showAddDialog = false }
                            ) { Text("Cancel") }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorItem(monitor: Monitor, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = monitor.name, style = MaterialTheme.typography.titleMedium)
                Text(text = monitor.url, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

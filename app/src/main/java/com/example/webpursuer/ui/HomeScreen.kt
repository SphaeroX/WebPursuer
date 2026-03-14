package com.murmli.webpursuer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.murmli.webpursuer.data.Monitor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        monitorViewModel: MonitorViewModel = viewModel(),
        reportViewModel: ReportViewModel = viewModel(),
        searchViewModel: SearchViewModel = viewModel(), // Added SearchViewModel
        generatedReportRepository: com.murmli.webpursuer.data.GeneratedReportRepository,
        initialDiffLogId: Int? = null,
        initialMonitorId: Int? = null,
        initialGeneratedReportId: Int? = null,
        onAddMonitorClick: () -> Unit
) {
    val monitors by monitorViewModel.monitors.collectAsState()
    var selectedMonitorId by remember { mutableStateOf(initialMonitorId) }

    val selectedMonitor = monitors.find { it.id == selectedMonitorId }

    // Navigation States
    var showSettings by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var showTimeline by remember { mutableStateOf(false) }
    var showReportEdit by remember { mutableStateOf(false) }
    var selectedReportForEdit by remember {
        mutableStateOf<com.murmli.webpursuer.data.Report?>(null)
    }

    // Search States
    var showSearchEdit by remember { mutableStateOf(false) }
    var selectedSearchForEdit by remember {
        mutableStateOf<com.murmli.webpursuer.data.Search?>(null)
    }

    var showSearchHistoryId by remember { mutableStateOf<Int?>(null) }
    var searchHistoryTitle by remember { mutableStateOf("") }

    var showReportHistory by remember { mutableStateOf<Int?>(null) } // Report ID
    var showReportContent by remember {
        mutableStateOf(initialGeneratedReportId)
    } // GeneratedReport ID

    // Diff Screen State
    var diffLogId by remember { mutableStateOf(initialDiffLogId) }
    var diffMonitorId by remember { mutableStateOf(initialMonitorId) }

    // Tab State
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Monitors, 1 = Reports, 2 = Searches

    // FAB Dialog State
    var showAddDialog by remember { mutableStateOf(false) }

    // Helper function to check if we're on a sub-screen
    fun isOnSubScreen(): Boolean {
        return diffLogId != null && diffMonitorId != null ||
                showLogs ||
                showTimeline ||
                showSettings ||
                showReportContent != null ||
                showReportHistory != null ||
                showReportEdit ||
                showSearchEdit ||
                showSearchHistoryId != null ||
                selectedMonitor != null
    }

    // Handle back button - hierarchical navigation
    BackHandler(enabled = isOnSubScreen()) {
        if (diffLogId != null && diffMonitorId != null) {
            diffLogId = null
            diffMonitorId = null
        } else if (showLogs) {
            showLogs = false
        } else if (showTimeline) {
            showTimeline = false
        } else if (showSettings) {
            showSettings = false
        } else if (showReportContent != null) {
            showReportContent = null
        } else if (showReportHistory != null) {
            showReportHistory = null
        } else if (showReportEdit) {
            showReportEdit = false
            selectedReportForEdit = null
        } else if (showSearchEdit) {
            showSearchEdit = false
            selectedSearchForEdit = null
        } else if (showSearchHistoryId != null) {
            showSearchHistoryId = null
        } else if (selectedMonitorId != null) {
            selectedMonitorId = null
        }
    }

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
    } else if (showTimeline) {
        RecentChangesScreen(
            viewModel = monitorViewModel,
            onBackClick = { showTimeline = false },
            onLogClick = { mId, lId ->
                diffMonitorId = mId
                diffLogId = lId
                // showTimeline = false // Keep it true or false based on preference
            }
        )
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
    } else if (showSearchEdit) {
        SearchEditScreen(
                search = selectedSearchForEdit,
                viewModel = searchViewModel,
                monitorViewModel = monitorViewModel,
                onBackClick = {
                    showSearchEdit = false
                    selectedSearchForEdit = null
                }
        )
    } else if (showSearchHistoryId != null) {
        SearchHistoryScreen(
                searchId = showSearchHistoryId!!,
                searchTitle = searchHistoryTitle,
                viewModel = searchViewModel,
                onBackClick = { showSearchHistoryId = null }
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
                                    IconButton(onClick = { showTimeline = true }) {
                                        Icon(
                                                Icons.Default.DateRange,
                                                contentDescription = "Timeline"
                                        )
                                    }
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
                            androidx.compose.material3.Tab(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    text = { Text("Searches") }
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
            when (selectedTab) {
                0 -> {
                    // Monitor List
                    if (monitors.isEmpty()) {
                        Column(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) { Text("No monitors yet. Add one!") }
                    } else {
                        // Load change percentages for all monitors
                        val changePercentages = remember(monitors) {
                            mutableStateMapOf<Int, Double?>()
                        }
                        
                        LaunchedEffect(monitors) {
                            monitors.forEach { monitor ->
                                val lastLog = monitorViewModel.getLastChangedLog(monitor.id)
                                changePercentages[monitor.id] = lastLog?.changePercentage
                            }
                        }
                        
                        LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(monitors) { monitor ->
                                MonitorItem(
                                        monitor = monitor,
                                        lastChangePercentage = changePercentages[monitor.id],
                                        onClick = { selectedMonitorId = monitor.id },
                                        onEditClick = { selectedMonitorId = monitor.id },
                                        onDeleteClick = { monitorViewModel.deleteMonitor(monitor) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Report List
                    ReportList(
                            viewModel = reportViewModel,
                            onEditClick = { report ->
                                selectedReportForEdit = report
                                showReportEdit = true
                            },
                            onHistoryClick = { report -> 
                                showReportHistory = report.id 
                            },
                            innerPadding = innerPadding
                    )
                }
                2 -> {
                    // Search List
                    SearchList(
                            viewModel = searchViewModel,
                            onEditClick = { search ->
                                selectedSearchForEdit = search
                                showSearchEdit = true
                            },
                            onHistoryClick = { search ->
                                showSearchHistoryId = search.id
                                searchHistoryTitle = search.title.ifBlank { search.prompt }
                            },
                            innerPadding = innerPadding
                    )
                }
            }
        }

        if (showAddDialog) {
            AddDialog(
                onAddMonitor = {
                    showAddDialog = false
                    onAddMonitorClick()
                },
                onAddReport = {
                    showAddDialog = false
                    showReportEdit = true
                    selectedReportForEdit = null
                },
                onAddSearch = {
                    showAddDialog = false
                    showSearchEdit = true
                    selectedSearchForEdit = null
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
fun AddDialog(
    onAddMonitor: () -> Unit,
    onAddReport: () -> Unit,
    onAddSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neu hinzufügen") },
        text = {
            Column {
                androidx.compose.material3.TextButton(
                    onClick = onAddMonitor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Monitor (Webseite)")
                    }
                }
                androidx.compose.material3.TextButton(
                    onClick = onAddReport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bericht (AI Zusammenfassung)")
                    }
                }
                androidx.compose.material3.TextButton(
                    onClick = onAddSearch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Suche (AI Web Suche)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun MonitorItem(
        monitor: Monitor,
        lastChangePercentage: Double?,
        onClick: () -> Unit,
        onEditClick: () -> Unit,
        onDeleteClick: () -> Unit
) {
    Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = monitor.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = monitor.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                )
                if (lastChangePercentage != null) {
                    Text(
                        text = "Change: ${String.format("%.1f", lastChangePercentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (lastChangePercentage > 0) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

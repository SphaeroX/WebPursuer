package com.murmli.webpursuer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmli.webpursuer.data.CheckLog
import com.murmli.webpursuer.data.Monitor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentChangesScreen(
    viewModel: MonitorViewModel,
    onBackClick: () -> Unit,
    onLogClick: (monitorId: Int, logId: Int) -> Unit
) {
    val pageSize by viewModel.recentChangesPageSize.collectAsState(initial = 10)
    val sortOrder by viewModel.recentChangesSortOrder.collectAsState(initial = "DESC")
    
    var currentPage by remember { mutableIntStateOf(0) }
    var logs by remember { mutableStateOf<List<CheckLog>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    
    val monitors by viewModel.monitors.collectAsState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Load data when page, size or sort order changes
    LaunchedEffect(currentPage, pageSize, sortOrder) {
        isLoading = true
        totalCount = viewModel.getTotalChangedCount()
        logs = viewModel.getRecentChangesPaged(pageSize, currentPage * pageSize, sortOrder)
        isLoading = false
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Timeline Settings") },
            text = {
                Column {
                    Text("Logs per page:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = pageSize.toFloat(),
                            onValueChange = { viewModel.setRecentChangesPageSize(it.toInt()) },
                            valueRange = 5f..50f,
                            steps = 9,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${pageSize}", modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Sorting:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = sortOrder == "DESC",
                            onClick = { viewModel.setRecentChangesSortOrder("DESC") }
                        )
                        Text("Newest first", modifier = Modifier.clickable { viewModel.setRecentChangesSortOrder("DESC") })
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        RadioButton(
                            selected = sortOrder == "ASC",
                            onClick = { viewModel.setRecentChangesSortOrder("ASC") }
                        )
                        Text("Oldest first", modifier = Modifier.clickable { viewModel.setRecentChangesSortOrder("ASC") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Changes Timeline") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No changes found.", color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(logs) { log ->
                        val monitor = monitors.find { it.id == log.monitorId }
                        RecentChangeItem(log, monitor, onLogClick)
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
                
                // Pagination Controls
                val totalPages = (totalCount + pageSize - 1) / pageSize
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Arrangement.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous", tint = if (currentPage > 0) Color.White else Color.Gray)
                        }
                        
                        Text("Page ${currentPage + 1} of $totalPages", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                        
                        IconButton(
                            onClick = { if (currentPage < totalPages - 1) currentPage++ },
                            enabled = currentPage < totalPages - 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = if (currentPage < totalPages - 1) Color.White else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentChangeItem(log: CheckLog, monitor: Monitor?, onClick: (Int, Int) -> Unit) {
    val dateStr = SimpleDateFormat("dd.MM.yy, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(log.monitorId, log.id) }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = monitor?.name ?: "Unknown Monitor",
                color = Color(0xFF64B5F6),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dateStr,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = log.message,
            color = Color.White,
            fontSize = 14.sp
        )
        
        if (log.changePercentage != null) {
            Text(
                text = "Change: ${String.format("%.1f", log.changePercentage)}%",
                color = if (log.changePercentage!! > 0) Color(0xFF81C784) else Color.White,
                fontSize = 12.sp
            )
        }
    }
}

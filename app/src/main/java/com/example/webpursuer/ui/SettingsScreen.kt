package com.example.webpursuer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.webpursuer.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    
    val apiKey by settingsRepository.apiKey.collectAsState(initial = null)
    val model by settingsRepository.model.collectAsState(initial = "google/gemini-2.5-flash")
    
    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }
    
    // Update inputs when data loads
    LaunchedEffect(apiKey, model) {
        if (apiKeyInput.isEmpty()) apiKeyInput = apiKey ?: ""
        if (modelInput.isEmpty()) modelInput = model
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            val notificationsEnabled by settingsRepository.notificationsEnabled.collectAsState(initial = true)

            Text("Allgemein", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Benachrichtigungen aktivieren")
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { 
                        scope.launch {
                            settingsRepository.saveNotificationsEnabled(it)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // REPORTS SECTION
            val reportEnabled by settingsRepository.reportEnabled.collectAsState(initial = false)
            val reportTimeHour by settingsRepository.reportTimeHour.collectAsState(initial = 8)
            val reportSelection by settingsRepository.reportMonitorSelection.collectAsState(initial = emptySet())
            var showMonitorDialog by remember { mutableStateOf(false) }

            Text("Daily Reports (LLM)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Daily Reports")
                Switch(
                    checked = reportEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsRepository.saveReportEnabled(enabled)
                            // Schedule logic handled below in Save or via a side effect? 
                            // Creating the worker request here is cleaner for IMMEDIATE feedback, 
                            // but usually we wait for "Save". 
                            // However, let's keep it simple: Save handles persistence, 
                            // but we also need to trigger WorkManager update.
                        }
                    }
                )
            }

            if (reportEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Report Time: ${String.format("%02d:00", reportTimeHour)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = reportTimeHour.toFloat(),
                    onValueChange = { scope.launch { settingsRepository.saveReportTimeHour(it.toInt()) } },
                    valueRange = 0f..23f,
                    steps = 22
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { showMonitorDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (reportSelection.isEmpty()) "Select Websites (All)" else "Select Websites (${reportSelection.size})")
                }
            }

            if (showMonitorDialog) {
                ReportSettingsDialog(
                    monitorDao = com.example.webpursuer.data.AppDatabase.getDatabase(context).monitorDao(),
                    initialSelection = reportSelection,
                    onDismiss = { showMonitorDialog = false },
                    onConfirm = { selection ->
                        scope.launch {
                            settingsRepository.saveReportMonitorSelection(selection)
                        }
                        showMonitorDialog = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("OpenRouter Configuration", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Model (e.g., google/gemini-2.5-flash)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            var testResult by remember { mutableStateOf<String?>(null) }
            var isTesting by remember { mutableStateOf(false) }
            val openRouterService = remember { com.example.webpursuer.network.OpenRouterService(settingsRepository) }

            Button(
                onClick = {
                    isTesting = true
                    testResult = null
                    scope.launch {
                        testResult = openRouterService.testConnection(apiKeyInput, modelInput)
                        isTesting = false
                    }
                },
                enabled = !isTesting && apiKeyInput.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Text("Test Connection")
                }
            }
            
            if (testResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testResult!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (testResult!!.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        settingsRepository.saveApiKey(apiKeyInput)
                        settingsRepository.saveModel(modelInput)
                        
                        // Reschedule Report Worker
                        scheduleReportWorker(context, reportEnabled, reportTimeHour)
                        
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

fun scheduleReportWorker(context: android.content.Context, enabled: Boolean, hour: Int) {
    if (!enabled) {
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("ReportWorker")
        return
    }

    val now = java.util.Calendar.getInstance()
    val target = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    // If target is before now, schedule for tomorrow
    if (target.before(now)) {
        target.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }

    val initialDelay = target.timeInMillis - now.timeInMillis

    val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.webpursuer.worker.ReportWorker>(24, java.util.concurrent.TimeUnit.HOURS)
        .setInitialDelay(initialDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "ReportWorker",
        androidx.work.ExistingPeriodicWorkPolicy.UPDATE, // Update to apply new time
        workRequest
    )
}

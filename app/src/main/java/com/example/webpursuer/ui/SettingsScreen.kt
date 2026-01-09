package com.murmli.webpursuer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.murmli.webpursuer.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, onLogsClick: () -> Unit) {
        val context = LocalContext.current
        val settingsRepository = remember { SettingsRepository(context) }
        val scope = rememberCoroutineScope()

        val apiKey by settingsRepository.apiKey.collectAsState(initial = null)
        val reportModel by
                settingsRepository.reportModel.collectAsState(
                        initial = "google/gemini-3-flash-preview"
                )
        val monitorModel by
                settingsRepository.monitorModel.collectAsState(
                        initial = "google/gemini-3-flash-preview"
                )

        var apiKeyInput by remember { mutableStateOf("") }
        var reportModelInput by remember { mutableStateOf("") }
        var monitorModelInput by remember { mutableStateOf("") }

        // Update inputs when data loads
        LaunchedEffect(apiKey, reportModel, monitorModel) {
                if (apiKeyInput.isEmpty()) apiKeyInput = apiKey ?: ""
                if (reportModelInput.isEmpty()) reportModelInput = reportModel
                if (monitorModelInput.isEmpty()) monitorModelInput = monitorModel
        }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Settings") },
                                navigationIcon = {
                                        IconButton(onClick = onBackClick) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                }
                        )
                }
        ) { innerPadding ->
                Column(
                        modifier =
                                Modifier.padding(innerPadding)
                                        .padding(16.dp)
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                ) {
                        val notificationsEnabled by
                                settingsRepository.notificationsEnabled.collectAsState(
                                        initial = true
                                )

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
                                                        settingsRepository.saveNotificationsEnabled(
                                                                it
                                                        )
                                                }
                                        }
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                                "OpenRouter (LLM) Configuration",
                                style = MaterialTheme.typography.titleMedium
                        )
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
                                value = reportModelInput,
                                onValueChange = { reportModelInput = it },
                                label = {
                                        Text("Report Model (e.g., google/gemini-3-flash-preview)")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                                value = monitorModelInput,
                                onValueChange = { monitorModelInput = it },
                                label = { Text("Monitor/Interpret Model") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        var testResult by remember { mutableStateOf<String?>(null) }
                        var isTesting by remember { mutableStateOf(false) }

                        val database = remember {
                                com.murmli.webpursuer.data.AppDatabase.getDatabase(context)
                        }
                        val logRepository = remember {
                                com.murmli.webpursuer.data.LogRepository(database.appLogDao())
                        }
                        val openRouterService = remember {
                                com.murmli.webpursuer.network.OpenRouterService(
                                        settingsRepository,
                                        logRepository
                                )
                        }

                        Button(
                                onClick = {
                                        isTesting = true
                                        testResult = null
                                        scope.launch {
                                                // Testing with Report Model as primary validation
                                                testResult =
                                                        openRouterService.testConnection(
                                                                apiKeyInput,
                                                                reportModelInput
                                                        )
                                                isTesting = false
                                        }
                                },
                                enabled = !isTesting && apiKeyInput.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                        )
                        ) {
                                if (isTesting) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.onSecondary
                                        )
                                } else {
                                        Text("Test Connection (Report Model)")
                                }
                        }

                        if (testResult != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = testResult!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                if (testResult!!.startsWith("Error"))
                                                        MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.primary
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                                onClick = {
                                        scope.launch {
                                                settingsRepository.saveApiKey(apiKeyInput)
                                                settingsRepository.saveReportModel(reportModelInput)
                                                settingsRepository.saveMonitorModel(
                                                        monitorModelInput
                                                )
                                                onBackClick()
                                        }
                                },
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Save") }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Advanced", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        val pm =
                                context.getSystemService(android.content.Context.POWER_SERVICE) as
                                        android.os.PowerManager
                        val isIgnoringBatteryOptimizations =
                                if (android.os.Build.VERSION.SDK_INT >=
                                                android.os.Build.VERSION_CODES.M
                                ) {
                                        pm.isIgnoringBatteryOptimizations(context.packageName)
                                } else {
                                        true
                                }

                        if (!isIgnoringBatteryOptimizations &&
                                        android.os.Build.VERSION.SDK_INT >=
                                                android.os.Build.VERSION_CODES.M
                        ) {
                                Button(
                                        onClick = {
                                                val intent =
                                                        android.content.Intent(
                                                                        android.provider.Settings
                                                                                .ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                                                )
                                                                .apply {
                                                                        data =
                                                                                android.net.Uri
                                                                                        .parse(
                                                                                                "package:${context.packageName}"
                                                                                        )
                                                                }
                                                context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.tertiary
                                                )
                                ) { Text("Disable Battery Optimization (Fix Background Issues)") }
                                Spacer(modifier = Modifier.height(16.dp))
                        } else if (android.os.Build.VERSION.SDK_INT >=
                                        android.os.Build.VERSION_CODES.M
                        ) {
                                Text(
                                        text = "Battery Optimization: Disabled (Good)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(
                                onClick = onLogsClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                        )
                        ) { Text("View System Logs") }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Datenverwaltung", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        val backupRepository = remember {
                                com.murmli.webpursuer.data.BackupRepository(context)
                        }

                        // Export Launcher
                        val exportLauncher =
                                rememberLauncherForActivityResult(
                                        androidx.activity.result.contract.ActivityResultContracts
                                                .StartActivityForResult()
                                ) {
                                        // No result handling needed strictly, but good for
                                        // validation
                                }

                        // Import Launcher
                        val importLauncher =
                                rememberLauncherForActivityResult(
                                        androidx.activity.result.contract.ActivityResultContracts
                                                .GetContent()
                                ) { uri: android.net.Uri? ->
                                        uri?.let {
                                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                        val success =
                                                                backupRepository.importDatabase(it)
                                                        kotlinx.coroutines.withContext(
                                                                kotlinx.coroutines.Dispatchers.Main
                                                        ) {
                                                                if (success) {
                                                                        android.widget.Toast
                                                                                .makeText(
                                                                                        context,
                                                                                        "Datenbank erfolgreich wiederhergestellt. Bitte Starten Sie die App neu.",
                                                                                        android.widget
                                                                                                .Toast
                                                                                                .LENGTH_LONG
                                                                                )
                                                                                .show()
                                                                } else {
                                                                        android.widget.Toast
                                                                                .makeText(
                                                                                        context,
                                                                                        "Fehler beim Wiederherstellen der Datenbank.",
                                                                                        android.widget
                                                                                                .Toast
                                                                                                .LENGTH_LONG
                                                                                )
                                                                                .show()
                                                                }
                                                        }
                                                }
                                        }
                                }

                        Button(
                                onClick = {
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                val uri = backupRepository.exportDatabase()
                                                if (uri != null) {
                                                        val shareIntent =
                                                                android.content.Intent(
                                                                                android.content
                                                                                        .Intent
                                                                                        .ACTION_SEND
                                                                        )
                                                                        .apply {
                                                                                type =
                                                                                        "application/octet-stream"
                                                                                putExtra(
                                                                                        android.content
                                                                                                .Intent
                                                                                                .EXTRA_STREAM,
                                                                                        uri
                                                                                )
                                                                                addFlags(
                                                                                        android.content
                                                                                                .Intent
                                                                                                .FLAG_GRANT_READ_URI_PERMISSION
                                                                                )
                                                                        }
                                                        exportLauncher.launch(
                                                                android.content.Intent
                                                                        .createChooser(
                                                                                shareIntent,
                                                                                "Backup teilen"
                                                                        )
                                                        )
                                                }
                                        }
                                },
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Backup erstellen (Export)") }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                                onClick = { importLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                        ) { Text("Backup wiederherstellen (Import)") }
                }
        }
}

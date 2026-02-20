package com.murmli.webpursuer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.murmli.webpursuer.data.Search
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEditScreen(
        search: Search?,
        viewModel: SearchViewModel,
        monitorViewModel: MonitorViewModel, // For API key flow
        onBackClick: () -> Unit
) {
    // If search is null, we are creating a new one
    var promptInput by remember { mutableStateOf(search?.prompt ?: "") }
    var titleInput by remember { mutableStateOf(search?.title ?: "") }

    // Schedule State
    var scheduleType by remember { mutableStateOf(search?.scheduleType ?: "INTERVAL") }
    var intervalMinutes by remember { mutableLongStateOf(search?.intervalMinutes ?: 60L) }
    var scheduleHour by remember { mutableIntStateOf(search?.scheduleHour ?: 0) }
    var scheduleMinute by remember { mutableIntStateOf(search?.scheduleMinute ?: 0) }
    var scheduleDays by remember { mutableIntStateOf(search?.scheduleDays ?: 127) }

    // Other Settings
    var notificationsEnabled by remember { mutableStateOf(search?.notificationEnabled ?: true) }
    var aiConditionEnabled by remember { mutableStateOf(search?.aiConditionEnabled ?: false) }
    var aiConditionPrompt by remember { mutableStateOf(search?.aiConditionPrompt ?: "") }

    val apiKey by monitorViewModel.apiKey.collectAsState(initial = null)
    val isAiEnabled = !apiKey.isNullOrBlank()

    // Helper to update or create
    fun saveSearch() {
        val newSearch =
                Search(
                        id = search?.id ?: 0,
                        title = titleInput,
                        prompt = promptInput,
                        scheduleType = scheduleType,
                        intervalMinutes = intervalMinutes,
                        scheduleHour = scheduleHour,
                        scheduleMinute = scheduleMinute,
                        scheduleDays = scheduleDays,
                        enabled = search?.enabled ?: true,
                        notificationEnabled = notificationsEnabled,
                        aiConditionEnabled = aiConditionEnabled,
                        aiConditionPrompt = if (aiConditionEnabled) aiConditionPrompt else null,
                        lastRunTime = search?.lastRunTime ?: 0L
                )
        if (search == null) {
            viewModel.addSearch(newSearch)
        } else {
            viewModel.updateSearch(newSearch)
        }
        onBackClick()
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(if (search == null) "New Search" else "Edit Search") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (search != null) {
                                IconButton(onClick = { viewModel.runSearchNow(search.id) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Run Now")
                                }
                            }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { saveSearch() }) {
                    Text("Save", modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
    ) { innerPadding ->
        Column(
                modifier =
                        Modifier.padding(innerPadding)
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Prompt Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search Query", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Title (Display Name)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            label = { Text("What to search for?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                    )
                }
            }

            // Schedule Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Schedule Settings", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Dropdown for Schedule Type
                    val options =
                            listOf("Specific Time" to "SPECIFIC_TIME", "Interval" to "INTERVAL")
                    var expandedDropdown by remember { mutableStateOf(false) }
                    val selectedOptionText =
                            options.find { it.second == scheduleType }?.first ?: "Interval"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = !expandedDropdown },
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    readOnly = true,
                                    value = selectedOptionText,
                                    onValueChange = {},
                                    label = { Text("Schedule Type") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expandedDropdown
                                        )
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false },
                            ) {
                                options.forEach { selectionOption ->
                                    DropdownMenuItem(
                                            text = { Text(selectionOption.first) },
                                            onClick = {
                                                scheduleType = selectionOption.second
                                                expandedDropdown = false
                                            },
                                            contentPadding =
                                                    ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                    }

                    if (scheduleType == "SPECIFIC_TIME") {
                        val context = LocalContext.current
                        val timePickerDialog =
                                android.app.TimePickerDialog(
                                        context,
                                        { _, hour: Int, minute: Int ->
                                            scheduleHour = hour
                                            scheduleMinute = minute
                                        },
                                        scheduleHour,
                                        scheduleMinute,
                                        true
                                )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                    "Time: ${String.format(Locale.getDefault(), "%02d:%02d", scheduleHour, scheduleMinute)}",
                                    style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = { timePickerDialog.show() }) { Text("Set Time") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // Days
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
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
                                                        if (checked) scheduleDays or mask
                                                        else scheduleDays and mask.inv()
                                            }
                                    )
                                    Text(dayName, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    } else {
                        // Interval Input
                        Spacer(modifier = Modifier.height(8.dp))
                        // Simplified interval input (minutes)
                        var intervalText by remember { mutableStateOf(intervalMinutes.toString()) }
                        OutlinedTextField(
                                value = intervalText,
                                onValueChange = {
                                    intervalText = it
                                    if (it.all { char -> char.isDigit() }) {
                                        intervalMinutes = it.toLongOrNull() ?: 60L
                                    }
                                },
                                label = { Text("Interval (minutes)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                        )
                    }
                }
            }

            // Notifications Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Notifications", modifier = Modifier.weight(1f))
                        Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                }
            }

            // AI Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Configuration", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isAiEnabled) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "OpenRouter API Key required in Settings",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Intelligent Condition (LLM)",
                                color = if (isAiEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                "Notify only if condition met",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAiEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        Switch(
                                checked = aiConditionEnabled,
                                onCheckedChange = { aiConditionEnabled = it },
                                enabled = isAiEnabled
                        )
                    }

                    if (aiConditionEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                                value = aiConditionPrompt,
                                onValueChange = { aiConditionPrompt = it },
                                label = { Text("Condition (e.g. Is price < 100?)") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("The AI will answer YES or NO") },
                                enabled = isAiEnabled
                        )
                    }
                }
            }

            // Spacer for FAB
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

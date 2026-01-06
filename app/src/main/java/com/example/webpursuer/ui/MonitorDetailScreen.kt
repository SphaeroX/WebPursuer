package com.murmli.webpursuer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.murmli.webpursuer.data.CheckLog
import com.murmli.webpursuer.data.Monitor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDetailScreen(
        monitor: Monitor,
        viewModel: MonitorViewModel = viewModel(),
        onBackClick: () -> Unit,
        onLogClick: (CheckLog) -> Unit
) {
        val logs by viewModel.getLogsForMonitor(monitor.id).collectAsState(initial = emptyList())
        // Local state for immediate input feedback to avoid cursor jumping
        // We use remember(monitor.id) so it resets if we switch monitors somehow,
        // but not strictly necessary if screen is recreated.
        var selectorInput by remember(monitor.id) { mutableStateOf(monitor.selector) }
        var aiInstructionInput by
                remember(monitor.id) { mutableStateOf(monitor.aiInterpreterInstruction) }
        var llmPromptInput by remember(monitor.id) { mutableStateOf(monitor.llmPrompt ?: "") }

        // Dropdown state
        var expanded by remember { mutableStateOf(false) }
        val intervals = listOf(10L to "10 Minutes", 60L to "1 Hour", 1440L to "24 Hours")

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text(monitor.name) },
                                navigationIcon = {
                                        IconButton(onClick = onBackClick) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                },
                                actions = {
                                        IconButton(onClick = { viewModel.checkNow(monitor) }) {
                                                Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = "Check Now"
                                                )
                                        }
                                }
                        )
                }
        ) { innerPadding ->
                // Use LazyColumn for the WHOLE screen content to ensure scrolling works
                // even with keyboard open and lists at the bottom.
                LazyColumn(
                        modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // ITEM 1: General Info Card
                        item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                        "URL: ${monitor.url}",
                                                        style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Selector Input with local state
                                                OutlinedTextField(
                                                        value = selectorInput,
                                                        onValueChange = {
                                                                selectorInput = it
                                                                viewModel.updateMonitor(
                                                                        monitor.copy(selector = it)
                                                                )
                                                        },
                                                        label = { Text("CSS Selector") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        trailingIcon = {
                                                                val context =
                                                                        androidx.compose.ui.platform
                                                                                .LocalContext
                                                                                .current
                                                                IconButton(
                                                                        onClick = {
                                                                                val intent =
                                                                                        android.content
                                                                                                .Intent(
                                                                                                        context,
                                                                                                        BrowserActivity::class
                                                                                                                .java
                                                                                                )
                                                                                                .apply {
                                                                                                        putExtra(
                                                                                                                "monitorId",
                                                                                                                monitor.id
                                                                                                        )
                                                                                                        putExtra(
                                                                                                                "url",
                                                                                                                monitor.url
                                                                                                        )
                                                                                                }
                                                                                context.startActivity(
                                                                                        intent
                                                                                )
                                                                        }
                                                                ) {
                                                                        Icon(
                                                                                Icons.Default.Edit,
                                                                                contentDescription =
                                                                                        "Edit in Browser"
                                                                        )
                                                                }
                                                        }
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Schedule Settings
                                                Text(
                                                        "Schedule Type",
                                                        style = MaterialTheme.typography.titleMedium
                                                )

                                                // Update state variables to use properties from
                                                // monitor
                                                var scheduleType by remember {
                                                        mutableStateOf(monitor.scheduleType)
                                                }
                                                var scheduleHour by remember {
                                                        mutableIntStateOf(monitor.scheduleHour)
                                                }
                                                var scheduleMinute by remember {
                                                        mutableIntStateOf(monitor.scheduleMinute)
                                                }
                                                var scheduleDays by remember {
                                                        mutableIntStateOf(monitor.scheduleDays)
                                                }

                                                // Schedule Type Dropdown
                                                val options =
                                                        listOf(
                                                                "Specific Time (Weekly/Daily)" to
                                                                        "SPECIFIC_TIME",
                                                                "Interval" to "INTERVAL"
                                                        )
                                                // Map legacy "DAILY" to "SPECIFIC_TIME" for UI
                                                // display if needed, or just let it be
                                                val effectiveScheduleType =
                                                        if (scheduleType == "DAILY") "SPECIFIC_TIME"
                                                        else scheduleType

                                                val selectedOptionText =
                                                        options
                                                                .find {
                                                                        it.second ==
                                                                                effectiveScheduleType
                                                                }
                                                                ?.first
                                                                ?: options[0].first
                                                var expandedDropdown by remember {
                                                        mutableStateOf(false)
                                                }

                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 8.dp)
                                                ) {
                                                        ExposedDropdownMenuBox(
                                                                expanded = expandedDropdown,
                                                                onExpandedChange = {
                                                                        expandedDropdown =
                                                                                !expandedDropdown
                                                                },
                                                                modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                                OutlinedTextField(
                                                                        modifier =
                                                                                Modifier.menuAnchor()
                                                                                        .fillMaxWidth(),
                                                                        readOnly = true,
                                                                        value = selectedOptionText,
                                                                        onValueChange = {},
                                                                        label = {
                                                                                Text(
                                                                                        "Schedule Type"
                                                                                )
                                                                        },
                                                                        trailingIcon = {
                                                                                ExposedDropdownMenuDefaults
                                                                                        .TrailingIcon(
                                                                                                expanded =
                                                                                                        expandedDropdown
                                                                                        )
                                                                        },
                                                                        colors =
                                                                                ExposedDropdownMenuDefaults
                                                                                        .outlinedTextFieldColors(),
                                                                )
                                                                ExposedDropdownMenu(
                                                                        expanded = expandedDropdown,
                                                                        onDismissRequest = {
                                                                                expandedDropdown =
                                                                                        false
                                                                        },
                                                                ) {
                                                                        options.forEach {
                                                                                selectionOption ->
                                                                                DropdownMenuItem(
                                                                                        text = {
                                                                                                Text(
                                                                                                        selectionOption
                                                                                                                .first
                                                                                                )
                                                                                        },
                                                                                        onClick = {
                                                                                                scheduleType =
                                                                                                        selectionOption
                                                                                                                .second
                                                                                                expandedDropdown =
                                                                                                        false
                                                                                                // Update database immediately
                                                                                                viewModel
                                                                                                        .updateMonitor(
                                                                                                                monitor.copy(
                                                                                                                        scheduleType =
                                                                                                                                selectionOption
                                                                                                                                        .second
                                                                                                                )
                                                                                                        )
                                                                                        },
                                                                                        contentPadding =
                                                                                                ExposedDropdownMenuDefaults
                                                                                                        .ItemContentPadding,
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }

                                                val context = LocalContext.current
                                                val timePickerDialog =
                                                        android.app.TimePickerDialog(
                                                                context,
                                                                { _, hour: Int, minute: Int ->
                                                                        scheduleHour = hour
                                                                        scheduleMinute = minute
                                                                        viewModel.updateMonitor(
                                                                                monitor.copy(
                                                                                        scheduleHour =
                                                                                                hour,
                                                                                        scheduleMinute =
                                                                                                minute
                                                                                )
                                                                        )
                                                                },
                                                                scheduleHour,
                                                                scheduleMinute,
                                                                true
                                                        )

                                                if (effectiveScheduleType == "SPECIFIC_TIME") {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                "Time: ${String.format(Locale.getDefault(), "%02d:%02d", scheduleHour, scheduleMinute)}",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyLarge
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                Button(
                                                                        onClick = {
                                                                                timePickerDialog
                                                                                        .show()
                                                                        }
                                                                ) { Text("Set Time") }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                                "Repeat on:",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium
                                                        )
                                                        // Day Selection
                                                        val days =
                                                                listOf(
                                                                        "Mon",
                                                                        "Tue",
                                                                        "Wed",
                                                                        "Thu",
                                                                        "Fri",
                                                                        "Sat",
                                                                        "Sun"
                                                                )
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween
                                                        ) {
                                                                days.forEachIndexed { index, dayName
                                                                        ->
                                                                        val mask = 1 shl index
                                                                        val isSelected =
                                                                                (scheduleDays and
                                                                                        mask) != 0
                                                                        Column(
                                                                                horizontalAlignment =
                                                                                        Alignment
                                                                                                .CenterHorizontally
                                                                        ) {
                                                                                Checkbox(
                                                                                        checked =
                                                                                                isSelected,
                                                                                        onCheckedChange = {
                                                                                                checked
                                                                                                ->
                                                                                                val newDays =
                                                                                                        if (checked
                                                                                                        ) {
                                                                                                                scheduleDays or
                                                                                                                        mask
                                                                                                        } else {
                                                                                                                scheduleDays and
                                                                                                                        mask.inv()
                                                                                                        }
                                                                                                scheduleDays =
                                                                                                        newDays
                                                                                                viewModel
                                                                                                        .updateMonitor(
                                                                                                                monitor.copy(
                                                                                                                        scheduleDays =
                                                                                                                                newDays
                                                                                                                )
                                                                                                        )
                                                                                        }
                                                                                )
                                                                                Text(
                                                                                        dayName,
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .labelSmall
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                } else {
                                                        // INTERVAL
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // Local state for interval text
                                                        var intervalText by
                                                                remember(
                                                                        monitor.checkIntervalMinutes
                                                                ) {
                                                                        if (monitor.checkIntervalMinutes ==
                                                                                        0L
                                                                        )
                                                                                mutableStateOf("")
                                                                        else if (monitor.checkIntervalMinutes %
                                                                                        60 == 0L
                                                                        )
                                                                                mutableStateOf(
                                                                                        "${monitor.checkIntervalMinutes / 60}h"
                                                                                )
                                                                        else
                                                                                mutableStateOf(
                                                                                        "${monitor.checkIntervalMinutes}m"
                                                                                )
                                                                }

                                                        OutlinedTextField(
                                                                value = intervalText,
                                                                onValueChange = { input ->
                                                                        intervalText = input
                                                                        // Parse input logic
                                                                        var totalMinutes = 0L
                                                                        val hourMatch =
                                                                                Regex("(\\d+)\\s*h")
                                                                                        .find(input)
                                                                        val minMatch =
                                                                                Regex("(\\d+)\\s*m")
                                                                                        .find(input)

                                                                        if (hourMatch != null) {
                                                                                totalMinutes +=
                                                                                        (hourMatch
                                                                                                        .groupValues[
                                                                                                        1]
                                                                                                .toLongOrNull()
                                                                                                ?: 0) *
                                                                                                60
                                                                        }
                                                                        if (minMatch != null) {
                                                                                totalMinutes +=
                                                                                        (minMatch.groupValues[
                                                                                                        1]
                                                                                                .toLongOrNull()
                                                                                                ?: 0)
                                                                        }
                                                                        if (totalMinutes == 0L &&
                                                                                        input.all {
                                                                                                it.isDigit()
                                                                                        }
                                                                        ) {
                                                                                totalMinutes =
                                                                                        input.toLongOrNull()
                                                                                                ?: 0L
                                                                        }

                                                                        if (totalMinutes > 0) {
                                                                                viewModel
                                                                                        .updateMonitor(
                                                                                                monitor.copy(
                                                                                                        checkIntervalMinutes =
                                                                                                                totalMinutes
                                                                                                )
                                                                                        )
                                                                        }
                                                                },
                                                                label = {
                                                                        Text(
                                                                                "Interval (e.g. 15m, 1h)"
                                                                        )
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                singleLine = true,
                                                                supportingText = {
                                                                        Text(
                                                                                "Android minimum is 15 minutes"
                                                                        )
                                                                }
                                                        )

                                                        Text(
                                                                "Start first run at:",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                modifier =
                                                                        Modifier.padding(top = 8.dp)
                                                        )
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                "${String.format(Locale.getDefault(), "%02d:%02d", scheduleHour, scheduleMinute)}",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyLarge
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                Button(
                                                                        onClick = {
                                                                                timePickerDialog
                                                                                        .show()
                                                                        }
                                                                ) { Text("Set Start Time") }
                                                        }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))
                                                Divider()
                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Notification Toggle
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Text(
                                                                "Send Notifications",
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                        Switch(
                                                                checked =
                                                                        monitor.notificationsEnabled,
                                                                onCheckedChange = {
                                                                        viewModel.updateMonitor(
                                                                                monitor.copy(
                                                                                        notificationsEnabled =
                                                                                                it
                                                                                )
                                                                        )
                                                                }
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // AI Configuration Card
                                                val apiKey by
                                                        viewModel.apiKey.collectAsState(
                                                                initial = null
                                                        )
                                                val isAiEnabled = !apiKey.isNullOrBlank()

                                                Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors =
                                                                CardDefaults.cardColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                )
                                                ) {
                                                        Column(modifier = Modifier.padding(16.dp)) {
                                                                Text(
                                                                        "AI Configuration",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleMedium
                                                                )

                                                                if (!isAiEnabled) {
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Card(
                                                                                colors =
                                                                                        CardDefaults
                                                                                                .cardColors(
                                                                                                        containerColor =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .errorContainer
                                                                                                ),
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                        ) {
                                                                                Row(
                                                                                        modifier =
                                                                                                Modifier.padding(
                                                                                                        8.dp
                                                                                                ),
                                                                                        verticalAlignment =
                                                                                                Alignment
                                                                                                        .CenterVertically
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Filled
                                                                                                        .Info,
                                                                                                contentDescription =
                                                                                                        "Info",
                                                                                                tint =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onErrorContainer
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.width(
                                                                                                                8.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "OpenRouter API Key required in Settings",
                                                                                                color =
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onErrorContainer,
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .bodySmall,
                                                                                                modifier =
                                                                                                        Modifier.weight(
                                                                                                                1f
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        8.dp
                                                                                )
                                                                )

                                                                // AI Data Interpretation
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) {
                                                                        Text(
                                                                                "Interpret Data with AI",
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        ),
                                                                                color =
                                                                                        if (isAiEnabled
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.38f
                                                                                                        )
                                                                        )
                                                                        Switch(
                                                                                checked =
                                                                                        monitor.useAiInterpreter,
                                                                                enabled =
                                                                                        isAiEnabled,
                                                                                onCheckedChange = {
                                                                                        viewModel
                                                                                                .updateMonitor(
                                                                                                        monitor.copy(
                                                                                                                useAiInterpreter =
                                                                                                                        it
                                                                                                        )
                                                                                                )
                                                                                }
                                                                        )
                                                                }

                                                                if (monitor.useAiInterpreter) {
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        OutlinedTextField(
                                                                                value =
                                                                                        aiInstructionInput,
                                                                                onValueChange = {
                                                                                        aiInstructionInput =
                                                                                                it
                                                                                        viewModel
                                                                                                .updateMonitor(
                                                                                                        monitor.copy(
                                                                                                                aiInterpreterInstruction =
                                                                                                                        it
                                                                                                        )
                                                                                                )
                                                                                },
                                                                                label = {
                                                                                        Text(
                                                                                                "Interpretation Instruction"
                                                                                        )
                                                                                },
                                                                                placeholder = {
                                                                                        Text(
                                                                                                "e.g. Extract the price, Summarize the table"
                                                                                        )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth(),
                                                                                minLines = 2,
                                                                                enabled =
                                                                                        isAiEnabled
                                                                        )
                                                                }

                                                                Divider(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        vertical =
                                                                                                12.dp
                                                                                )
                                                                )

                                                                // LLM Condition
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) {
                                                                        Text(
                                                                                "Intelligent Condition (LLM)",
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        ),
                                                                                color =
                                                                                        if (isAiEnabled
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.38f
                                                                                                        )
                                                                        )
                                                                        Switch(
                                                                                checked =
                                                                                        monitor.llmEnabled,
                                                                                enabled =
                                                                                        isAiEnabled,
                                                                                onCheckedChange = {
                                                                                        viewModel
                                                                                                .updateMonitor(
                                                                                                        monitor.copy(
                                                                                                                llmEnabled =
                                                                                                                        it
                                                                                                        )
                                                                                                )
                                                                                }
                                                                        )
                                                                }

                                                                if (monitor.llmEnabled) {
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        OutlinedTextField(
                                                                                value =
                                                                                        llmPromptInput,
                                                                                onValueChange = {
                                                                                        llmPromptInput =
                                                                                                it
                                                                                        viewModel
                                                                                                .updateMonitor(
                                                                                                        monitor.copy(
                                                                                                                llmPrompt =
                                                                                                                        it
                                                                                                        )
                                                                                                )
                                                                                },
                                                                                label = {
                                                                                        Text(
                                                                                                "Condition Prompt"
                                                                                        )
                                                                                },
                                                                                placeholder = {
                                                                                        Text(
                                                                                                "e.g., Notify if price is below $50"
                                                                                        )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth(),
                                                                                minLines = 2,
                                                                                enabled =
                                                                                        isAiEnabled
                                                                        )
                                                                }

                                                                Divider(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        vertical =
                                                                                                12.dp
                                                                                )
                                                                )

                                                                // Web Search
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "Enable Web Search",
                                                                                        color =
                                                                                                if (isAiEnabled
                                                                                                )
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurface
                                                                                                else
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurface
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.38f
                                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        "Allow AI to search the web for context",
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodySmall,
                                                                                        color =
                                                                                                if (isAiEnabled
                                                                                                )
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurfaceVariant
                                                                                                else
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurfaceVariant
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.38f
                                                                                                                )
                                                                                )
                                                                        }
                                                                        Switch(
                                                                                checked =
                                                                                        monitor.useWebSearch,
                                                                                enabled =
                                                                                        isAiEnabled,
                                                                                onCheckedChange = {
                                                                                        viewModel
                                                                                                .updateMonitor(
                                                                                                        monitor.copy(
                                                                                                                useWebSearch =
                                                                                                                        it
                                                                                                        )
                                                                                                )
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        // ITEM 2: Section Header
                        item { Text("Check History", style = MaterialTheme.typography.titleMedium) }

                        // ITEM 3: Logs List (directly as items in parent LazyColumn)
                        items(logs) { log -> LogItem(log, onClick = { onLogClick(log) }) }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogItem(log: CheckLog, onClick: () -> Unit) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        dateFormat.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall
                                )
                                Text(log.message, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                                text = log.result,
                                color =
                                        if (log.result == "CHANGED") MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium
                        )
                }
        }
}

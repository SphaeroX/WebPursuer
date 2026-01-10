package com.murmli.webpursuer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murmli.webpursuer.data.SearchLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(
        searchId: Int,
        searchTitle: String,
        viewModel: SearchViewModel,
        onBackClick: () -> Unit
) {
    val logs by viewModel.getLogsForSearch(searchId).collectAsState(initial = emptyList())
    // Make sure we import SearchLog correctly or use fully qualified if needed, assuming data
    // package is correct
    var selectedLog by remember { mutableStateOf<SearchLog?>(null) }

    if (selectedLog != null) {
        val log = selectedLog!!
        BackHandler { selectedLog = null }

        Scaffold(
                topBar = {
                    TopAppBar(
                            title = {
                                Text(
                                        text =
                                                SimpleDateFormat(
                                                                "yyyy-MM-dd HH:mm:ss",
                                                                Locale.getDefault()
                                                        )
                                                        .format(Date(log.timestamp))
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { selectedLog = null }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                    )
                                }
                            }
                    )
                }
        ) { innerPadding ->
            SelectionContainer {
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = PaddingValues(16.dp)
                ) {
                    if (log.aiConditionMet != null) {
                        item {
                            Text(
                                    text =
                                            if (log.aiConditionMet) "AI Condition: MET"
                                            else "AI Condition: NOT MET",
                                    color =
                                            if (log.aiConditionMet)
                                                    MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    item {
                        MarkdownText(markdown = log.resultText, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    } else {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text(searchTitle.ifBlank { "Search History" }) },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.runSearchNow(searchId) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Now")
                                }
                            }
                    )
                }
        ) { innerPadding ->
            if (logs.isEmpty()) {
                Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                ) { Text("No history yet.") }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { items(logs) { log -> SearchLogItem(log, onClick = { selectedLog = log }) } }
            }
        }
    }
}

@Composable
fun SearchLogItem(log: SearchLog, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelMedium
                )
                if (log.aiConditionMet != null) {
                    Text(
                            text = if (log.aiConditionMet) "AI Met" else "AI Not Met",
                            color =
                                    if (log.aiConditionMet) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text =
                            log.resultText.take(200) +
                                    if (log.resultText.length > 200) "..." else "",
                    style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

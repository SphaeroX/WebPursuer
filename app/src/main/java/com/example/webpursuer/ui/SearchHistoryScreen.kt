package com.murmli.webpursuer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(searchTitle.ifBlank { "Search History" }) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            ) { items(logs) { log -> SearchLogItem(log) } }
        }
    }
}

@Composable
fun SearchLogItem(log: SearchLog) {
    Card(modifier = Modifier.fillMaxWidth()) {
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

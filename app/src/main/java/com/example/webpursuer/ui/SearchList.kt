package com.murmli.webpursuer.ui

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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.murmli.webpursuer.data.Search
import java.util.Locale

@Composable
fun SearchList(
        viewModel: SearchViewModel,
        onEditClick: (Search) -> Unit,
        onHistoryClick: (Search) -> Unit,
        innerPadding: PaddingValues
) {
    val searches by viewModel.searches.collectAsState()

    if (searches.isEmpty()) {
        Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) { Text("No searches yet. Add one!") }
    } else {
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searches) { search ->
                SearchItem(
                        search = search,
                        onEditClick = { onEditClick(search) },
                        onHistoryClick = { onHistoryClick(search) },
                        onDeleteClick = { viewModel.deleteSearch(search) },
                        onRunClick = { viewModel.runSearchNow(search.id) },
                        onToggle = { enabled ->
                            viewModel.updateSearch(search.copy(enabled = enabled))
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchItem(
        search: Search,
        onEditClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onDeleteClick: () -> Unit,
        onRunClick: () -> Unit,
        onToggle: (Boolean) -> Unit
) {
    Card(onClick = onEditClick, modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text =
                                search.title.ifBlank { search.prompt }.take(50) +
                                        if (search.title.isBlank() && search.prompt.length > 50)
                                                "..."
                                        else "",
                        style = MaterialTheme.typography.titleMedium
                )

                val scheduleText =
                        if (search.scheduleType == "INTERVAL") {
                            if (search.intervalMinutes >= 60) {
                                "Every ${search.intervalMinutes/60} hours"
                            } else {
                                "Every ${search.intervalMinutes} minutes"
                            }
                        } else {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val activeDays =
                                    days.filterIndexed { index, _ ->
                                        (search.scheduleDays and (1 shl index)) != 0
                                    }
                            val daysText =
                                    if (activeDays.size == 7) "Daily"
                                    else activeDays.joinToString(", ")
                            "$daysText at ${String.format(Locale.getDefault(), "%02d:%02d", search.scheduleHour, search.scheduleMinute)}"
                        }

                Text(text = scheduleText, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Remove Play Button from here as requested
                Switch(checked = search.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onHistoryClick) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

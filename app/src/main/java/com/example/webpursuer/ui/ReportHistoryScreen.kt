package com.example.webpursuer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.webpursuer.data.GeneratedReport
import com.example.webpursuer.data.GeneratedReportRepository
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class ReportHistoryViewModel(
    private val repository: GeneratedReportRepository,
    private val reportId: Int
) : ViewModel() {
    val reports = repository.getReportsFor(reportId)
        .map { list -> list.sortedByDescending { it.timestamp } }
}

class ReportHistoryViewModelFactory(
    private val repository: GeneratedReportRepository,
    private val reportId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportHistoryViewModel(repository, reportId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHistoryScreen(
    reportId: Int,
    repository: GeneratedReportRepository,
    onNavigateBack: () -> Unit,
    onViewReport: (Int) -> Unit
) {
    val viewModel: ReportHistoryViewModel = viewModel(
        factory = ReportHistoryViewModelFactory(repository, reportId)
    )
    val reports by viewModel.reports.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No reports generated yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { report ->
                    GeneratedReportItem(report = report, onClick = { onViewReport(report.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedReportItem(
    report: GeneratedReport,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(report.timestamp)),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.summary, // Summary is already truncated/prepared
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
    }
}

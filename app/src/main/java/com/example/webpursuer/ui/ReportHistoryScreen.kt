package com.murmli.webpursuer.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.murmli.webpursuer.data.GeneratedReport
import com.murmli.webpursuer.data.GeneratedReportRepository
import com.murmli.webpursuer.worker.ReportWorker
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReportHistoryViewModel(
        application: Application,
        private val repository: GeneratedReportRepository,
        private val reportId: Int
) : AndroidViewModel(application) {

    val reports =
            repository.getReportsFor(reportId).map { list ->
                list.sortedByDescending { it.timestamp }
            }

    fun generateReportNow() {
        val workManager = WorkManager.getInstance(getApplication())
        val request =
                OneTimeWorkRequestBuilder<ReportWorker>()
                        .setInputData(workDataOf("report_id" to reportId))
                        .build()
        workManager.enqueue(request)
    }
}

class ReportHistoryViewModelFactory(
        private val application: Application,
        private val repository: GeneratedReportRepository,
        private val reportId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportHistoryViewModel(application, repository, reportId) as T
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
    val context = LocalContext.current.applicationContext as Application
    val viewModel: ReportHistoryViewModel =
            viewModel(factory = ReportHistoryViewModelFactory(context, repository, reportId))
    val reports by viewModel.reports.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Report History") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                    onClick = {
                                        viewModel.generateReportNow()
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                    "Report generation started"
                                            )
                                        }
                                    }
                            ) { Icon(Icons.Default.PlayArrow, contentDescription = "Run Now") }
                        }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (reports.isEmpty()) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
            ) { Text("No reports generated yet.") }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
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
fun GeneratedReportItem(report: GeneratedReport, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                    text =
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .format(Date(report.timestamp)),
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

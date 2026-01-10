package com.murmli.webpursuer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmli.webpursuer.data.GeneratedReport
import com.murmli.webpursuer.data.GeneratedReportRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContentScreen(
        generatedReportId: Int,
        repository: GeneratedReportRepository,
        onNavigateBack: () -> Unit
) {
    var report by remember { mutableStateOf<GeneratedReport?>(null) }
    var showDebugDialog by remember { mutableStateOf(false) }

    LaunchedEffect(generatedReportId) { report = repository.getReport(generatedReportId) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Report Content") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            var showMenu by remember { mutableStateOf(false) }
                            val context = androidx.compose.ui.platform.LocalContext.current

                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }

                            DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                            ) {
                                if (report?.debugPrompt != null) {
                                    DropdownMenuItem(
                                            text = { Text("Debug Info") },
                                            onClick = {
                                                showMenu = false
                                                showDebugDialog = true
                                            }
                                    )
                                }
                                DropdownMenuItem(
                                        text = { Text("PDF Export") },
                                        onClick = {
                                            showMenu = false
                                            report?.let {
                                                com.murmli.webpursuer.utils.PdfExportManager
                                                        .exportReportAsPdf(context, it)
                                            }
                                        }
                                )
                            }
                        }
                )
            }
    ) { innerPadding ->
        val currentReport = report
        if (currentReport == null) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            SelectionContainer {
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        MarkdownText(
                                markdown = currentReport.content,
                                modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showDebugDialog && report?.debugPrompt != null) {
        AlertDialog(
                onDismissRequest = { showDebugDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDebugDialog = false }) { Text("Close") }
                },
                title = { Text("Debug Prompt") },
                text = {
                    SelectionContainer {
                        LazyColumn {
                            item {
                                Text(
                                        text = report!!.debugPrompt!!,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
        )
    }
}

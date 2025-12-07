package com.example.webpursuer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.webpursuer.data.GeneratedReport
import com.example.webpursuer.data.GeneratedReportRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContentScreen(
    generatedReportId: Int,
    repository: GeneratedReportRepository,
    onNavigateBack: () -> Unit
) {
    var report by remember { mutableStateOf<GeneratedReport?>(null) }
    
    LaunchedEffect(generatedReportId) {
        report = repository.getReport(generatedReportId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Content") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val currentReport = report
        if (currentReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        // Simple text rendering for now, maybe replaced by Markdown renderer if needed
                        // Ideally we would parse the markdown here. 
                        // For MVP, just displaying the text with proper whitespace handling.
                        Text(
                            text = currentReport.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

package com.example.webpursuer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.webpursuer.data.CheckLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffScreen(
    checkLogId: Int,
    monitorId: Int, // Can be used to fetch monitor name if needed
    viewModel: MonitorViewModel,
    onBackClick: () -> Unit
) {
    var newLog by remember { mutableStateOf<CheckLog?>(null) }
    var oldLog by remember { mutableStateOf<CheckLog?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(checkLogId) {
        val log = viewModel.getCheckLog(checkLogId)
        newLog = log
        if (log != null) {
            oldLog = viewModel.getPreviousCheckLog(log.monitorId, log.timestamp)
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Änderungen")
                        // Could add Monitor Name here if fetched
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black // Dark background as per screenshot
    ) { innerPadding ->
        if (loading) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text("Laden...", color = Color.White)
            }
        } else if (newLog == null) {
             Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                 horizontalAlignment = Alignment.CenterHorizontally,
                 verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text("Log nicht gefunden.", color = Color.White)
            }
        } else {
            Column(modifier = Modifier.padding(innerPadding)) {
                // Version Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Vorherige", color = Color(0xFFE57373)) // Red-ish
                        oldLog?.let {
                            Text(
                                SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault()).format(Date(it.timestamp)),
                                color = Color(0xFFE57373),
                                fontSize = 12.sp
                            )
                        } ?: Text("-", color = Color(0xFFE57373))
                    }
                    
                    Text("->", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Aktuell", color = Color(0xFF81C784)) // Green-ish
                        Text(
                            SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault()).format(Date(newLog!!.timestamp)),
                            color = Color(0xFF81C784),
                            fontSize = 12.sp
                        )
                    }
                }

                // Diff Content
                val oldText = oldLog?.content ?: ""
                val newText = newLog?.content ?: ""
                val diffLines = GenerateDiff(oldText, newText)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(diffLines) { line ->
                        Text(
                            text = line.text,
                            color = line.color,
                            textDecoration = line.decoration,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

data class DiffLine(
    val text: String,
    val color: Color,
    val decoration: TextDecoration? = null
)

fun GenerateDiff(oldText: String, newText: String): List<DiffLine> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    val result = mutableListOf<DiffLine>()

    // Very simple differ: Compare lines. 
    // Ideally use a proper diff algorithm (Myers), but for now efficient enough for small texts
    // Using a simplistic approach: 
    // If lines match, white.
    // If old has line not in new -> Removed (Red)
    // If new has line not in old -> Added (Green)
    // This simple approach fails for shifts. 
    // Let's use a slightly better heuristics: LCS is too complex to implement from scratch reliably in one go.
    // Let's stick to a simple localized lookahead or just standard distinct lines if easy.
    // Actually, for this specific user request ("like Web Alert"), a simple "What is added, What is removed" block is often used.
    
    // Better Approach for this context:
    // Just show the new text, but highlight added parts in Green. 
    // And show removed parts in Red (maybe interleaved?).
    
    // Let's implement a basic LCS-based diff for lines.
    val dp = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }

    for (i in 1..oldLines.size) {
        for (j in 1..newLines.size) {
            if (oldLines[i - 1] == newLines[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    var i = oldLines.size
    var j = newLines.size
    val tempResult = mutableListOf<DiffLine>()

    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
            tempResult.add(DiffLine(oldLines[i - 1], Color.White))
            i--
            j--
        } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            tempResult.add(DiffLine(newLines[j - 1], Color(0xFF69F0AE), TextDecoration.Underline)) // Added
            j--
        } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
            tempResult.add(DiffLine(oldLines[i - 1], Color(0xFFFF5252), TextDecoration.LineThrough)) // Removed
            i--
        }
    }

    return tempResult.reversed()
}

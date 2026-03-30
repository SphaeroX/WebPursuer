package com.murmli.webpursuer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmli.webpursuer.data.Monitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffScreen(
        checkLogId: Int,
        monitorId: Int,
        viewModel: MonitorViewModel,
        onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val diffFilterMode by viewModel.diffFilterMode.collectAsState(initial = "ALL")
    val savedDiffViewMode by viewModel.diffViewMode.collectAsState(initial = "DIFF")

    // Navigation filter state
    var showOnlyOverThresholdNav by remember { mutableStateOf(false) }

    // State for navigation
    val logs by (if (showOnlyOverThresholdNav) 
        viewModel.getLogsForMonitorOverThreshold(monitorId) 
    else viewModel.getLogsForMonitorFiltered(monitorId))
        .collectAsState(initial = emptyList())
    var monitor by remember { mutableStateOf<Monitor?>(null) }

    // Current Index in the logs list (0 = Newest)
    // Initialize with -1 until logs are loaded
    var currentIndex by remember { mutableIntStateOf(-1) }

    val currentLog = if (currentIndex in logs.indices) logs[currentIndex] else null
    val previousLog = if (currentIndex + 1 in logs.indices) logs[currentIndex + 1] else null

    var showRendered by remember { mutableStateOf(false) }
    var viewModeInitialized by remember { mutableStateOf(false) }

    // Initialize currentIndex based on checkLogId when logs are first loaded
    LaunchedEffect(logs, checkLogId) {
        if (logs.isNotEmpty() && currentIndex == -1) {
            val idx = logs.indexOfFirst { it.id == checkLogId }
            currentIndex = if (idx != -1) idx else 0
        }
        if (monitor == null) {
            monitor = viewModel.getMonitor(monitorId)
        }
    }

    // Initialize showRendered from settings or heuristic
    LaunchedEffect(monitor, currentLog, savedDiffViewMode) {
        if (!viewModeInitialized && monitor != null) {
            // Check if we have a saved preference
            if (savedDiffViewMode == "RENDERED") {
                showRendered = true
            } else if (savedDiffViewMode == "DIFF") {
                showRendered = false
            } else {
                // Heuristic if no preference yet
                showRendered = monitor?.useAiInterpreter == true || isLikelyMarkdown(currentLog?.content)
            }
            viewModeInitialized = true
        }
    }

    var showFilterMenu by remember { mutableStateOf(false) }
    var showVersionSelectionDialog by remember { mutableStateOf(false) }
    var showRaw by remember { mutableStateOf(false) }

    // Helper to change version
    fun goToNewer() {
        if (currentIndex > 0) currentIndex--
    }

    fun goToOlder() {
        if (currentIndex < logs.size - 1) currentIndex++
    }

    // Swipe detection state
    var swipeOffsetX by remember { mutableStateOf(0f) }

    if (showVersionSelectionDialog && logs.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
                onDismissRequest = { showVersionSelectionDialog = false },
                title = { Text("Select Version") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(logs.size) { index ->
                            val log = logs[index]
                            val vNum = logs.size - index
                            val dateStr =
                                    SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault())
                                            .format(Date(log.timestamp))
                            val isSelected = index == currentIndex

                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .clickable {
                                                        currentIndex = index
                                                        showVersionSelectionDialog = false
                                                    }
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 8.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(32.dp))
                                }
                                Column {
                                    Text(
                                            "Version $vNum",
                                            fontWeight =
                                                    androidx.compose.ui.text.font.FontWeight.Bold,
                                            fontSize = 16.sp
                                    )
                                    Text(dateStr, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            androidx.compose.material3.HorizontalDivider()
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                            onClick = { showVersionSelectionDialog = false }
                    ) { Text("Cancel") }
                }
        )
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Column { Text("Changes") } },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Toggle Rendered/Diff view - ALWAYS available now
                            androidx.compose.material3.TextButton(onClick = { 
                                showRendered = !showRendered 
                                viewModel.setDiffViewMode(if (showRendered) "RENDERED" else "DIFF")
                            }) {
                                Text(
                                        text = if (showRendered) "Show Diff" else "Show MD",
                                        color = Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }

                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                        Icons.Default.List,
                                        contentDescription = "Filter",
                                        tint = Color.White
                                )
                            }
                            DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                        text = { Text("Select Version...") },
                                        onClick = {
                                            showVersionSelectionDialog = true
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.DateRange, contentDescription = null)
                                        }
                                )
                                DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (showOnlyOverThresholdNav) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(26.dp))
                                                }
                                                Text("Navigation: Over Threshold Only")
                                            }
                                        },
                                        onClick = {
                                            showOnlyOverThresholdNav = !showOnlyOverThresholdNav
                                            showFilterMenu = false
                                        }
                                )
                                androidx.compose.material3.HorizontalDivider()
                                
                                val filterModes = listOf(
                                    "CHANGES" to "All Changes",
                                    "ALL" to "Full Content",
                                    "NEW" to "New (Green)",
                                    "REMOVED" to "Removed (Red)",
                                    "UNCHANGED" to "Unchanged"
                                )
                                
                                filterModes.forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (diffFilterMode == mode) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(26.dp))
                                                }
                                                Text(label)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setDiffFilterMode(mode)
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }

                            if (monitor != null) {
                                IconButton(
                                        onClick = {
                                            val intent =
                                                    Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(monitor!!.url)
                                                    )
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Open in Browser",
                                            tint = Color.White
                                    )
                                }
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Black,
                                        titleContentColor = Color.White,
                                        navigationIconContentColor = Color.White,
                                        actionIconContentColor = Color.White
                                )
                )
            },
            containerColor = Color.Black
    ) { innerPadding ->
        Column(
                modifier =
                        Modifier.padding(innerPadding).fillMaxSize().pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (swipeOffsetX > 100) {
                                            // Swipe Right -> Go to Older
                                            goToOlder()
                                        } else if (swipeOffsetX < -100) {
                                            // Swipe Left -> Go to Newer
                                            goToNewer()
                                        }
                                        swipeOffsetX = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        swipeOffsetX += dragAmount
                                    }
                            )
                        }
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading history...", color = Color.White)
                }
            } else if (currentLog == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Logs not found.", color = Color.White)
                }
            } else {
                // Version Header / Navigation Controls
                Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement =
                                androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous (Older) Button
                    Button(
                            onClick = { goToOlder() },
                            enabled = currentIndex < logs.size - 1,
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = Color(0xFFE57373)
                                    )
                    ) { Text("< Older") }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Date Display
                        Text(
                                SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault())
                                        .format(Date(currentLog.timestamp)),
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    // Next (Newer) Button
                    Button(
                            onClick = { goToNewer() },
                            enabled = currentIndex > 0,
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = Color(0xFF81C784)
                                    )
                    ) { Text("Newer >") }
                }

                if (!showRendered) {
                    // Standard Diff View Header
                    // Compare Info
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement =
                                    androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Compare with:", color = Color.Gray, fontSize = 12.sp)
                            previousLog?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault())
                                            .format(Date(it.timestamp)),
                                        color = Color(0xFFE57373),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${String.format("%.1f", currentLog.changePercentage)}% change)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                                    ?: Text(
                                            "None (Initial)",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                    )
                        }
                    }

                    val hasRawContent = currentLog.rawContent != null
                    if (hasRawContent) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    "Show Raw Content",
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.Switch(
                                    checked = showRaw,
                                    onCheckedChange = { showRaw = it }
                            )
                        }
                    }
                }

                val oldText =
                        if (showRaw) (previousLog?.rawContent ?: previousLog?.content ?: "")
                        else (previousLog?.content ?: "")
                val newText =
                        if (showRaw) (currentLog.rawContent ?: "") else (currentLog.content ?: "")

                if (showRendered) {
                    // Markdown Rendered Document View
                    // Use SelectionContainer to allow copying text
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        MarkdownDocumentView(
                            markdown = newText,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Diff View
                    val allDiffLines = GenerateDiff(oldText, newText)

                    val filteredLines =
                            when (diffFilterMode) {
                                "CHANGES" -> allDiffLines.filter { it.color != Color.White }
                                "NEW" -> allDiffLines.filter { it.color == Color(0xFF69F0AE) }
                                "REMOVED" -> allDiffLines.filter { it.color == Color(0xFFFF5252) }
                                "UNCHANGED" -> allDiffLines.filter { it.color == Color.White }
                                else -> allDiffLines
                            }

                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(filteredLines) { line ->
                            Text(
                                    text = parseMarkdownText(line.text),
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
}

data class DiffLine(val text: String, val color: Color, val decoration: TextDecoration? = null)

fun GenerateDiff(oldText: String, newText: String): List<DiffLine> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    val result = mutableListOf<DiffLine>()

    // LCS-based diff for lines.
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
            tempResult.add(
                    DiffLine(newLines[j - 1], Color(0xFF69F0AE), TextDecoration.Underline)
            ) // Added
            j--
        } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
            tempResult.add(
                    DiffLine(oldLines[i - 1], Color(0xFFFF5252), TextDecoration.LineThrough)
            ) // Removed
            i--
        }
    }

    return tempResult.reversed()
}

fun isLikelyMarkdown(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    // Simple heuristics for Markdown
    val markdownIndicators =
            listOf(
                    "## ",
                    "### ",
                    "#### ", // Headers
                    "**",
                    "__", // Bold
                    "* ",
                    "- ", // Lists
                    "[",
                    "](", // Links
                    "```",
                    "`" // Code
            )
    // Check if any indicator is present
    return markdownIndicators.any { text.contains(it) }
}

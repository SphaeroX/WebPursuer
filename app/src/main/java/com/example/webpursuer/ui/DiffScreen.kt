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

    // State for navigation
    val logs by viewModel.getLogsForMonitorFiltered(monitorId).collectAsState(initial = emptyList())
    var monitor by remember { mutableStateOf<Monitor?>(null) }

    // Current Index in the logs list (0 = Newest)
    // Initialize with -1 until logs are loaded
    var currentIndex by remember { mutableIntStateOf(-1) }

    val currentLog = if (currentIndex in logs.indices) logs[currentIndex] else null
    val previousLog = if (currentIndex + 1 in logs.indices) logs[currentIndex + 1] else null

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

    var showFilterMenu by remember { mutableStateOf(false) }
    var showVersionSelectionDialog by remember { mutableStateOf(false) }
    var showRaw by remember { mutableStateOf(false) }

    // Default to Rendered view if AI Interpreter is used OR content looks like Markdown
    // We use a derived state based on monitor loaded.
    var showRendered by
            remember(monitor, currentLog) {
                mutableStateOf(
                        monitor?.useAiInterpreter == true || isLikelyMarkdown(currentLog?.content)
                )
            }

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
                title = { Text("Version auswählen") },
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
                    ) { Text("Abbrechen") }
                }
        )
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Column { Text("Änderungen") } },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Toggle Rendered/Diff view - ALWAYS available now
                            IconButton(onClick = { showRendered = !showRendered }) {
                                Text(
                                        text = if (showRendered) "Show Diff" else "Show MD",
                                        color = Color.White,
                                        fontSize = 12.sp,
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
                                        text = { Text("Version auswählen...") },
                                        onClick = {
                                            showVersionSelectionDialog = true
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.DateRange, contentDescription = null)
                                        }
                                )
                                androidx.compose.material3.HorizontalDivider()
                                DropdownMenuItem(
                                        text = { Text("Alle anzeigen") },
                                        onClick = {
                                            viewModel.setDiffFilterMode("ALL")
                                            showFilterMenu = false
                                        }
                                )
                                DropdownMenuItem(
                                        text = { Text("Nur Neue (Grün)") },
                                        onClick = {
                                            viewModel.setDiffFilterMode("NEW")
                                            showFilterMenu = false
                                        }
                                )
                                DropdownMenuItem(
                                        text = { Text("Nur Entfernte (Rot)") },
                                        onClick = {
                                            viewModel.setDiffFilterMode("REMOVED")
                                            showFilterMenu = false
                                        }
                                )
                                DropdownMenuItem(
                                        text = { Text("Nur Unveränderte") },
                                        onClick = {
                                            viewModel.setDiffFilterMode("UNCHANGED")
                                            showFilterMenu = false
                                        }
                                )
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
                    Text("Lade Historie...", color = Color.White)
                }
            } else if (currentLog == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Logs nicht gefunden.", color = Color.White)
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
                    ) { Text("< Älter") }

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
                    ) { Text("Neuer >") }
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
                            Text("Vergleich mit:", color = Color.Gray, fontSize = 12.sp)
                            previousLog?.let {
                                Text(
                                        SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault())
                                                .format(Date(it.timestamp)),
                                        color = Color(0xFFE57373),
                                        fontSize = 12.sp
                                )
                            }
                                    ?: Text(
                                            "Nichts (Initial)",
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
                    // Markdown Rendered View
                    // Use SelectionContainer to allow copying text
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        // Wrap in LazyColumn for scrolling
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            item {
                                // We utilize the existing MarkdownText composable
                                // Need to ensure color is visible against black background.
                                // MarkdownText likely uses onSurface which might be black on light
                                // theme,
                                // but here container is Black.
                                // Let's wrap in a Surface to ensure theme colors are consistent if
                                // needed,
                                // or force content color.
                                // Since DiffScreen enforces dark mode (Black container),
                                // we should check if MarkdownText is adaptive.
                                // MarkdownText uses MaterialTheme.colorScheme.onSurface.
                                // If we want it to look good on black, we should provide a dark
                                // surface or override styles.
                                // Easiest is to wrap this part in a surface of dark color (or reuse
                                // scaffold background)
                                // and ensure theme is dark or just put it in a box with white text.
                                // But MarkdownText internally sets color.
                                // Let's assume MaterialTheme handles it or we override
                                // contentColor.

                                // Override LocalContentColor to White for this section if needed,
                                // but MarkdownText sets its own color.
                                // Let's rely on standard Theme.
                                Surface(color = Color.Black, contentColor = Color.White) {
                                    MarkdownText(
                                            markdown = newText,
                                            modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Diff View
                    val allDiffLines = GenerateDiff(oldText, newText)

                    val filteredLines =
                            when (diffFilterMode) {
                                "NEW" -> allDiffLines.filter { it.color == Color(0xFF69F0AE) }
                                "REMOVED" -> allDiffLines.filter { it.color == Color(0xFFFF5252) }
                                "UNCHANGED" -> allDiffLines.filter { it.color == Color.White }
                                else -> allDiffLines
                            }

                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(filteredLines) { line ->
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
}

data class DiffLine(val text: String, val color: Color, val decoration: TextDecoration? = null)

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
    // Let's use a slightly better heuristics: LCS is too complex to implement from scratch reliably
    // in one go.
    // Let's stick to a simple localized lookahead or just standard distinct lines if easy.
    // Actually, for this specific user request ("like Web Alert"), a simple "What is added, What is
    // removed" block is often used.

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

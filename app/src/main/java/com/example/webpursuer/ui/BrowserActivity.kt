package com.murmli.webpursuer.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.murmli.webpursuer.data.Interaction
import com.murmli.webpursuer.data.Monitor
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserActivity : ComponentActivity() {

    private lateinit var webView: WebView

    private val browserViewModel: BrowserViewModel by viewModels()
    private val monitorViewModel: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { BrowserScreen() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BrowserScreen() {
        var displayUrl by remember {
            mutableStateOf("")
        }
        var actualUrl by remember {
            mutableStateOf(
                if (intent.hasExtra("monitorId")) {
                    intent.getStringExtra("initialUrl") ?: "https://www.google.com"
                } else {
                    "https://www.google.com"
                }
            )
        }
        var isUrlFieldFocused by remember { mutableStateOf(false) }
        val isSelecting by browserViewModel.isSelecting.collectAsState()
        val currentSelector by browserViewModel.currentSelector.collectAsState()
        var monitorName by remember { mutableStateOf("") }

        val selectedSelector by browserViewModel.selectedSelector.collectAsState()
        var showSaveDialog by remember { mutableStateOf(false) }

        // Test Dialog State
        var showTestDialog by remember { mutableStateOf(false) }
        var testContent by remember { mutableStateOf("") }

        val scope = rememberCoroutineScope()

        // Replay State
        var isReplaying by remember { mutableStateOf(false) }
        var replayStatus by remember { mutableStateOf("Initializing...") }
        var checkId by remember { mutableStateOf(-1) }
        var isCancelled by remember { mutableStateOf(false) }

        // Load Monitor Data if editing
        LaunchedEffect(Unit) {
            val id = intent.getIntExtra("monitorId", -1)
            if (id != -1) {
                checkId = id
                isReplaying = true
                replayStatus = "Loading monitor data..."
                val monitor = monitorViewModel.getMonitor(id)
                if (monitor != null) {
                    monitorName = monitor.name
                    actualUrl = monitor.url
                    val interactions = monitorViewModel.getInteractions(id)

                    // Wait for WebView to be ready and page to load
                    replayStatus = "Loading page..."
                    while (!::webView.isInitialized) {
                        kotlinx.coroutines.delay(100)
                    }
                    webView.loadUrl(actualUrl)

                    // Wait for page load (simple delay for now, can be improved with onPageFinished
                    // hook but handling state across composables is tricky)
                    // Better interaction: leverage onPageFinished via a shared state or just wait
                    for (i in 0..50) {
                        if (isCancelled) break
                        kotlinx.coroutines.delay(100)
                    }

                    replayStatus = "Replaying interactions..."
                    for ((index, interaction) in interactions.withIndex()) {
                        if (isCancelled) {
                            replayStatus = "Abgebrochen"
                            break
                        }
                        
                        val actionName = when (interaction.type) {
                            "click" -> "Klicken"
                            "input" -> "Eingeben"
                            "scroll" -> "Scrollen"
                            "wait" -> "Warten"
                            else -> interaction.type.replaceFirstChar { it.uppercase() }
                        }
                        replayStatus = "Aktion ${index + 1}/${interactions.size}: $actionName..."
                        
                        var js = ""
                        var waitTime = 500L // Default small delay for stability between actions

                        val escapedSelector = interaction.selector.replace("'", "\\'")
                        val findElJs = """
                            function findEl(sel) {
                                if (sel.startsWith("xpath=")) {
                                    return document.evaluate(sel.substring(6), document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                                }
                                if (sel.startsWith("text=")) {
                                    return document.evaluate("//*[normalize-space()='" + sel.substring(5).replace(/'/g, "\\'") + "']", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                                }
                                try { var el = document.querySelector(sel); if (el) return el; } catch(e) {}
                                return null;
                            }
                        """.trimIndent()

                        when (interaction.type) {
                            "click" -> {
                                js = "$findElJs; var el = findEl('$escapedSelector'); if(el) el.click();"
                            }
                            "input" -> {
                                val escapedValue = interaction.value?.replace("'", "\\'")?.replace("\n", "\\n") ?: ""
                                js = "$findElJs; var el = findEl('$escapedSelector'); if(el) { el.value = '$escapedValue'; el.dispatchEvent(new Event('input', {bubbles: true})); el.dispatchEvent(new Event('change', {bubbles: true})); }"
                            }
                            "scroll" -> {
                                js = "window.scrollTo({top: ${interaction.value ?: "0"}, behavior: 'smooth'});"
                                waitTime = 1000L // Extra time for smooth scroll animation
                            }
                            "wait" -> {
                                waitTime = interaction.value?.toLongOrNull() ?: 500L
                                // Optionally cap to a reasonable max like 30s to prevent hanging too long if accidental
                                waitTime = waitTime.coerceAtMost(30000L)
                            }
                        }

                        if (js.isNotEmpty()) {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                webView.evaluateJavascript(js, null)
                            }
                        }
                        
                        if (waitTime > 0) {
                            // Check for cancellation during wait time in small chunks to be responsive
                            val chunks = (waitTime / 100).toInt()
                            for (i in 0..chunks) {
                                if (isCancelled) break
                                kotlinx.coroutines.delay(100)
                            }
                        }

                        // Wait for page to finish loading if interaction caused navigation
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            while (webView.progress < 100) {
                                if (isCancelled) break
                                kotlinx.coroutines.delay(100)
                            }
                        }
                    }

                    if (!isCancelled) {
                        replayStatus = "Restoring selection..."
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            browserViewModel.updateCurrentSelector(monitor.selector)
                            
                            val isRunMode = intent.getBooleanExtra("isRunMode", false)
                            if (!isRunMode) {
                                browserViewModel.setSelectionMode(true)
                            }
                            
                            // Try to highlight the existing selector
                            webView.evaluateJavascript(
                                    "window.highlightSelector('${monitor.selector}')",
                                    null
                            )
                            
                            if (isRunMode) {
                                android.widget.Toast.makeText(this@BrowserActivity, "Aufzeichnung beendet.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    isReplaying = false
                } else {
                    isReplaying = false
                }
            }
        }

        LaunchedEffect(selectedSelector) {
            if (selectedSelector != null) {
                showSaveDialog = true
            }
        }

        // Effect to handle selection mode changes
        LaunchedEffect(isSelecting) {
            if (::webView.isInitialized) {
                if (isSelecting) {
                    webView.evaluateJavascript("window.enableSelectionMode()", null)
                } else {
                    webView.evaluateJavascript("window.disableSelectionMode()", null)
                }
            }
        }

        if (showTestDialog) {
            AlertDialog(
                    onDismissRequest = { showTestDialog = false },
                    title = { Text("Content Preview") },
                    text = {
                        Box(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                            val scrollState = rememberScrollState()
                            Text(
                                    text = testContent,
                                    modifier = Modifier.verticalScroll(scrollState)
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showTestDialog = false }) { Text("Close") }
                    }
            )
        }

        if (showSaveDialog && selectedSelector != null) {
            AlertDialog(
                    onDismissRequest = {
                        showSaveDialog = false
                        browserViewModel.clearSelection()
                        browserViewModel.setSelectionMode(false)
                    },
                    title = { Text("Save Monitor") },
                    text = {
                        Column {
                            Text("Selected Element:")
                            Text(selectedSelector!!, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                    value = monitorName,
                                    onValueChange = { monitorName = it },
                                    label = { Text("Monitor Name") },
                                    modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                                onClick = {
                                    scope.launch {
                                        val id = intent.getIntExtra("monitorId", -1)
                                        if (id != -1) {
                                            // Update existing monitor
                                            val monitor = monitorViewModel.getMonitor(id)
                                            if (monitor != null) {
                                                val existingInteractions =
                                                        monitorViewModel.getInteractions(id)
                                                val newInteractions =
                                                        browserViewModel.getRecordedInteractions()
                                                                .mapIndexed { index, data ->
                                                                    Interaction(
                                                                            monitorId = id,
                                                                            type = data.type,
                                                                            selector =
                                                                                    data.selector,
                                                                            value = data.value,
                                                                            orderIndex =
                                                                                    existingInteractions
                                                                                            .size +
                                                                                            index
                                                                    )
                                                                }
                                                monitorViewModel.updateMonitor(
                                                        monitor.copy(
                                                                selector = selectedSelector!!,
                                                                url = actualUrl
                                                        )
                                                )
                                            }
                                        } else {
                                            // New Monitor
                                            val newMonitorId = monitorViewModel.addMonitor(
                                                    Monitor(
                                                            url = actualUrl,
                                                            name =
                                                                    monitorName.ifBlank {
                                                                        try {
                                                                            android.net.Uri.parse(actualUrl)
                                                                                    .host
                                                                                    ?: "Monitor"
                                                                        } catch (e: Exception) {
                                                                            "Monitor"
                                                                        }
                                                                    },
                                                            selector = selectedSelector!!
                                                    ),
                                                    browserViewModel.getRecordedInteractions()
                                                            .mapIndexed { index, data ->
                                                                Interaction(
                                                                        monitorId = 0,
                                                                        type = data.type,
                                                                        selector = data.selector,
                                                                        value = data.value,
                                                                        orderIndex = index
                                                                )
                                                            }
                                            )
                                            
                                            // Navigate back to MainActivity with the new monitor ID
                                            val intent = android.content.Intent(
                                                this@BrowserActivity,
                                                com.murmli.webpursuer.MainActivity::class.java
                                            ).apply {
                                                putExtra("monitorId", newMonitorId)
                                                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            startActivity(intent)
                                        }
                                        showSaveDialog = false
                                        browserViewModel.clearSelection()
                                        browserViewModel.setSelectionMode(false)
                                        browserViewModel.clearInteractions()
                                        finish()
                                    }
                                }
                        ) { Text("Save") }
                    },
                    dismissButton = {
                        Button(
                                onClick = {
                                    showSaveDialog = false
                                    browserViewModel.clearSelection()
                                    browserViewModel.setSelectionMode(false)
                                }
                        ) { Text("Cancel") }
                    }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                    topBar = {
                        Column {
                            // Custom Slimmer URL Bar
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .statusBarsPadding()
                                                    .height(56.dp)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }

                                TextField(
                                        value = if (isUrlFieldFocused) displayUrl else actualUrl,
                                        onValueChange = { displayUrl = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .onFocusChanged { focusState ->
                                                isUrlFieldFocused = focusState.isFocused
                                                if (focusState.isFocused) {
                                                    displayUrl = if (actualUrl == "https://www.google.com") "" else actualUrl
                                                }
                                            },
                                        singleLine = true,
                                        colors =
                                                TextFieldDefaults.colors(
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                        focusedContainerColor =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .surfaceVariant,
                                                        unfocusedContainerColor =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .surfaceVariant
                                                ),
                                        shape = MaterialTheme.shapes.small
                                )

                                IconButton(
                                        onClick = {
                                            browserViewModel.toggleSelectionMode()
                                            if (!isSelecting) injectRecorderScript()
                                        }
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "Select",
                                            tint =
                                                    if (isSelecting) Color(0xFF008f39)
                                                    else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                        onClick = {
                                            val inputUrl = if (isUrlFieldFocused) displayUrl else actualUrl
                                            val processedUrl = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                                                "https://$inputUrl"
                                            } else {
                                                inputUrl
                                            }
                                            actualUrl = processedUrl
                                            displayUrl = ""
                                            browserViewModel.clearInteractions()
                                            webView.loadUrl(processedUrl)
                                        }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Go")
                                }
                            }

                            if (isSelecting) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.8f))
                                                        .padding(8.dp)
                                ) {
                                    // Row 1: Selector
                                    TextField(
                                            value = currentSelector,
                                            onValueChange = { newSelector ->
                                                browserViewModel.updateCurrentSelector(newSelector)
                                                webView.evaluateJavascript(
                                                        "window.highlightSelector('$newSelector')",
                                                        null
                                                )
                                            },
                                            colors =
                                                    TextFieldDefaults.colors(
                                                            focusedContainerColor =
                                                                    Color.Transparent,
                                                            unfocusedContainerColor =
                                                                    Color.Transparent,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White,
                                                            cursorColor = Color.White,
                                                            focusedIndicatorColor = Color.White,
                                                            unfocusedIndicatorColor = Color.White
                                                    ),
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1,
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            placeholder = {
                                                Text("CSS Selector", color = Color.Gray)
                                            }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Row 2: Buttons
                                    Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row {
                                            // Plus Button (Expand/Parent)
                                            Button(
                                                    onClick = {
                                                        webView.evaluateJavascript(
                                                                "window.selectParent()",
                                                                null
                                                        )
                                                    },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(40.dp),
                                                    colors =
                                                            ButtonDefaults.buttonColors(
                                                                    containerColor = Color.DarkGray
                                                            )
                                            ) {
                                                Text(
                                                        "+",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Minus Button (Shrink/Child)
                                            Button(
                                                    onClick = {
                                                        webView.evaluateJavascript(
                                                                "window.selectChild()",
                                                                null
                                                        )
                                                    },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(40.dp),
                                                    colors =
                                                            ButtonDefaults.buttonColors(
                                                                    containerColor = Color.DarkGray
                                                            )
                                            ) {
                                                Text(
                                                        "-",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Test Button
                                            Button(
                                                    onClick = {
                                                        webView.evaluateJavascript(
                                                                "window.getTextContent()"
                                                        ) { value ->
                                                            val cleanValue =
                                                                    value?.let {
                                                                        if (it == "null") ""
                                                                        else if (it.startsWith(
                                                                                        "\""
                                                                                ) &&
                                                                                        it.endsWith(
                                                                                                "\""
                                                                                        )
                                                                        ) {
                                                                            it.substring(
                                                                                            1,
                                                                                            it.length -
                                                                                                    1
                                                                                    )
                                                                                    .replace(
                                                                                            "\\n",
                                                                                            "\n"
                                                                                    )
                                                                                    .replace(
                                                                                            "\\\"",
                                                                                            "\""
                                                                                    )
                                                                                    .replace(
                                                                                            "\\\\",
                                                                                            "\\"
                                                                                    )
                                                                        } else it
                                                                    }
                                                                            ?: ""

                                                            testContent = cleanValue
                                                            showTestDialog = true
                                                        }
                                                    },
                                                    contentPadding =
                                                            PaddingValues(horizontal = 8.dp),
                                                    modifier = Modifier.height(40.dp),
                                                    colors =
                                                            ButtonDefaults.buttonColors(
                                                                    containerColor = Color.Blue
                                                            )
                                            ) { Text("Test") }
                                        }

                                        // Confirm Button
                                        Button(
                                                onClick = {
                                                    if (currentSelector.isNotEmpty()) {
                                                        browserViewModel.onElementSelected(
                                                                currentSelector
                                                        )
                                                    }
                                                },
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF008f39)
                                                        ),
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                modifier = Modifier.height(40.dp)
                                        ) { Text("Auswählen") }
                                    }
                                }
                            }
                        }
                    }
            ) { innerPadding ->
                AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams =
                                        android.view.ViewGroup.LayoutParams(
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                isVerticalScrollBarEnabled = true

                                webViewClient =
                                        object : WebViewClient() {
                                            override fun onPageFinished(
                                                    view: WebView?,
                                                    url: String?
                                            ) {
                                                super.onPageFinished(view, url)
                                                injectRecorderScript()
                                                if (isSelecting) {
                                                    view?.evaluateJavascript(
                                                            "window.enableSelectionMode()",
                                                            null
                                                    )
                                                } else {
                                                    view?.evaluateJavascript(
                                                            "window.disableSelectionMode()",
                                                            null
                                                    )
                                                }
                                            }
                                        }
                                addJavascriptInterface(WebAppInterface(context), "Android")
                                if (intent.hasExtra("monitorId")) {
                                    loadUrl(actualUrl)
                                } else if (intent.hasExtra("initialUrl")) {
                                    loadUrl(intent.getStringExtra("initialUrl")!!)
                                }
                                webView = this
                            }
                        },
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
            }

            if (isReplaying) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .zIndex(2f),
                        contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { isCancelled = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = replayStatus,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                text = "Please wait while we navigate to the target page...",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    private fun injectRecorderScript() {
        if (!::webView.isInitialized) return
        try {
            val inputStream = assets.open("recorder.js")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val script = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                script.append(line)
                script.append("\n")
            }
            reader.close()

            webView.evaluateJavascript(script.toString(), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onInteractionRecorded(type: String, selector: String, value: String) {
        runOnUiThread { browserViewModel.recordInteraction(type, selector, value) }
    }

    fun onSelectorUpdated(selector: String) {
        runOnUiThread { browserViewModel.updateCurrentSelector(selector) }
    }

    fun onSelectionConfirmed(selector: String) {
        runOnUiThread { browserViewModel.onElementSelected(selector) }
    }

    fun onElementSelected(selector: String) {
        onSelectionConfirmed(selector)
    }
}

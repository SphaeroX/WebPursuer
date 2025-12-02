package com.example.webpursuer.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webpursuer.R
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.ui.unit.dp

import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.webpursuer.data.Monitor
import com.example.webpursuer.ui.MonitorViewModel

class BrowserActivity : ComponentActivity() {

    private lateinit var webView: WebView

    private val browserViewModel: BrowserViewModel by viewModels()
    private val monitorViewModel: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            BrowserScreen()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BrowserScreen() {
        var url by remember { mutableStateOf("https://google.com") }
        // isRecording removed
        val isSelecting by browserViewModel.isSelecting.collectAsState()
        var monitorName by remember { mutableStateOf("") }
        
        val selectedSelector by browserViewModel.selectedSelector.collectAsState()
        var showSaveDialog by remember { mutableStateOf(false) }

        LaunchedEffect(selectedSelector) {
            if (selectedSelector != null) {
                showSaveDialog = true
            }
        }

        // Effect to handle selection mode changes
        LaunchedEffect(isSelecting) {
            if (isSelecting) {
                webView.evaluateJavascript("window.enableSelectionMode()", null)
            } else {
                webView.evaluateJavascript("window.disableSelectionMode()", null)
            }
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
                            label = { Text("Monitor Name") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        // Save Monitor
                        monitorViewModel.addMonitor(
                            Monitor(
                                url = webView.url ?: url,
                                name = monitorName.ifBlank { "Monitor" },
                                selector = selectedSelector!!
                            ),
                            browserViewModel.getRecordedInteractions().mapIndexed { index, data ->
                                com.example.webpursuer.data.Interaction(
                                    monitorId = 0, // Will be set in ViewModel
                                    type = data.type,
                                    selector = data.selector,
                                    value = data.value,
                                    orderIndex = index
                                )
                            }
                        )
                        showSaveDialog = false
                        browserViewModel.clearSelection()
                        browserViewModel.setSelectionMode(false)
                        browserViewModel.clearInteractions()
                        finish() // Go back to Home
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = { 
                        showSaveDialog = false 
                        browserViewModel.clearSelection()
                        browserViewModel.setSelectionMode(false)
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        TextField(
                            value = url, 
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        ) 
                    },
                    actions = {
                        IconButton(onClick = { 
                            webView.loadUrl(url) 
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Go")
                        }
                        // Record button removed as per user request (Auto-recording)
                        
                        IconButton(onClick = { 
                            browserViewModel.toggleSelectionMode()
                            // Script injection happens in LaunchedEffect or onPageFinished
                            if (!isSelecting) injectRecorderScript() // Inject if we are STARTING selection (actually logic is a bit mixed, just ensure script is there)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check, 
                                contentDescription = "Select",
                                tint = if (isSelecting) androidx.compose.ui.graphics.Color.Green else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                injectRecorderScript() // Always inject to ensure functions are available
                                if (isSelecting) {
                                    view?.evaluateJavascript("window.enableSelectionMode()", null)
                                } else {
                                    view?.evaluateJavascript("window.disableSelectionMode()", null)
                                }
                            }
                        }
                        addJavascriptInterface(WebAppInterface(context), "Android")
                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        }
    }

    fun onElementSelected(selector: String) {
        browserViewModel.onElementSelected(selector)
    }
    
    fun onInteractionRecorded(type: String, target: String, value: String) {
        // Only record if NOT selecting
        // We need to access the state 'isSelecting' here, but it's inside Composable.
        // Ideally, WebAppInterface should talk to ViewModel directly or via a stable callback.
        // For now, we can check browserViewModel state if we moved isSelecting there, 
        // OR we can just rely on the fact that JS disables recording when in selection mode (if we implemented that in JS).
        // Let's check browserViewModel.selectedSelector.value to see if we are "done". 
        // Actually, the user requirement is: Record everything UNTIL the checkmark is clicked (isSelecting = true).
        // So we should pass 'isSelecting' state to WebAppInterface or handle it here.
        // Since 'isSelecting' is local state in Composable, it's hard to access here.
        // Let's move 'isSelecting' to BrowserViewModel to make it cleaner.
        browserViewModel.recordInteraction(type, target, value)
    }

    private fun injectRecorderScript() {
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
}

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
        var isRecording by remember { mutableStateOf(false) }
        var isSelecting by remember { mutableStateOf(false) }
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
                    isSelecting = false 
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
                            )
                        )
                        showSaveDialog = false
                        browserViewModel.clearSelection()
                        isSelecting = false
                        finish() // Go back to Home
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = { 
                        showSaveDialog = false 
                        browserViewModel.clearSelection()
                        isSelecting = false
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
                        IconButton(onClick = { 
                            isRecording = !isRecording
                            if (isRecording) {
                                injectRecorderScript()
                            }
                        }) {
                            Text(if (isRecording) "REC" else "START")
                        }
                        IconButton(onClick = { 
                            isSelecting = !isSelecting
                            // Script injection happens in LaunchedEffect or onPageFinished
                            if (isSelecting) injectRecorderScript() 
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check, 
                                contentDescription = "Select",
                                tint = if (isSelecting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

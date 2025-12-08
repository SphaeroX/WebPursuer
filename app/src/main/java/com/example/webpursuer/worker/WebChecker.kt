package com.example.webpursuer.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.webpursuer.data.CheckLog
import com.example.webpursuer.data.CheckLogDao
import com.example.webpursuer.data.Monitor
import com.example.webpursuer.data.MonitorDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.first

class WebChecker(
    private val context: Context,
    private val monitorDao: MonitorDao,
    private val checkLogDao: CheckLogDao,
    private val interactionDao: com.example.webpursuer.data.InteractionDao,
    private val openRouterService: com.example.webpursuer.network.OpenRouterService,
    private val settingsRepository: com.example.webpursuer.data.SettingsRepository
) {

    suspend fun checkMonitor(monitor: Monitor, now: Long) {
        try {
            val interactions = interactionDao.getInteractionsForMonitor(monitor.id)
            var content = ""
            var attempt = 0
            while (content.isBlank() && attempt < 3) {
                if (attempt > 0) {
                    android.util.Log.d("WebChecker", "Retry load content attempt ${attempt + 1} for ${monitor.name}")
                    kotlinx.coroutines.delay(2000)
                }
                content = loadContent(monitor.url, monitor.selector, interactions)
                attempt++
            }

            if (content.isBlank()) {
                val errorMsg = "Empty content loaded after $attempt attempts for monitor ${monitor.name} (${monitor.url})."
                android.util.Log.e("WebChecker", errorMsg)
                
                 checkLogDao.insert(
                    CheckLog(
                        monitorId = monitor.id,
                        timestamp = now,
                        result = "FAILURE",
                        message = errorMsg,
                        content = null
                    )
                )
                return 
            }

            val contentHash = hash(content)

            var result: String
            var message: String
            val newHash: String?
            var shouldNotify = false

            // Standard Change Detection
            if (monitor.lastContentHash == null) {
                result = "SUCCESS"
                message = "Initial check successful."
                newHash = contentHash
                // Usually don't notify on initial check unless requested, but sticking to existing logic which seemed to not notify? 
                // Existing logic: if lastContentHash != contentHash (which is true if null? No, explicit null check). 
                // So initial check -> no notification.
            } else if (monitor.lastContentHash != contentHash) {
                result = "CHANGED"
                message = "Content changed!"
                newHash = contentHash
                shouldNotify = true
                
                if (monitor.llmEnabled && !monitor.llmPrompt.isNullOrBlank()) {
                    val llmResult = openRouterService.checkContent(monitor.llmPrompt, content)
                    if (llmResult) {
                        message += " LLM Condition Met."
                        // Notify
                    } else {
                        message += " LLM Condition NOT Met."
                        shouldNotify = false // Don't notify if LLM condition fails
                    }
                } else {
                    message += " Content changed for ${monitor.name}"
                }
            } else {
                result = "UNCHANGED"
                message = "No changes detected."
                newHash = monitor.lastContentHash
            }

            // Update Monitor
            monitorDao.update(
                monitor.copy(
                    lastCheckTime = now,
                    lastContentHash = newHash
                )
            )

            // Log Result
            val logId = checkLogDao.insert(
                CheckLog(
                    monitorId = monitor.id,
                    timestamp = now,
                    result = result,
                    message = message,
                    content = content
                )
            )

            // Send Notification if needed
            if (result == "CHANGED" && shouldNotify) {
                sendNotification(monitor.id, logId.toInt(), "Monitor Update", message)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            checkLogDao.insert(
                CheckLog(
                    monitorId = monitor.id,
                    timestamp = now,
                    result = "FAILURE",
                    message = "Error: ${e.message}",
                    content = null
                )
            )
        }
    }

    private suspend fun loadContent(url: String, selector: String, interactions: List<com.example.webpursuer.data.Interaction>): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            webView.webViewClient = object : WebViewClient() {
                var pageLoaded = false

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (pageLoaded) return
                    pageLoaded = true

                    // Execute interactions sequentially
                    Handler(Looper.getMainLooper()).postDelayed({
                        executeInteractions(webView, interactions, 0) {
                            // After all interactions, extract content
                            extractContent(webView, selector) { text ->
                                if (continuation.isActive) {
                                    continuation.resume(text)
                                }
                            }
                        }
                    }, 2000) // Initial wait
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                     // Handle error
                }
            }
            
            webView.loadUrl(url)
        }
    }

    private fun executeInteractions(webView: WebView, interactions: List<com.example.webpursuer.data.Interaction>, index: Int, onComplete: () -> Unit) {
        if (index >= interactions.size) {
            onComplete()
            return
        }

        val interaction = interactions[index]
        val js = when (interaction.type) {
            "click" -> "document.querySelector('${interaction.selector}').click();"
            "input" -> "document.querySelector('${interaction.selector}').value = '${interaction.value}'; document.querySelector('${interaction.selector}').dispatchEvent(new Event('change'));"
            else -> ""
        }

        if (js.isNotEmpty()) {
            webView.evaluateJavascript(js) {
                // Wait a bit after interaction
                Handler(Looper.getMainLooper()).postDelayed({
                    executeInteractions(webView, interactions, index + 1, onComplete)
                }, 2000) // 2 seconds delay between actions
            }
        } else {
            executeInteractions(webView, interactions, index + 1, onComplete)
        }
    }

    private fun extractContent(webView: WebView, selector: String, callback: (String) -> Unit) {
        val js = """
            (function() {
                var element = document.querySelector('$selector');
                if (!element) return '';
                
                function getRecursiveText(node) {
                    if (node.nodeType === 3) { // Node.TEXT_NODE
                        return (node.nodeValue || "").trim();
                    }
                    if (node.nodeType !== 1) return ""; // Node.ELEMENT_NODE
                    
                    var tagName = node.tagName.toLowerCase();
                    // Skip scripts and styles
                    if (tagName === 'script' || tagName === 'style' || tagName === 'noscript') return "";
                    
                    var text = "";
                    var isBlock = false;
                    try {
                        var style = window.getComputedStyle(node);
                        // Skip hidden elements, but leniently
                        if (style.display === 'none' || style.visibility === 'hidden') return "";
                        isBlock = (style.display === 'block' || style.display === 'flex' || style.display === 'grid' || style.display === 'table-row');
                    } catch (e) {}

                    // Form elements
                    if (tagName === 'input') {
                        var type = node.type ? node.type.toLowerCase() : 'text';
                        if (type !== 'hidden' && type !== 'submit' && type !== 'button' && type !== 'image') {
                             return node.value || "";
                        }
                    }
                    if (tagName === 'textarea') {
                        return node.value || "";
                    }
                    if (tagName === 'select') {
                         if (node.selectedIndex >= 0) return node.options[node.selectedIndex].text;
                         return "";
                    }
                    if (tagName === 'br') return "\n";
                    
                    // Children
                    var childTexts = [];
                    for (var i = 0; i < node.childNodes.length; i++) {
                        var childVal = getRecursiveText(node.childNodes[i]);
                        if (childVal) childTexts.push(childVal);
                    }
                    
                    text = childTexts.join(isBlock ? "\n" : " ");
                    
                    if (isBlock) text = "\n" + text + "\n";
                    
                    return text;
                }

                // Clean up multiple newlines
                return getRecursiveText(element).replace(/\n\s*\n/g, '\n').trim();
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { result ->
            val text = if (result != null && result != "null") {
                 if (result.startsWith("\"") && result.endsWith("\"")) {
                    result.substring(1, result.length - 1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                 } else {
                     result
                 }
            } else {
                ""
            }
            callback(text)
        }
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun sendNotification(monitorId: Int, logId: Int, title: String, message: String) {
        // Check if notifications are enabled globally
        val isGloballyEnabled = settingsRepository.notificationsEnabled.first()
        if (!isGloballyEnabled) {
            return
        }

        // Check if notifications are enabled for this monitor
        val monitor = monitorDao.getById(monitorId)
        if (monitor != null && !monitor.notificationsEnabled) {
            return
        }

        val intent = android.content.Intent(context, com.example.webpursuer.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("monitorId", monitorId)
            putExtra("checkLogId", logId)
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            context, monitorId, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, "web_monitor_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a default icon for now
            .setContentTitle("Changes found") // Simplified title
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(androidx.core.app.NotificationManagerCompat.from(context)) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notify(monitorId, builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

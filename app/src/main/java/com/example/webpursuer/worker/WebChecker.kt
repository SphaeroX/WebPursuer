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
            val content = loadContent(monitor.url, monitor.selector, interactions)
            val contentHash = hash(content)

            var result: String
            var message: String
            val newHash: String?

            // Standard Change Detection
            if (monitor.lastContentHash == null) {
                result = "SUCCESS"
                message = "Initial check successful."
                newHash = contentHash
            } else if (monitor.lastContentHash != contentHash) {
                result = "CHANGED"
                message = "Content changed!"
                newHash = contentHash
                
                if (monitor.llmEnabled && !monitor.llmPrompt.isNullOrBlank()) {
                    val llmResult = openRouterService.checkContent(monitor.llmPrompt, content)
                    if (llmResult) {
                        sendNotification(monitor.id, "Smart Alert", "Condition met: ${monitor.llmPrompt}")
                        message += " LLM Condition Met."
                    } else {
                        message += " LLM Condition NOT Met."
                    }
                } else {
                    sendNotification(monitor.id, "Monitor Update", "Content changed for ${monitor.name}")
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
            checkLogDao.insert(
                CheckLog(
                    monitorId = monitor.id,
                    timestamp = now,
                    result = result,
                    message = message,
                    content = content
                )
            )

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
                return element ? element.innerText : '';
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js) { result ->
            val text = if (result != null && result != "null") {
                result.substring(1, result.length - 1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
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

    private suspend fun sendNotification(monitorId: Int, title: String, message: String) {
        // Check if notifications are enabled
        val isEnabled = settingsRepository.notificationsEnabled.first()
        if (!isEnabled) {
            return
        }

        val intent = android.content.Intent(context, com.example.webpursuer.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, "web_monitor_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a default icon for now
            .setContentTitle(title)
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

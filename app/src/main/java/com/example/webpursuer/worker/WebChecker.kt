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

class WebChecker(
    private val context: Context,
    private val monitorDao: MonitorDao,
    private val checkLogDao: CheckLogDao,
    private val interactionDao: com.example.webpursuer.data.InteractionDao
) {

    suspend fun checkMonitor(monitor: Monitor, now: Long) {
        try {
            val interactions = interactionDao.getInteractionsForMonitor(monitor.id)
            val content = loadContent(monitor.url, monitor.selector, interactions)
            val contentHash = hash(content)

            val result: String
            val message: String
            val newHash: String?

            if (monitor.lastContentHash == null) {
                // First check
                result = "SUCCESS"
                message = "Initial check successful."
                newHash = contentHash
            } else if (monitor.lastContentHash != contentHash) {
                result = "CHANGED"
                message = "Content changed!"
                newHash = contentHash
                // TODO: Trigger Notification here
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
}

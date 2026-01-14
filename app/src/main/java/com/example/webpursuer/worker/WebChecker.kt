package com.murmli.webpursuer.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.murmli.webpursuer.data.CheckLog
import com.murmli.webpursuer.data.CheckLogDao
import com.murmli.webpursuer.data.Monitor
import com.murmli.webpursuer.data.MonitorDao
import java.io.StringReader
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class WebChecker(
        private val context: Context,
        private val monitorDao: MonitorDao,
        private val checkLogDao: CheckLogDao,
        private val interactionDao: com.murmli.webpursuer.data.InteractionDao,
        private val openRouterService: com.murmli.webpursuer.network.OpenRouterService,
        private val settingsRepository: com.murmli.webpursuer.data.SettingsRepository,
        private val logRepository: com.murmli.webpursuer.data.LogRepository
) {

    suspend fun checkMonitor(monitor: Monitor, now: Long) {
        try {
            val interactions = interactionDao.getInteractionsForMonitor(monitor.id)
            var content = ""
            var attempt = 0
            while (content.isBlank() && attempt < 3) {
                if (attempt > 0) {
                    android.util.Log.d(
                            "WebChecker",
                            "Retry load content attempt ${attempt + 1} for ${monitor.name}"
                    )
                    kotlinx.coroutines.delay(2000)
                }
                content = loadContent(monitor.url, monitor.selector, interactions)
                attempt++
            }

            if (content.isBlank()) {
                val errorMsg =
                        "Empty content loaded after $attempt attempts for monitor ${monitor.name} (${monitor.url})."
                android.util.Log.e("WebChecker", errorMsg)
                logRepository.logError("MONITOR", errorMsg)

                checkLogDao.insert(
                        CheckLog(
                                monitorId = monitor.id,
                                timestamp = now,
                                result = "FAILURE",
                                message = errorMsg,
                                content = null,
                                rawContent = null
                        )
                )
                return
            }

            var rawContent: String? = null

            // RSS Handling
            if (isRssFeed(content)) {
                try {
                    logRepository.logInfo(
                            "MONITOR",
                            "RSS Feed detected for ${monitor.name}, parsing content..."
                    )
                    val parsed = parseRss(content)
                    if (parsed.isNotBlank()) {
                        rawContent = content // Save original XML as rawContent
                        content = parsed
                    }
                } catch (e: Exception) {
                    logRepository.logError(
                            "MONITOR",
                            "Failed to parse RSS for ${monitor.name}: ${e.message}"
                    )
                }
            }
            if (monitor.useAiInterpreter) {
                rawContent = content
                logRepository.logInfo(
                        "MONITOR",
                        "Interpreting content with AI for ${monitor.name}..."
                )
                content =
                        openRouterService.interpretContent(
                                monitor.aiInterpreterInstruction,
                                content,
                                monitor.useWebSearch
                        )
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
            } else if (monitor.lastContentHash != contentHash) {
                result = "CHANGED"
                message = "Content changed!"
                newHash = contentHash
                shouldNotify = true

                if (monitor.llmEnabled && !monitor.llmPrompt.isNullOrBlank()) {
                    val llmResult =
                            openRouterService.checkContent(
                                    monitor.llmPrompt,
                                    content,
                                    monitor.useWebSearch
                            )
                    if (llmResult) {
                        message += " LLM Condition Met."
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
            monitorDao.update(monitor.copy(lastCheckTime = now, lastContentHash = newHash))

            // Log Result
            val logId =
                    checkLogDao.insert(
                            CheckLog(
                                    monitorId = monitor.id,
                                    timestamp = now,
                                    result = result,
                                    message = message,
                                    content = content,
                                    rawContent = rawContent
                            )
                    )

            logRepository.logInfo(
                    "MONITOR",
                    "Check finished for ${monitor.name}: $result - $message"
            )

            // Send Notification if needed
            if (result == "CHANGED" && shouldNotify) {
                sendNotification(monitor.id, logId.toInt(), "Monitor Update", message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logRepository.logError(
                    "MONITOR",
                    "Unexpected error checking ${monitor.name}: ${e.message}",
                    e.stackTraceToString()
            )
            checkLogDao.insert(
                    CheckLog(
                            monitorId = monitor.id,
                            timestamp = now,
                            result = "FAILURE",
                            message = "Error: ${e.message}",
                            content = null,
                            rawContent = null
                    )
            )
        }
    }

    private suspend fun loadContent(
            url: String,
            selector: String,
            interactions: List<com.murmli.webpursuer.data.Interaction>
    ): String =
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true

                    webView.webViewClient =
                            object : WebViewClient() {
                                var pageLoaded = false

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (pageLoaded) return
                                    pageLoaded = true

                                    // Execute interactions sequentially
                                    Handler(Looper.getMainLooper())
                                            .postDelayed(
                                                    {
                                                        executeInteractions(
                                                                webView,
                                                                interactions,
                                                                0
                                                        ) {
                                                            // After all interactions, extract
                                                            // content
                                                            extractContent(webView, selector) { text
                                                                ->
                                                                if (continuation.isActive) {
                                                                    continuation.resume(text)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    2000
                                            ) // Initial wait
                                }

                                override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                ) {
                                    // Handle error
                                }
                            }

                    webView.loadUrl(url)
                }
            }

    private fun executeInteractions(
            webView: WebView,
            interactions: List<com.murmli.webpursuer.data.Interaction>,
            index: Int,
            onComplete: () -> Unit
    ) {
        if (index >= interactions.size) {
            onComplete()
            return
        }

        val interaction = interactions[index]
        val js =
                when (interaction.type) {
                    "click" -> "document.querySelector('${interaction.selector}').click();"
                    "input" ->
                            "document.querySelector('${interaction.selector}').value = '${interaction.value}'; document.querySelector('${interaction.selector}').dispatchEvent(new Event('change'));"
                    else -> ""
                }

        if (js.isNotEmpty()) {
            webView.evaluateJavascript(js) {
                // Wait a bit after interaction
                Handler(Looper.getMainLooper())
                        .postDelayed(
                                {
                                    executeInteractions(
                                            webView,
                                            interactions,
                                            index + 1,
                                            onComplete
                                    )
                                },
                                2000
                        ) // 2 seconds delay between actions
            }
        } else {
            executeInteractions(webView, interactions, index + 1, onComplete)
        }
    }

    private fun extractContent(webView: WebView, selector: String, callback: (String) -> Unit) {
        val js =
                """
            (function() {
                // Check if content is XML/RSS
                var contentType = document.contentType;
                if (contentType && (contentType.includes('xml') || contentType.includes('rss'))) {
                    return document.documentElement.outerHTML;
                }

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
            val text =
                    if (result != null && result != "null") {
                        try {
                            if (result.startsWith("\"") && result.endsWith("\"")) {
                                // Manual unescape to avoid build issues
                                var unescaped = result.substring(1, result.length - 1)
                                unescaped = unescaped.replace("\\\"", "\"").replace("\\\\", "\\")

                                // Decode Unicode escape sequences
                                val regex = Regex("\\\\u([0-9a-fA-F]{4})")
                                unescaped =
                                        regex.replace(unescaped) {
                                            it.groupValues[1].toInt(16).toChar().toString()
                                        }
                                unescaped.replace("\\n", "\n")
                            } else {
                                result
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WebChecker", "Unescape failed", e)
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

    private fun isRssFeed(content: String): Boolean {
        val trimmed = content.trimStart()
        return trimmed.startsWith("<?xml") ||
                trimmed.startsWith("<rss") ||
                trimmed.startsWith("<feed")
    }

    private fun parseRss(xml: String): String {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml))

            val sb = StringBuilder()
            var eventType = xpp.eventType
            var insideItem = false
            var title = ""
            var link = ""
            var description = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = xpp.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true) ||
                                        name.equals("entry", ignoreCase = true)
                        ) {
                            insideItem = true
                            title = ""
                            link = ""
                            description = ""
                        } else if (insideItem) {
                            if (name != null) {
                                when {
                                    name.equals("title", ignoreCase = true) ->
                                            title = safeNextText(xpp)
                                    name.equals("link", ignoreCase = true) -> {
                                        val href = xpp.getAttributeValue(null, "href")
                                        if (!href.isNullOrBlank()) {
                                            link = href
                                        } else {
                                            val text = safeNextText(xpp)
                                            if (text.isNotBlank()) link = text
                                        }
                                    }
                                    name.equals("description", ignoreCase = true) ||
                                            name.equals("summary", ignoreCase = true) -> {
                                        description = safeNextText(xpp)
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true) ||
                                        name.equals("entry", ignoreCase = true)
                        ) {
                            insideItem = false
                            sb.append("## $title\n")
                            if (link.isNotBlank()) sb.append("Link: $link\n")
                            if (description.isNotBlank()) {
                                val cleanDesc = cleanHtml(description)
                                if (cleanDesc.isNotBlank()) sb.append("Summary: $cleanDesc\n")
                            }
                            sb.append("---\n")
                        }
                    }
                }
                eventType = xpp.next()
            }
            return sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun safeNextText(xpp: XmlPullParser): String {
        return try {
            if (xpp.next() == XmlPullParser.TEXT) {
                xpp.text?.trim() ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun cleanHtml(html: String): String {
        return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT)
                .toString()
                .trim()
                .replace(Regex("\\n\\s*\\n"), "\n")
    }

    private suspend fun sendNotification(
            monitorId: Int,
            logId: Int,
            title: String,
            message: String
    ) {
        // Check if notifications are enabled globally
        val isGloballyEnabled = settingsRepository.notificationsEnabled.first()
        if (!isGloballyEnabled) {
            return
        }

        // Check if notifications are enabled for this monitor
        // Check if notifications are enabled for this monitor
        val monitor = monitorDao.getById(monitorId)
        if (monitor == null || !monitor.notificationsEnabled) {
            return
        }

        val intent =
                android.content.Intent(context, com.murmli.webpursuer.MainActivity::class.java)
                        .apply {
                            flags =
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("monitorId", monitorId)
                            putExtra("checkLogId", logId)
                        }
        val pendingIntent: android.app.PendingIntent =
                android.app.PendingIntent.getActivity(
                        context,
                        monitorId,
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE or
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )

        val builder =
                androidx.core.app.NotificationCompat.Builder(context, "web_monitor_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Changes found")
                        .setContentText("Content changed for ${monitor.name}")
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

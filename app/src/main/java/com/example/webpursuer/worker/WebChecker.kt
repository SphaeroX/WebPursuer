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

                val logId =
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

                sendNotification(monitor.id, logId.toInt(), "Monitor Failure", errorMsg)
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
            var changePercentage: Double? = null

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

                // Calculate change percentage
                val previousLog = checkLogDao.getPreviousLog(monitor.id, now)
                val previousContent = previousLog?.content
                changePercentage =
                        if (previousContent != null) {
                            calculateChangePercentage(previousContent, content)
                        } else {
                            100.0 // If no previous content, assume 100% change
                        }

                // Check threshold
                if (monitor.thresholdValue > 0) {
                    val thresholdMet =
                            when (monitor.thresholdType) {
                                "PERCENTAGE" -> changePercentage >= monitor.thresholdValue
                                "CHARACTER_COUNT" -> {
                                    val charDiff =
                                            kotlin.math.abs(
                                                    content.length - (previousContent?.length ?: 0)
                                            )
                                    charDiff >= monitor.thresholdValue.toInt()
                                }
                                else -> true
                            }
                    if (!thresholdMet) {
                        shouldNotify = false
                        message +=
                                " (below threshold ${monitor.thresholdValue}${if (monitor.thresholdType == "PERCENTAGE") "%" else " chars"})"
                    } else {
                        message += " (${String.format("%.1f", changePercentage)}% change)"
                    }
                } else {
                    message += " (${String.format("%.1f", changePercentage)}% change)"
                }

                if (monitor.llmEnabled && !monitor.llmPrompt.isNullOrBlank()) {
                    // Truncate content to avoid token limits (e.g., 8000 chars)
                    val truncatedContent = com.example.webpursuer.util.DiffUtils.truncate(content)
                    val llmResult =
                            openRouterService.checkContent(
                                    monitor.llmPrompt,
                                    truncatedContent,
                                    monitor.useWebSearch
                            )
                    if (llmResult) {
                        message += " LLM Condition Met."
                    } else {
                        message += " LLM Condition NOT Met."
                        shouldNotify = false // Don't notify if LLM condition fails
                    }
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
                                    rawContent = rawContent,
                                    changePercentage = changePercentage
                            )
                    )

            logRepository.logInfo(
                    "MONITOR",
                    "Check finished for ${monitor.name}: $result - $message"
            )

            // Send Notification if needed
            if (result == "CHANGED" && shouldNotify) {
                sendNotification(
                        monitor.id,
                        logId.toInt(),
                        "Monitor Update",
                        message,
                        changePercentage
                )
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
                    
                    // Crucial: Set a fixed layout size so the browser thinks it's on a real screen
                    // This triggers responsive rendering and makes elements "visible" for extraction
                    webView.layout(0, 0, 1280, 1024)
                    
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.databaseEnabled = true
                    webView.settings.loadWithOverviewMode = true
                    webView.settings.useWideViewPort = true
                    webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    
                    // Use a Desktop User-Agent to avoid simplified mobile layouts and better bypass bot-detection
                    webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    
                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                    android.webkit.CookieManager.getInstance()
                            .setAcceptThirdPartyCookies(webView, true)

                    webView.webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                android.util.Log.d("WebChecker-JS", "${it.messageLevel()}: ${it.message()}")
                            }
                            return true
                        }
                    }

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
                                                            // After all interactions, wait for selector to have content
                                                            waitForSelectorAndContent(webView, selector) {
                                                                // Extract content
                                                                extractContent(webView, selector) { text ->
                                                                    if (continuation.isActive) {
                                                                        continuation.resume(text)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    3000
                                            ) // Initial wait
                                }

                                override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                ) {
                                    android.util.Log.e("WebChecker", "Error loading $failingUrl: $description ($errorCode)")
                                }
                            }

                    webView.loadUrl(url)
                }
            }

    private fun getCommonJsHelpers(): String {
        return """
            function querySelectorDeep(selector, root = document) {
                // Try shallow first for performance
                let el = root.querySelector(selector);
                if (el) return el;
                
                // Optimized deep search
                function findInShadows(currentRoot) {
                    const all = currentRoot.querySelectorAll('*');
                    for (let i = 0; i < all.length; i++) {
                        const node = all[i];
                        if (node.shadowRoot) {
                            const found = node.shadowRoot.querySelector(selector) || findInShadows(node.shadowRoot);
                            if (found) return found;
                        }
                    }
                    return null;
                }
                return findInShadows(root);
            }

            function findEl(selStr, root = document) {
                var selectors = [];
                try {
                    var parsed = JSON.parse(selStr);
                    if (Array.isArray(parsed)) selectors = parsed;
                    else selectors = [selStr];
                } catch(e) {
                    selectors = [selStr];
                }
                for (var i = 0; i < selectors.length; i++) {
                    var sel = selectors[i];
                    try {
                        if (sel.startsWith("xpath=")) {
                            var el = document.evaluate(sel.substring(6), root, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                            if (el) return el;
                        } else if (sel.startsWith("text=")) {
                            // Text selector within root
                            var el = document.evaluate(".//*[normalize-space()='" + sel.substring(5).replace(/'/g, "\\'") + "']", root, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                            if (el) return el;
                        } else {
                            var el = querySelectorDeep(sel, root);
                            if (el) return el;
                        }
                    } catch(e) {}
                }
                return null;
            }

            function getRecursiveText(node) {
                if (node.nodeType === 3) { // Node.TEXT_NODE
                    return (node.nodeValue || "").trim();
                }
                if (node.nodeType !== 1) return ""; // Node.ELEMENT_NODE
                
                var tagName = node.tagName.toLowerCase();
                if (tagName === 'script' || tagName === 'style' || tagName === 'noscript') return "";
                
                var text = "";
                var isBlock = false;
                try {
                    var style = window.getComputedStyle(node);
                    if (style.display === 'none' || style.visibility === 'hidden') return "";
                    isBlock = (style.display === 'block' || style.display === 'flex' || style.display === 'grid' || style.display === 'table-row');
                } catch (e) {}

                if (tagName === 'input') {
                    var type = node.type ? node.type.toLowerCase() : 'text';
                    if (type !== 'hidden' && type !== 'submit' && type !== 'button' && type !== 'image') {
                         return node.value || "";
                    }
                }
                if (tagName === 'textarea') return node.value || "";
                if (tagName === 'select') {
                     if (node.selectedIndex >= 0) return node.options[node.selectedIndex].text;
                     return "";
                }
                if (tagName === 'br') return "\n";
                
                var childTexts = [];

                // Traverse Shadow DOM if it exists
                if (node.shadowRoot) {
                    for (var i = 0; i < node.shadowRoot.childNodes.length; i++) {
                        var childVal = getRecursiveText(node.shadowRoot.childNodes[i]);
                        if (childVal) childTexts.push(childVal);
                    }
                }

                for (var i = 0; i < node.childNodes.length; i++) {
                    var childVal = getRecursiveText(node.childNodes[i]);
                    if (childVal) childTexts.push(childVal);
                }
                
                text = childTexts.join(isBlock ? "\n" : " ");
                if (isBlock) text = "\n" + text + "\n";
                return text;
            }
        """.trimIndent()
    }

    private fun waitForSelectorAndContent(webView: WebView, selector: String, onComplete: () -> Unit) {
        val escapedSelector =
                selector.replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
        val js = """
            (function() {
                ${getCommonJsHelpers()}
                var el = findEl('$escapedSelector');
                if (el) {
                    var text = getRecursiveText(el);
                    if (text.trim().length > 0) return true;
                }
                return false;
            })();
        """.trimIndent()

        val maxWait = 15000L // 15 seconds max wait for content
        val pollInterval = 1000L
        val startTime = System.currentTimeMillis()

        fun poll() {
            webView.evaluateJavascript(js) { result ->
                if (result == "true" || (System.currentTimeMillis() - startTime) >= maxWait) {
                    onComplete()
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({ poll() }, pollInterval)
                }
            }
        }
        poll()
    }

    private fun waitForPageLoadThenNext(
            webView: WebView,
            interactions: List<com.murmli.webpursuer.data.Interaction>,
            nextIndex: Int,
            onComplete: () -> Unit
    ) {
        if (webView.progress < 100) {
            Handler(Looper.getMainLooper())
                    .postDelayed(
                            {
                                waitForPageLoadThenNext(
                                        webView,
                                        interactions,
                                        nextIndex,
                                        onComplete
                                )
                            },
                            500
                    )
        } else {
            executeInteractions(webView, interactions, nextIndex, onComplete)
        }
    }

    private fun waitForPageLoadThenComplete(webView: WebView, onComplete: () -> Unit) {
        if (webView.progress < 100) {
            Handler(Looper.getMainLooper())
                    .postDelayed({ waitForPageLoadThenComplete(webView, onComplete) }, 500)
        } else {
            onComplete()
        }
    }

    private fun executeInteractions(
            webView: WebView,
            interactions: List<com.murmli.webpursuer.data.Interaction>,
            index: Int,
            onComplete: () -> Unit
    ) {
        if (index >= interactions.size) {
            waitForPageLoadThenComplete(webView, onComplete)
            return
        }

        val interaction = interactions[index]
        val escapedSelector =
                interaction
                        .selector
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")

        var js = ""
        var waitTime = 500L

        when (interaction.type) {
            "click" -> {
                js =
                        """
                        ${getCommonJsHelpers()}
                        var el = findEl('$escapedSelector'); 
                        if(el) { 
                            var opts = { bubbles: true, cancelable: true, view: window, buttons: 1 }; 
                            el.dispatchEvent(new PointerEvent('pointerdown', opts)); 
                            el.dispatchEvent(new MouseEvent('mousedown', opts)); 
                            el.dispatchEvent(new PointerEvent('pointerup', opts)); 
                            el.dispatchEvent(new MouseEvent('mouseup', opts)); 
                            el.dispatchEvent(new MouseEvent('click', opts)); 
                        }
                        """.trimIndent()
            }
            "input" -> {
                val escapedValue =
                        interaction.value?.replace("'", "\\'")?.replace("\n", "\\n") ?: ""
                js =
                        """
                        ${getCommonJsHelpers()}
                        var el = findEl('$escapedSelector'); 
                        if(el) { 
                            el.value = '$escapedValue'; 
                            el.dispatchEvent(new Event('input', {bubbles: true})); 
                            el.dispatchEvent(new Event('change', {bubbles: true})); 
                        }
                        """.trimIndent()
            }
            "scroll" -> {
                js = "window.scrollTo({top: ${interaction.value ?: "0"}, behavior: 'smooth'});"
                waitTime = 1000L
            }
            "wait" -> {
                waitTime = interaction.value?.toLongOrNull() ?: 500L
                waitTime = waitTime.coerceAtMost(30000L)
            }
        }

        if (js.isNotEmpty()) {
            webView.evaluateJavascript(js) {
                Handler(Looper.getMainLooper())
                        .postDelayed(
                                {
                                    waitForPageLoadThenNext(
                                            webView,
                                            interactions,
                                            index + 1,
                                            onComplete
                                    )
                                },
                                waitTime
                        )
            }
        } else {
            Handler(Looper.getMainLooper())
                    .postDelayed(
                            {
                                waitForPageLoadThenNext(
                                        webView,
                                        interactions,
                                        index + 1,
                                        onComplete
                                )
                            },
                            waitTime
                    )
        }
    }

    private fun extractContent(webView: WebView, selector: String, callback: (String) -> Unit) {
        val escapedSelector =
                selector.replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
        val js =
                """
            (function() {
                // Check if content is XML/RSS
                var contentType = document.contentType;
                if (contentType && (contentType.includes('xml') || contentType.includes('rss'))) {
                    return document.documentElement.outerHTML;
                }

                ${getCommonJsHelpers()}

                var element = findEl('$escapedSelector');
                if (!element) return '';
                
                // Clean up multiple newlines
                return getRecursiveText(element).replace(/\n\s*\n/g, '\n').trim();
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            val text =
                    if (result != null && result != "null") {
                        try {
                            var unescaped = result

                            // Remove surrounding quotes if present
                            if (unescaped.startsWith("\"") && unescaped.endsWith("\"")) {
                                unescaped = unescaped.substring(1, unescaped.length - 1)
                            }

                            // Unescape JSON special characters
                            unescaped =
                                    unescaped
                                            .replace("\\\"", "\"")
                                            .replace("\\\\", "\\")
                                            .replace("\\n", "\n")
                                            .replace("\\r", "\r")
                                            .replace("\\t", "\t")

                            // Decode Unicode escape sequences (\uXXXX)
                            val unicodeRegex = Regex("\\\\u([0-9a-fA-F]{4})")
                            unescaped =
                                    unicodeRegex.replace(unescaped) {
                                        it.groupValues[1].toInt(16).toChar().toString()
                                    }

                            unescaped
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

    private fun calculateChangePercentage(oldContent: String, newContent: String): Double {
        val oldLength = oldContent.length
        val newLength = newContent.length

        if (oldLength == 0 && newLength == 0) return 0.0
        if (oldLength == 0) return 100.0
        if (oldContent == newContent) return 0.0

        // Use fast approximation for large strings to prevent ANRs
        if (oldLength > 2000 || newLength > 2000) {
            val oldWords = oldContent.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
            val newWords = newContent.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()

            val intersection = oldWords.intersect(newWords).size
            val union = oldWords.union(newWords).size

            if (union == 0) return 0.0
            val similarity = intersection.toDouble() / union
            return (1.0 - similarity) * 100.0
        }

        // Calculate Levenshtein distance for smaller strings
        val distance = levenshteinDistance(oldContent, newContent)
        val maxLength = kotlin.math.max(oldLength, newLength)

        return (distance.toDouble() / maxLength) * 100.0
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] =
                        kotlin.math.min(
                                kotlin.math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                                dp[i - 1][j - 1] + cost
                        )
            }
        }

        return dp[m][n]
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
                            // Decode HTML entities in title
                            val decodedTitle = decodeHtmlEntities(title)
                            sb.append("## $decodedTitle\n")
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

    private fun decodeHtmlEntities(text: String): String {
        return text.replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&ndash;", "–")
                .replace("&mdash;", "—")
                .replace("&lsquo;", "'")
                .replace("&rsquo;", "'")
                .replace("&ldquo;", "\"")
                .replace("&rdquo;", "\"")
                .replace("&hellip;", "…")
                .replace("&euro;", "€")
                .replace("&pound;", "£")
                .replace("&yen;", "¥")
                .replace("&copy;", "©")
                .replace("&reg;", "®")
                .replace("&trade;", "™")
    }

    private suspend fun sendNotification(
            monitorId: Int,
            logId: Int,
            title: String,
            message: String,
            changePercentage: Double? = null
    ) {
        // Check if notifications are enabled globally
        val isGloballyEnabled = settingsRepository.notificationsEnabled.first()
        if (!isGloballyEnabled) {
            return
        }

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

        val notificationText =
                if (changePercentage != null) {
                    "${monitor.name} (${String.format("%.1f", changePercentage)}%)"
                } else {
                    monitor.name
                }

        val builder =
                androidx.core.app.NotificationCompat.Builder(context, "web_monitor_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(notificationText)
                        .setSubText(message)
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
